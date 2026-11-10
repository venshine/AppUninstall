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
package com.wx.appuninstall.observer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * 观察者冒烟测试：广播经真实的 Intent 解析、存储，再显示到列表；深浅色都能启动。
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class ObserverSmokeTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val created = ArrayList<ActivityController<*>>()

    @Before
    fun setUp() {
        EventStore.setTarget(context, EventStore.DEFAULT_TARGET)
        EventStore.clear(context)
    }

    @After
    fun tearDown() {
        created.forEach { runCatching { it.destroy() } }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun launch(): ObserverActivity {
        val controller = Robolectric.buildActivity(ObserverActivity::class.java).setup()
        created.add(controller)
        shadowOf(Looper.getMainLooper()).idle()
        return controller.get()
    }

    private fun removalIntent(action: String, pkg: String, configure: Intent.() -> Unit = {}) =
        Intent(action, Uri.parse("package:$pkg")).apply(configure)

    private fun Activity.events() = findViewById<LinearLayout>(R.id.events_container)

    @Test
    fun launchesInLightAndDark() {
        for (mode in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            AppCompatDelegate.setDefaultNightMode(mode)
            assertNotNull(launch().findViewById<View>(android.R.id.content))
        }
    }

    @Test
    fun fullyRemovedBroadcastIsRecordedAndShown() {
        val page = launch()
        RemovalReceiver().onReceive(context, removalIntent(RemovalParser.ACTION_FULLY_REMOVED, EventStore.DEFAULT_TARGET) {
            putExtra(Intent.EXTRA_UID, 10456)
            putExtra(Intent.EXTRA_DATA_REMOVED, true)
        })
        val stored = EventStore.all(context).single()
        assertEquals(RemovalType.UNINSTALLED, stored.type)
        assertEquals(10456, stored.uid)
        // 界面可见时 onChange 会立即刷新列表
        assertEquals(1, page.events().childCount)
        assertTrue((page.events().getChildAt(0) as LinearLayout).getChildAt(0).let { (it as TextView).text == "UNINSTALLED" })
    }

    @Test
    fun archiveIntentIsNotRecordedAsUninstall() {
        RemovalReceiver().onReceive(context, removalIntent(RemovalParser.ACTION_REMOVED, EventStore.DEFAULT_TARGET) {
            putExtra(RemovalParser.EXTRA_ARCHIVAL, true)
        })
        assertEquals(listOf(RemovalType.ARCHIVED), EventStore.all(context).map { it.type })
    }

    @Test
    fun otherPackagesAreIgnored() {
        RemovalReceiver().onReceive(context, removalIntent(RemovalParser.ACTION_FULLY_REMOVED, "com.other.app"))
        assertTrue(EventStore.all(context).isEmpty())
    }

    @Test
    fun targetCanBeChangedAndClearWorks() {
        val page = launch()
        page.findViewById<EditText>(R.id.target_input).setText("com.other.app")
        page.findViewById<View>(R.id.target_save).performClick()
        assertEquals("com.other.app", EventStore.target(context))
        RemovalReceiver().onReceive(context, removalIntent(RemovalParser.ACTION_FULLY_REMOVED, "com.other.app"))
        assertEquals(1, EventStore.all(context).size)
        page.findViewById<View>(R.id.events_clear).performClick()
        assertTrue(EventStore.all(context).isEmpty())
    }

    @Test
    fun storeKeepsOnlyNewestHundred() {
        repeat(EventStore.MAX + 5) { EventStore.add(context, RemovalEvent(RemovalType.UNINSTALLED, "p", it, it.toLong())) }
        val all = EventStore.all(context)
        assertEquals(EventStore.MAX, all.size)
        assertEquals(EventStore.MAX + 4, all.first().uid)
    }
}
