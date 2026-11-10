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

/** 设备的流失状态：UNINSTALLED 只来自 FCM 的 UNREGISTERED，SUSPECTED_CHURN 只来自心跳超时 */
enum class DeviceStatus { ACTIVE, SUSPECTED_CHURN, UNINSTALLED }

/**
 * 一台已登记的设备。[installId] 由 App 自己生成（卸载重装会变），不是系统设备标识。
 * [lastSeenAt] 是最近一次登记或心跳的时间（毫秒），[uninstalledAt] 非空表示 FCM 已判定令牌失效。
 */
data class Device(
    val installId: String,
    val fcmToken: String?,
    val lastSeenAt: Long,
    val uninstalledAt: Long? = null,
)

/**
 * 状态判定，纯函数。优先级：已被 FCM 判定卸载 > 心跳超时 > 活跃。
 * 超时用「严格大于」：恰好等于 [timeoutMillis] 仍算活跃。
 */
object ChurnJudge {
    fun judge(device: Device, now: Long, timeoutMillis: Long): DeviceStatus = when {
        device.uninstalledAt != null -> DeviceStatus.UNINSTALLED
        now - device.lastSeenAt > timeoutMillis -> DeviceStatus.SUSPECTED_CHURN
        else -> DeviceStatus.ACTIVE
    }
}
