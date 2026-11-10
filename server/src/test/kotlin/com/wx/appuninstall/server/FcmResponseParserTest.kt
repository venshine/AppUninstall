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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 响应样例按官方文档的字段构造：
 * - 错误码表 https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode
 * - 错误响应结构 https://firebase.google.com/docs/cloud-messaging/error-codes（details 里 FcmError 带 errorCode）
 * 成功响应的 name 字段见 projects.messages.send 文档。这些不是线上录制，真实响应以服务端实测为准。
 */
class FcmResponseParserTest {

    private fun error(code: Int, status: String, errorCode: String?): String {
        val debug = """{"@type":"type.googleapis.com/google.rpc.DebugInfo","detail":"x"}"""
        val fcm = """{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"$errorCode"}"""
        val details = if (errorCode != null) "$debug,$fcm" else debug
        return """{"error":{"code":$code,"message":"m","status":"$status","details":[$details]}}"""
    }

    @Test
    fun successIsDelivered() {
        val r = FcmResponseParser.parse(200, """{"name":"projects/p/messages/0:123"}""")
        assertEquals(FcmResult.Delivered("projects/p/messages/0:123"), r)
    }

    @Test
    fun unregisteredIsRecognisedFromFcmErrorDetail() {
        assertEquals(FcmResult.Unregistered, FcmResponseParser.parse(404, error(404, "NOT_FOUND", "UNREGISTERED")))
    }

    @Test
    fun notFoundWithoutDetailIsNotTreatedAsUninstall() {
        // 项目 ID 写错也会 404，没有 UNREGISTERED 细节时不能判定卸载
        val r = FcmResponseParser.parse(404, """{"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND"}}""")
        assertTrue(r is FcmResult.Unknown)
    }

    @Test
    fun invalidArgumentIsNotUninstall() {
        assertEquals(FcmResult.InvalidArgument, FcmResponseParser.parse(400, error(400, "INVALID_ARGUMENT", "INVALID_ARGUMENT")))
        assertEquals(FcmResult.InvalidArgument, FcmResponseParser.parse(400, """{"error":{"code":400,"status":"INVALID_ARGUMENT"}}"""))
    }

    @Test
    fun authAndRetryableErrorsAreClassified() {
        assertTrue(FcmResponseParser.parse(403, error(403, "PERMISSION_DENIED", "SENDER_ID_MISMATCH")) is FcmResult.AuthError)
        assertTrue(FcmResponseParser.parse(401, """{"error":{"code":401}}""") is FcmResult.AuthError)
        assertTrue(FcmResponseParser.parse(429, error(429, "RESOURCE_EXHAUSTED", "QUOTA_EXCEEDED")) is FcmResult.Retryable)
        assertTrue(FcmResponseParser.parse(503, error(503, "UNAVAILABLE", "UNAVAILABLE")) is FcmResult.Retryable)
        assertTrue(FcmResponseParser.parse(500, "") is FcmResult.Retryable)
    }

    @Test
    fun garbageBodyDoesNotThrow() {
        assertTrue(FcmResponseParser.parse(502, "<html>bad gateway</html>") is FcmResult.Unknown)
        assertTrue(FcmResponseParser.parse(404, "") is FcmResult.Unknown)
    }
}
