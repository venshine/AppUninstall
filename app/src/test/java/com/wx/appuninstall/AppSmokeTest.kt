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

import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.sun.net.httpserver.HttpServer
import androidx.work.Configuration
import androidx.work.WorkManager

import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.net.InetSocketAddress
import java.time.Duration

/**
 * 示例页冒烟测试：每个页面在浅色、深色下都能启动；没有 google-services.json 时显示「未配置」而不是崩溃；
 * 关键交互的结果符合预期。
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class AppSmokeTest {

    private val created = ArrayList<ActivityController<*>>()

    @Before
    fun setUp() {
        // Robolectric 里不会自动跑 androidx.startup，手动初始化 WorkManager（同步执行器，不依赖后台线程）
        runCatching {
            WorkManager.initialize(
                RuntimeEnvironment.getApplication(),
                Configuration.Builder().setExecutor(java.util.concurrent.Executor { it.run() }).build(),
            )
        }
    }

    @After
    fun tearDown() {
        created.forEach { runCatching { it.destroy() } }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun idle(seconds: Long = 1) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

    private fun <T : Activity> launch(type: Class<T>): T {
        val controller = Robolectric.buildActivity(type).setup()
        created.add(controller)
        idle()
        return controller.get()
    }

    private fun Activity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private val screens = listOf(
        MainActivity::class.java, FcmActivity::class.java, AnalyticsActivity::class.java,
        HeartbeatActivity::class.java, CompareActivity::class.java,
    )

    @Test
    fun everyScreenLaunchesInLightAndDark() {
        for (night in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            AppCompatDelegate.setDefaultNightMode(night)
            for (type in screens) assertNotNull("${type.simpleName} night=$night", launch(type).findViewById<View>(android.R.id.content))
        }
    }

    @Test
    fun firebaseChipFollowsConfigurationAndNeverCrashes() {
        // 没有 google-services.json 时为「未配置」；主人放入真实文件后为「已配置」，两种情况页面都应正常
        val configured = FirebaseStatus.isConfigured(RuntimeEnvironment.getApplication())
        val expected = if (configured) "Firebase 已配置" else "Firebase 未配置"
        assertEquals(expected, launch(MainActivity::class.java).text(R.id.firebase_chip))
    }

    @Test
    fun homeCardsOpenTheirScreens() {
        val home = launch(MainActivity::class.java)
        val expected = mapOf(
            R.id.card_fcm to FcmActivity::class.java, R.id.card_analytics to AnalyticsActivity::class.java,
            R.id.card_heartbeat to HeartbeatActivity::class.java, R.id.card_compare to CompareActivity::class.java,
        )
        for ((id, target) in expected) {
            home.findViewById<View>(id).performClick()
            assertEquals(target.name, shadowOf(home).nextStartedActivity.component?.className)
        }
    }

    @Test
    fun fcmPageExplainsMissingConfigAndTokenInsteadOfCrashing() {
        assumeFalse("放入真实 google-services.json 后跳过", FirebaseStatus.isConfigured(RuntimeEnvironment.getApplication()))
        val page = launch(FcmActivity::class.java)
        page.findViewById<View>(R.id.fcm_fetch).performClick()
        assertTrue(page.text(R.id.fcm_result).contains("google-services.json"))
        page.findViewById<View>(R.id.fcm_register).performClick()
        assertEquals("先获取令牌。", page.text(R.id.fcm_result))
    }

    @Test
    fun analyticsPageExplainsMissingConfig() {
        assumeFalse("放入真实 google-services.json 后跳过", FirebaseStatus.isConfigured(RuntimeEnvironment.getApplication()))
        val page = launch(AnalyticsActivity::class.java)
        page.findViewById<View>(R.id.analytics_enable).performClick()
        assertTrue(page.text(R.id.analytics_result).contains("google-services.json"))
    }

    @Test
    fun heartbeatPageTogglesAndSchedules() {
        val page = launch(HeartbeatActivity::class.java)
        val id = InstallId.get(page)
        assertEquals(id, page.text(R.id.hb_install_id))
        assertEquals(id, InstallId.get(page))

        val toggle = page.findViewById<TextView>(R.id.hb_foreground)
        toggle.performClick()
        assertEquals("停止前台心跳", toggle.text.toString())
        toggle.performClick()
        assertEquals("开始前台心跳（每 10 秒）", toggle.text.toString())

        page.findViewById<View>(R.id.hb_schedule).performClick()
        assertTrue(HeartbeatScheduler.isScheduled(page))
        page.findViewById<View>(R.id.hb_cancel).performClick()
        assertFalse(HeartbeatScheduler.isScheduled(page))
    }

    @Test
    fun comparePageListsEveryApproach() {
        val page = launch(CompareActivity::class.java)
        val list = page.findViewById<android.widget.LinearLayout>(R.id.compare_list)
        assertEquals(CompareData.approaches.size, list.childCount)
    }

    @Test
    fun compareDataEveryApproachHasHttpsSourceAndFilledFields() {
        assertEquals(CompareData.approaches.size, CompareData.approaches.map { it.id }.toSet().size)
        for (a in CompareData.approaches) {
            assertTrue(a.id, a.sources.isNotEmpty() && a.sources.all { it.second.startsWith("https://") })
            assertTrue(a.id, listOf(a.principle, a.latency, a.falsePositive, a.requires).all { it.isNotBlank() })
        }
    }

    @Test
    fun serverApiPostsRegistrationAndReadsErrors() {
        val received = ArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/register") { ex ->
            received.add(String(ex.requestBody.readAllBytes()))
            val bytes = """{"status":"ACTIVE"}""".toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.createContext("/heartbeat") { ex ->
            ex.sendResponseHeaders(400, -1)
            ex.close()
        }
        server.start()
        val old = ServerApi.baseUrl
        try {
            ServerApi.baseUrl = "http://127.0.0.1:${server.address.port}"
            val ok = ServerApi.register("abc", "tok")
            assertTrue(ok.ok)
            assertEquals("""{"status":"ACTIVE"}""", ok.body)
            assertEquals("""{"installId":"abc","token":"tok"}""", received.single())
            assertEquals(400, ServerApi.heartbeat("abc").code)

            server.stop(0)
            val down = ServerApi.heartbeat("abc")
            assertFalse(down.ok)
            assertEquals(-1, down.code)
        } finally {
            ServerApi.baseUrl = old
            server.stop(0)
        }
    }
}
