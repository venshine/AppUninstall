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

/**
 * FCM HTTP v1 发送结果的判定，只依赖 HTTP 状态码和响应体，可离线测试。
 *
 * 依据：
 * - 错误码表：https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode
 *   （UNREGISTERED 对应 404，INVALID_ARGUMENT 对应 400）
 * - 错误响应结构：https://firebase.google.com/docs/cloud-messaging/error-codes
 *   （details 数组里 @type 为 google.firebase.fcm.v1.FcmError 的元素带 errorCode）
 */
sealed class FcmResult {
    /** 发送成功（200），[name] 是 FCM 返回的消息名 */
    data class Delivered(val name: String?) : FcmResult()

    /** UNREGISTERED：令牌不会再有效，官方原文列出的原因含「应用卸载」，但也含令牌过期、应用更新后收不到消息 */
    data object Unregistered : FcmResult()

    /** INVALID_ARGUMENT：可能是令牌格式错，也可能是载荷错，不能据此判定卸载 */
    data object InvalidArgument : FcmResult()

    /** 401 / 403：凭据或发送方不匹配，属于服务端配置问题 */
    data class AuthError(val status: Int, val code: String?) : FcmResult()

    /** 429 / 500 / 503：可稍后重试 */
    data class Retryable(val status: Int, val code: String?) : FcmResult()

    /** 其他：包括没带 errorCode 的 404（可能是项目 ID 写错，不能当作卸载） */
    data class Unknown(val status: Int, val code: String?) : FcmResult()
}

object FcmResponseParser {

    private const val FCM_ERROR_TYPE = "type.googleapis.com/google.firebase.fcm.v1.FcmError"

    fun parse(status: Int, body: String): FcmResult {
        val json = runCatching { Json.parseObject(body) }.getOrNull()
        if (status == 200) return FcmResult.Delivered(json?.get("name") as? String)

        val error = json?.get("error") as? Map<*, *>
        val code = fcmErrorCode(error)
        return when {
            code == "UNREGISTERED" -> FcmResult.Unregistered
            code == "INVALID_ARGUMENT" -> FcmResult.InvalidArgument
            code == "SENDER_ID_MISMATCH" || code == "THIRD_PARTY_AUTH_ERROR" || status == 401 || status == 403 ->
                FcmResult.AuthError(status, code)
            code == "QUOTA_EXCEEDED" || code == "UNAVAILABLE" || code == "INTERNAL" ||
                status == 429 || status == 500 || status == 503 -> FcmResult.Retryable(status, code)
            // 没有 FcmError 细节时，400 按官方码表归为 INVALID_ARGUMENT
            status == 400 && code == null -> FcmResult.InvalidArgument
            else -> FcmResult.Unknown(status, code)
        }
    }

    /** 取 details 里 FcmError 的 errorCode；没有时返回 null */
    private fun fcmErrorCode(error: Map<*, *>?): String? {
        val details = error?.get("details") as? List<*> ?: return null
        for (item in details) {
            val detail = item as? Map<*, *> ?: continue
            if (detail["@type"] == FCM_ERROR_TYPE) return detail["errorCode"] as? String
        }
        return null
    }
}
