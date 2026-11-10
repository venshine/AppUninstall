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

import android.content.Intent

/** 观察者能区分的几种「包被移除」 */
enum class RemovalType {
    /** PACKAGE_FULLY_REMOVED：代码和数据都已移除，即真正卸载 */
    UNINSTALLED,

    /** PACKAGE_REMOVED + EXTRA_ARCHIVAL：Android 15 应用归档，APK 与缓存被移除，用户数据保留 */
    ARCHIVED,

    /** PACKAGE_REMOVED + EXTRA_REPLACING：更新替换，紧接着会有 PACKAGE_ADDED */
    UPDATING,

    /** PACKAGE_REMOVED，数据被一并移除：随后通常会有 PACKAGE_FULLY_REMOVED */
    REMOVED_WITH_DATA,

    /** PACKAGE_REMOVED，数据保留，既不是归档也不是更新（例如保留数据的卸载） */
    REMOVED_KEEP_DATA,
}

data class RemovalEvent(val type: RemovalType, val packageName: String, val uid: Int, val timeMillis: Long)

/**
 * 把系统广播解析成 [RemovalEvent]，纯函数，可在 JVM 上直接测试。
 *
 * 依据（均为官方 Intent 参考）：
 * - PACKAGE_FULLY_REMOVED 只在 EXTRA_DATA_REMOVED 为 true 且 EXTRA_REPLACING 为 false 时发出；
 * - EXTRA_ARCHIVAL 自 API 35 起用于 PACKAGE_ADDED / PACKAGE_REMOVED，表示正在归档；
 * - 归档时「APK 和缓存被移除、用户数据保留」（PackageInstaller.requestArchive）。
 * https://developer.android.com/reference/android/content/Intent
 */
object RemovalParser {

    const val ACTION_REMOVED = "android.intent.action.PACKAGE_REMOVED"
    const val ACTION_FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED"

    /** Intent.EXTRA_ARCHIVAL 的值；用字面量避免在低版本上触发 API 检查 */
    const val EXTRA_ARCHIVAL = "android.intent.extra.ARCHIVAL"

    fun parse(
        action: String?,
        packageName: String?,
        target: String,
        replacing: Boolean,
        dataRemoved: Boolean,
        archival: Boolean,
        uid: Int,
        now: Long,
    ): RemovalEvent? {
        if (packageName == null || packageName != target) return null
        val type = when (action) {
            ACTION_FULLY_REMOVED -> RemovalType.UNINSTALLED
            ACTION_REMOVED -> when {
                archival -> RemovalType.ARCHIVED
                replacing -> RemovalType.UPDATING
                dataRemoved -> RemovalType.REMOVED_WITH_DATA
                else -> RemovalType.REMOVED_KEEP_DATA
            }
            else -> return null
        }
        return RemovalEvent(type, packageName, uid, now)
    }

    fun parse(intent: Intent, target: String, now: Long): RemovalEvent? = parse(
        action = intent.action,
        packageName = intent.data?.schemeSpecificPart,
        target = target,
        replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false),
        dataRemoved = intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false),
        archival = intent.getBooleanExtra(EXTRA_ARCHIVAL, false),
        uid = intent.getIntExtra(Intent.EXTRA_UID, -1),
        now = now,
    )
}
