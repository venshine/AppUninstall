/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.appuninstall.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

/**
 * 本地演示服务端的路由与业务，和 HTTP 服务器本身分开，方便在测试里直接调用 [handle]。
 *
 * 接口（都是 JSON）：
 * - POST /register   {"installId","token"}   登记设备和 FCM 令牌
 * - POST /heartbeat  {"installId"}           上报心跳
 * - GET  /devices                            列出设备及判定状态
 * - POST /probe                              给所有登记过令牌的设备发一条数据消息，UNREGISTERED 记为已卸载
 * - GET  /health
 *
 * 没有鉴权，只应绑定回环地址，通过 adb reverse 给手机用。
 */
class Api(
    private val registry: DeviceRegistry,
    private val timeoutMillis: Long,
    private val fcm: FcmClient?,
) {
    class Response(val status: Int, val body: Map<String, Any?>)

    fun handle(method: String, path: String, body: String): Response = try {
        when {
            method == "GET" && path == "/health" -> ok("ok" to true)
            method == "POST" && path == "/register" -> {
                val json = Json.parseObject(body)
                val id = requireString(json, "installId")
                val token = (json["token"] as? String)?.takeIf { it.isNotBlank() }
                ok("status" to statusOf(registry.register(id, token)))
            }
            method == "POST" && path == "/heartbeat" -> {
                val id = requireString(Json.parseObject(body), "installId")
                ok("status" to statusOf(registry.heartbeat(id)))
            }
            method == "GET" && path == "/devices" -> devices()
            method == "POST" && path == "/probe" -> probe()
            else -> Response(404, mapOf("error" to "not_found"))
        }
    } catch (e: Json.JsonException) {
        Response(400, mapOf("error" to "bad_json", "message" to e.message))
    } catch (e: IllegalArgumentException) {
        Response(400, mapOf("error" to "bad_request", "message" to e.message))
    }

    private fun ok(vararg pairs: Pair<String, Any?>) = Response(200, mapOf(*pairs))

    private fun requireString(json: Map<String, Any?>, name: String): String =
        (json[name] as? String)?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("缺少 $name")

    private fun statusOf(device: Device) = ChurnJudge.judge(device, registry.now(), timeoutMillis).name

    private fun devices(): Response {
        val now = registry.now()
        val list = registry.snapshot().map {
            mapOf(
                "installId" to it.installId,
                "token" to it.fcmToken?.let { t -> "…" + t.takeLast(8) },
                "status" to ChurnJudge.judge(it, now, timeoutMillis).name,
                "lastSeenSecondsAgo" to (now - it.lastSeenAt) / 1000,
            )
        }
        return ok("timeoutSeconds" to timeoutMillis / 1000, "devices" to list)
    }

    private fun probe(): Response {
        val client = fcm ?: return Response(503, mapOf("error" to "fcm_not_configured"))
        val results = registry.snapshot().filter { it.fcmToken != null }.map { device ->
            val result = client.send(device.fcmToken!!, mapOf("type" to "probe"))
            if (result is FcmResult.Unregistered) registry.markUninstalled(device.installId)
            mapOf("installId" to device.installId, "result" to result.javaClass.simpleName)
        }
        return ok("results" to results)
    }
}

/** 启动入口：环境变量 PORT、BIND、HEARTBEAT_TIMEOUT_SECONDS、FCM_SERVICE_ACCOUNT（服务账号 JSON 路径） */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val bind = System.getenv("BIND") ?: "127.0.0.1"
    val timeout = (System.getenv("HEARTBEAT_TIMEOUT_SECONDS")?.toLongOrNull() ?: 1800L) * 1000
    val keyPath = Path.of(System.getenv("FCM_SERVICE_ACCOUNT") ?: ".private/keys/firebase-service-account.json")
    val fcm = if (Files.isRegularFile(keyPath)) {
        FcmClient(ServiceAccount.parse(Files.readString(keyPath)))
    } else {
        println("未找到服务账号密钥 $keyPath：/probe 不可用，登记与心跳照常工作")
        null
    }
    val api = Api(DeviceRegistry(), timeout, fcm)

    val server = HttpServer.create(InetSocketAddress(bind, port), 0)
    server.executor = Executors.newFixedThreadPool(4)
    server.createContext("/") { exchange -> respond(exchange, api) }
    server.start()
    println("AppUninstall server listening on http://$bind:$port  (heartbeat timeout ${timeout / 1000}s)")
    println("手机端：adb reverse tcp:$port tcp:$port")
}

private fun respond(exchange: HttpExchange, api: Api) {
    exchange.use {
        val body = String(it.requestBody.readNBytes(MAX_BODY), Charsets.UTF_8)
        val response = try {
            api.handle(it.requestMethod, it.requestURI.path, body)
        } catch (e: Exception) {
            Api.Response(500, mapOf("error" to "internal", "message" to e.message))
        }
        val bytes = Json.stringify(response.body).toByteArray()
        it.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        it.sendResponseHeaders(response.status, bytes.size.toLong())
        it.responseBody.use { out -> out.write(bytes) }
    }
}

private const val MAX_BODY = 16 * 1024
