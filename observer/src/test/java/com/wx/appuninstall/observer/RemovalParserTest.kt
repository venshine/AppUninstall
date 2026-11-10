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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemovalParserTest {

    private val target = "com.wx.appuninstall"

    private fun parse(
        action: String?, pkg: String? = target, replacing: Boolean = false,
        dataRemoved: Boolean = false, archival: Boolean = false,
    ) = RemovalParser.parse(action, pkg, target, replacing, dataRemoved, archival, uid = 10123, now = 42)

    @Test
    fun fullyRemovedIsUninstall() {
        assertEquals(
            RemovalEvent(RemovalType.UNINSTALLED, target, 10123, 42),
            parse(RemovalParser.ACTION_FULLY_REMOVED, dataRemoved = true),
        )
    }

    @Test
    fun removedWithArchivalIsArchiveNotUninstall() {
        assertEquals(RemovalType.ARCHIVED, parse(RemovalParser.ACTION_REMOVED, archival = true)?.type)
        // 归档优先于「数据是否移除」，避免误判成卸载
        assertEquals(RemovalType.ARCHIVED, parse(RemovalParser.ACTION_REMOVED, archival = true, dataRemoved = true)?.type)
    }

    @Test
    fun removedWithReplacingIsUpdate() {
        assertEquals(RemovalType.UPDATING, parse(RemovalParser.ACTION_REMOVED, replacing = true)?.type)
    }

    @Test
    fun removedPlainCasesFollowDataRemoved() {
        assertEquals(RemovalType.REMOVED_WITH_DATA, parse(RemovalParser.ACTION_REMOVED, dataRemoved = true)?.type)
        assertEquals(RemovalType.REMOVED_KEEP_DATA, parse(RemovalParser.ACTION_REMOVED)?.type)
    }

    @Test
    fun otherPackagesAndActionsAreIgnored() {
        assertNull(parse(RemovalParser.ACTION_FULLY_REMOVED, pkg = "com.other"))
        assertNull(parse(RemovalParser.ACTION_FULLY_REMOVED, pkg = null))
        assertNull(parse("android.intent.action.PACKAGE_ADDED"))
        assertNull(parse(null))
    }
}
