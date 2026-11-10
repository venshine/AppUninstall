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
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/** 测试用的假传输层：按 URL 返回预设响应并记录请求 */
class FakeTransport(private val fcmStatus: (String) -> HttpResult) : HttpTransport {
    val requests = ArrayList<Triple<String, Map<String, String>, String>>()

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResult {
        requests.add(Triple(url, headers, body))
        return if (url == FcmClient.TOKEN_URL) {
            HttpResult(200, """{"access_token":"ya29.fake","expires_in":3600,"token_type":"Bearer"}""")
        } else {
            fcmStatus(body)
        }
    }
}

private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
private val account = ServiceAccount("demo-project", "svc@demo-project.iam.gserviceaccount.com", keyPair.private)

class FcmClientTest {

    @Test
    fun jwtHasExpectedClaimsAndValidSignature() {
        val jwt = FcmClient(account, FakeTransport { HttpResult(200, "{}") }).signJwt(1_700_000_000)
        val (h, c, s) = jwt.split(".")
        val dec = Base64.getUrlDecoder()
        assertEquals(mapOf("alg" to "RS256", "typ" to "JWT"), Json.parseObject(String(dec.decode(h))))
        val claims = Json.parseObject(String(dec.decode(c)))
        assertEquals(account.clientEmail, claims["iss"])
        assertEquals(FcmClient.SCOPE, claims["scope"])
        assertEquals("https://oauth2.googleapis.com/token", claims["aud"])
        assertEquals(1_700_000_000L, claims["iat"])
        assertEquals(1_700_003_600L, claims["exp"])
        val verifier = Signature.getInstance("SHA256withRSA").apply { initVerify(keyPair.public); update("$h.$c".toByteArray()) }
        assertTrue(verifier.verify(dec.decode(s)))
    }

    @Test
    fun sendPostsToProjectEndpointWithBearerToken() {
        val transport = FakeTransport { HttpResult(200, """{"name":"projects/demo-project/messages/1"}""") }
        val result = FcmClient(account, transport).send("tok-1", mapOf("type" to "probe"))
        assertEquals(FcmResult.Delivered("projects/demo-project/messages/1"), result)

        val tokenRequest = transport.requests[0]
        assertEquals(FcmClient.TOKEN_URL, tokenRequest.first)
        assertTrue(tokenRequest.third.startsWith("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion="))

        val (url, headers, body) = transport.requests[1]
        assertEquals("https://fcm.googleapis.com/v1/projects/demo-project/messages:send", url)
        assertEquals("Bearer ya29.fake", headers["Authorization"])
        assertEquals("""{"message":{"token":"tok-1","data":{"type":"probe"}}}""", body)
    }

    @Test
    fun accessTokenIsCachedBetweenSends() {
        val transport = FakeTransport { HttpResult(200, """{"name":"n"}""") }
        val client = FcmClient(account, transport)
        client.send("a", emptyMap())
        client.send("b", emptyMap())
        assertEquals(1, transport.requests.count { it.first == FcmClient.TOKEN_URL })
    }

    @Test
    fun oauthFailureIsReportedNotSwallowed() {
        val failing = HttpTransport { _, _, _ -> HttpResult(400, """{"error":"invalid_grant"}""") }
        val e = runCatching { FcmClient(account, failing).accessToken() }.exceptionOrNull()
        assertTrue(e is IllegalStateException && e.message!!.contains("400"))
    }
}

class ApiTest {

    private var now = 0L
    private val registry = DeviceRegistry { now }
    private val unregistered = """{"error":{"code":404,"status":"NOT_FOUND","details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"UNREGISTERED"}]}}"""

    private fun api(fcm: FcmClient? = null) = Api(registry, 60_000, fcm)

    @Test
    fun registerThenHeartbeatThenTimeout() {
        val api = api()
        assertEquals(200, api.handle("POST", "/register", """{"installId":"a","token":"t"}""").status)
        now = 60_000
        assertEquals("ACTIVE", api.handle("POST", "/heartbeat", """{"installId":"a"}""").body["status"])
        now = 120_001
        val devices = api.handle("GET", "/devices", "").body["devices"] as List<*>
        assertEquals("SUSPECTED_CHURN", (devices[0] as Map<*, *>)["status"])
    }

    @Test
    fun badRequestsAreRejected() {
        val api = api()
        assertEquals(400, api.handle("POST", "/register", """{"token":"t"}""").status)
        assertEquals(400, api.handle("POST", "/heartbeat", "not json").status)
        assertEquals(404, api.handle("GET", "/nope", "").status)
    }

    @Test
    fun probeWithoutCredentialsIsServiceUnavailable() {
        assertEquals(503, api().handle("POST", "/probe", "").status)
    }

    @Test
    fun probeMarksUnregisteredTokenAsUninstalled() {
        val transport = FakeTransport { body -> if (body.contains("dead")) HttpResult(404, unregistered) else HttpResult(200, """{"name":"n"}""") }
        val api = api(FcmClient(account, transport))
        api.handle("POST", "/register", """{"installId":"gone","token":"dead-token"}""")
        api.handle("POST", "/register", """{"installId":"here","token":"live-token"}""")

        val results = api.handle("POST", "/probe", "").body["results"] as List<*>
        assertEquals(listOf("Unregistered", "Delivered"), results.map { (it as Map<*, *>)["result"] })

        val statuses = (api.handle("GET", "/devices", "").body["devices"] as List<*>)
            .associate { (it as Map<*, *>)["installId"] to it["status"] }
        assertEquals(mapOf("gone" to "UNINSTALLED", "here" to "ACTIVE"), statuses)
    }
}
