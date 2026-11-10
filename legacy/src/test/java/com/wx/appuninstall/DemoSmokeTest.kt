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
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.io.File
import java.time.Duration

/**
 * 示例页冒烟测试：每个页面在浅色、深色下都能启动，关键交互的结果符合预期。
 * 原生库在 JVM 上无法加载，所以启动监听的入口在测试里换成假实现。
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class DemoSmokeTest {

    private val created = ArrayList<ActivityController<*>>()
    private val originalStarter = WatchController.starter

    @Before
    fun setUp() {
        File(RuntimeEnvironment.getApplication().filesDir, "observedFile").delete()
    }

    @After
    fun tearDown() {
        // 销毁本用例启动过的 Activity，恢复启动入口和浅色
        created.forEach { runCatching { it.destroy() } }
        WatchController.starter = originalStarter
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun idle(seconds: Long = 1) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    private fun <T : Activity> launch(type: Class<T>): T {
        val controller = Robolectric.buildActivity(type).setup()
        created.add(controller)
        idle()
        return controller.get()
    }

    private fun Activity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private val screens = listOf(
        MainActivity::class.java, PrincipleActivity::class.java, WatchActivity::class.java,
        UninstallActivity::class.java, AlternativesActivity::class.java,
    )

    @Test
    fun allScreensLaunchInLightMode() {
        screens.forEach { type ->
            assertTrue("${type.simpleName} 应为浅色", !DemoTheme.isNight(launch(type)))
        }
    }

    @Test
    fun allScreensLaunchInDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        screens.forEach { type ->
            assertTrue("${type.simpleName} 应为深色", DemoTheme.isNight(launch(type)))
        }
    }

    @Test
    fun homeShowsVersionDeviceAndOpensDemos() {
        val home = launch(MainActivity::class.java)
        assertTrue(home.text(R.id.version_chip).startsWith("v"))
        assertTrue(home.text(R.id.preview_device).contains("API"))
        val targets = mapOf(
            R.id.card_principle to PrincipleActivity::class.java, R.id.card_watch to WatchActivity::class.java,
            R.id.card_uninstall to UninstallActivity::class.java,
            R.id.card_alternatives to AlternativesActivity::class.java,
        )
        targets.forEach { (id, type) ->
            home.findViewById<View>(id).performClick()
            assertEquals(type.name, shadowOf(home).nextStartedActivity.component?.className)
        }
    }

    @Test
    fun principleStepsAdvanceAndReset() {
        val page = launch(PrincipleActivity::class.java)
        assertEquals("0 / 4", page.text(R.id.principle_progress))
        val next = page.findViewById<View>(R.id.principle_next)
        next.performClick()
        assertTrue(page.findViewById<View>(R.id.step_1).isSelected)
        repeat(3) { next.performClick() }
        assertTrue(page.findViewById<View>(R.id.step_4).isSelected)
        assertFalse(page.findViewById<View>(R.id.step_1).isSelected)
        assertEquals("4 / 4", page.text(R.id.principle_progress))
        page.findViewById<View>(R.id.principle_reset).performClick()
        assertEquals("0 / 4", page.text(R.id.principle_progress))
    }

    @Test
    fun principleAutoPlayReachesLastStep() {
        val page = launch(PrincipleActivity::class.java)
        page.findViewById<View>(R.id.principle_play).performClick()
        idle(10)
        assertTrue(page.findViewById<View>(R.id.step_4).isSelected)
        assertEquals(page.getString(R.string.principle_play), page.text(R.id.principle_play))
    }

    @Test
    fun watchShowsReturnValuePidAndAliveState() {
        val me = ProcessHandle.current().pid().toInt()
        WatchController.starter = { context ->
            // 模拟 native：监听进程把 pid 写进 observedFile（后面带字面量 "/n"）
            File(context.filesDir, "observedFile").writeText("$me/n")
            4321
        }
        val page = launch(WatchActivity::class.java)
        assertEquals(page.getString(R.string.watch_not_started), page.text(R.id.watch_return))
        assertEquals(page.getString(R.string.watch_dead), page.text(R.id.watch_alive))

        page.findViewById<View>(R.id.watch_start).performClick()
        assertEquals("4321", page.text(R.id.watch_return))
        assertEquals(me.toString(), page.text(R.id.watch_pid))
        assertEquals(page.getString(R.string.watch_alive), page.text(R.id.watch_alive))
    }

    @Test
    fun watchReportsMissingNativeLibrary() {
        WatchController.starter = { throw UnsatisfiedLinkError("no uninstall") }
        val page = launch(WatchActivity::class.java)
        page.findViewById<View>(R.id.watch_start).performClick()
        assertTrue(page.text(R.id.watch_return).contains("UnsatisfiedLinkError"))
    }

    @Test
    fun watcherPidParsesFileFormat() {
        val ctx: Context = RuntimeEnvironment.getApplication()
        assertNull(WatchController.watcherPid(ctx))
        val file = File(ctx.filesDir, "observedFile")
        file.writeText("")
        assertNull(WatchController.watcherPid(ctx))
        file.writeText("1234/n")
        assertEquals(1234, WatchController.watcherPid(ctx))
    }

    @Test
    fun uninstallButtonLaunchesSystemUninstallForThisPackage() {
        val page = launch(UninstallActivity::class.java)
        page.findViewById<View>(R.id.uninstall_button).performClick()
        val intent: Intent = shadowOf(page).nextStartedActivityForResult.intent
        @Suppress("DEPRECATION")
        assertEquals(Intent.ACTION_UNINSTALL_PACKAGE, intent.action)
        assertEquals("package:${page.packageName}", intent.data.toString())
    }

    @Test
    fun alternativesLinkToReadmeSection() {
        val page = launch(AlternativesActivity::class.java)
        listOf(R.id.alt_fcm, R.id.alt_analytics, R.id.alt_play, R.id.alt_heartbeat, R.id.alt_readme).forEach { id ->
            page.findViewById<View>(id).performClick()
            val intent = shadowOf(page).nextStartedActivity
            assertNotNull(intent)
            assertEquals(AlternativesActivity.README_URL, intent.data.toString())
        }
    }
}
