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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChurnJudgeTest {

    private val timeout = 60_000L
    private fun device(seen: Long, uninstalled: Long? = null) = Device("id", "token", seen, uninstalled)

    @Test
    fun recentHeartbeatIsActive() {
        assertEquals(DeviceStatus.ACTIVE, ChurnJudge.judge(device(seen = 1_000), now = 30_000, timeoutMillis = timeout))
    }

    @Test
    fun exactlyAtTimeoutIsStillActive() {
        assertEquals(DeviceStatus.ACTIVE, ChurnJudge.judge(device(seen = 0), now = timeout, timeoutMillis = timeout))
    }

    @Test
    fun oneMillisecondPastTimeoutIsSuspected() {
        assertEquals(
            DeviceStatus.SUSPECTED_CHURN,
            ChurnJudge.judge(device(seen = 0), now = timeout + 1, timeoutMillis = timeout),
        )
    }

    @Test
    fun fcmVerdictBeatsHeartbeat() {
        // 刚有过心跳但 FCM 已判定令牌失效：以 FCM 为准
        assertEquals(
            DeviceStatus.UNINSTALLED,
            ChurnJudge.judge(device(seen = 10, uninstalled = 20), now = 30, timeoutMillis = timeout),
        )
    }
}

class DeviceRegistryTest {

    private var now = 1_000L
    private val registry = DeviceRegistry { now }

    @Test
    fun registerKeepsOldTokenWhenNoneGiven() {
        registry.register("a", "tok")
        now = 2_000
        val device = registry.register("a", null)
        assertEquals("tok", device.fcmToken)
        assertEquals(2_000L, device.lastSeenAt)
    }

    @Test
    fun heartbeatCreatesUnknownDevice() {
        val device = registry.heartbeat("new")
        assertNull(device.fcmToken)
        assertEquals(1, registry.snapshot().size)
    }

    @Test
    fun heartbeatAfterUninstallVerdictRevivesDevice() {
        registry.register("a", "tok")
        assertNotNull(registry.markUninstalled("a")?.uninstalledAt)
        now = 5_000
        assertNull(registry.heartbeat("a").uninstalledAt)
    }

    @Test
    fun markUninstalledOnUnknownDeviceReturnsNull() {
        assertNull(registry.markUninstalled("ghost"))
    }
}
