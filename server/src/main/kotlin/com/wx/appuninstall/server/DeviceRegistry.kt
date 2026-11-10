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
 * 设备登记表，内存存储（进程重启即清空，演示用途）。所有方法线程安全。
 *
 * 任何登记或心跳都证明 App 还在，所以会清掉此前的「已卸载」标记。
 */
class DeviceRegistry(private val clock: () -> Long = System::currentTimeMillis) {

    private val devices = LinkedHashMap<String, Device>()

    @Synchronized
    fun register(installId: String, token: String?): Device {
        val old = devices[installId]
        val device = Device(installId, token ?: old?.fcmToken, clock())
        devices[installId] = device
        return device
    }

    @Synchronized
    fun heartbeat(installId: String): Device {
        val old = devices[installId]
        val device = Device(installId, old?.fcmToken, clock())
        devices[installId] = device
        return device
    }

    /** FCM 返回 UNREGISTERED 后调用；设备不存在时返回 null */
    @Synchronized
    fun markUninstalled(installId: String): Device? {
        val old = devices[installId] ?: return null
        val device = old.copy(uninstalledAt = clock())
        devices[installId] = device
        return device
    }

    @Synchronized
    fun snapshot(): List<Device> = devices.values.toList()

    fun now(): Long = clock()
}
