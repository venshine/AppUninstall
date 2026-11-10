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
package com.wx.appuninstall

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.FirebaseApp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/** 演示服务端地址：电脑上的服务端通过 `adb reverse tcp:8080 tcp:8080` 暴露给手机 */
object ServerConfig {
    const val BASE_URL = "http://127.0.0.1:8080"
}

/** 本 App 的安装标识：首次启动随机生成，卸载重装会变，不是系统设备标识 */
object InstallId {
    private const val KEY = "install_id"

    fun get(context: Context): String {
        val prefs = AppState.prefs(context)
        return prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY, it).apply()
        }
    }
}

/** 本地缓存：最近一次拿到的 FCM 令牌、最近一次收到服务端数据消息的时间 */
object AppState {
    private const val FILE = "app_state"

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun token(context: Context): String? = prefs(context).getString("fcm_token", null)

    fun saveToken(context: Context, token: String) = prefs(context).edit().putString("fcm_token", token).apply()

    fun lastPush(context: Context): Long = prefs(context).getLong("last_push", 0L)

    fun saveLastPush(context: Context, time: Long) = prefs(context).edit().putLong("last_push", time).apply()
}

/** Firebase 是否已配置：没有 google-services.json 时构建不会生成默认 FirebaseApp */
object FirebaseStatus {
    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
}

/** 服务端请求结果。[code] 为 -1 表示没连上 */
data class Reply(val ok: Boolean, val code: Int, val body: String)

/** 与本地演示服务端通信，阻塞调用，必须在后台线程使用（见 [Bg]） */
object ServerApi {

    var baseUrl: String = ServerConfig.BASE_URL

    fun register(installId: String, token: String?): Reply =
        post("/register", JSONObject().put("installId", installId).apply { if (token != null) put("token", token) })

    fun heartbeat(installId: String): Reply = post("/heartbeat", JSONObject().put("installId", installId))

    fun devices(): Reply = request("GET", "/devices", null)

    private fun post(path: String, json: JSONObject) = request("POST", path, json.toString())

    private fun request(method: String, path: String, body: String?): Reply {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            Reply(code in 200..299, code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (e: java.io.IOException) {
            Reply(false, -1, e.message.orEmpty())
        } finally {
            connection.disconnect()
        }
    }
}

/** 单线程后台执行，结果回到主线程 */
object Bg {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun <T> run(work: () -> T, done: (T) -> Unit) {
        executor.execute {
            val result = work()
            main.post { done(result) }
        }
    }
}
