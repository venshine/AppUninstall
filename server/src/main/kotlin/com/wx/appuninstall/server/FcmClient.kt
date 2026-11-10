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

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64

/** HTTP 响应：状态码加正文 */
data class HttpResult(val status: Int, val body: String)

/** 发 POST 请求的最小抽象，测试里换成假实现 */
fun interface HttpTransport {
    fun post(url: String, headers: Map<String, String>, body: String): HttpResult
}

/** 基于 JDK 自带 java.net.http 的实现 */
class JdkHttpTransport : HttpTransport {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResult {
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(body))
        headers.forEach { (k, v) -> builder.header(k, v) }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body())
    }
}

/** 服务账号密钥文件里用到的字段 */
data class ServiceAccount(val projectId: String, val clientEmail: String, val privateKey: PrivateKey) {
    companion object {
        /** 解析 Firebase 控制台下载的服务账号 JSON */
        fun parse(json: String): ServiceAccount {
            val map = Json.parseObject(json)
            fun field(name: String) = map[name] as? String ?: throw IllegalArgumentException("服务账号 JSON 缺少 $name")
            val pem = field("private_key")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace(Regex("\\s"), "")
            val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)))
            return ServiceAccount(field("project_id"), field("client_email"), key)
        }
    }
}

/**
 * FCM HTTP v1 客户端：用服务账号换 OAuth 访问令牌，再向已登记的令牌发数据消息。
 *
 * 依据：
 * - 服务账号换令牌（RS256 的 JWT，POST https://oauth2.googleapis.com/token，grant_type 为 jwt-bearer）：
 *   https://developers.google.com/identity/protocols/oauth2/service-account
 * - 发送端点 POST https://fcm.googleapis.com/v1/projects/{project}/messages:send，
 *   作用域 https://www.googleapis.com/auth/firebase.messaging：
 *   https://firebase.google.com/docs/cloud-messaging/send/v1-api
 */
class FcmClient(
    private val account: ServiceAccount,
    private val transport: HttpTransport = JdkHttpTransport(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var cachedToken: String? = null
    private var cachedUntil = 0L

    @Synchronized
    fun accessToken(): String {
        val now = clock()
        cachedToken?.let { if (now < cachedUntil - 60_000) return it }
        val assertion = signJwt(now / 1000)
        val body = "grant_type=" + enc("urn:ietf:params:oauth:grant-type:jwt-bearer") + "&assertion=" + enc(assertion)
        val result = transport.post(TOKEN_URL, mapOf("Content-Type" to "application/x-www-form-urlencoded"), body)
        if (result.status != 200) throw IllegalStateException("换取访问令牌失败：HTTP ${result.status} ${result.body}")
        val json = Json.parseObject(result.body)
        val token = json["access_token"] as? String ?: throw IllegalStateException("响应缺少 access_token")
        val ttl = (json["expires_in"] as? Long) ?: 3600L
        cachedToken = token
        cachedUntil = now + ttl * 1000
        return token
    }

    /** 给一个令牌发一条仅含数据的消息（不会弹通知），返回判定结果 */
    fun send(token: String, data: Map<String, String>): FcmResult {
        val message = mapOf("message" to mapOf("token" to token, "data" to data))
        val result = transport.post(
            "https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send",
            mapOf("Authorization" to "Bearer ${accessToken()}", "Content-Type" to "application/json"),
            Json.stringify(message),
        )
        return FcmResponseParser.parse(result.status, result.body)
    }

    internal fun signJwt(issuedAtSeconds: Long): String {
        val header = b64(Json.stringify(mapOf("alg" to "RS256", "typ" to "JWT")).toByteArray())
        val claims = b64(
            Json.stringify(
                mapOf(
                    "iss" to account.clientEmail,
                    "scope" to SCOPE,
                    "aud" to TOKEN_URL,
                    "iat" to issuedAtSeconds,
                    "exp" to issuedAtSeconds + 3600,
                )
            ).toByteArray()
        )
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(account.privateKey)
        signer.update("$header.$claims".toByteArray())
        return "$header.$claims.${b64(signer.sign())}"
    }

    private fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun enc(s: String) = java.net.URLEncoder.encode(s, Charsets.UTF_8)

    companion object {
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    }
}
