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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 接收包移除广播。清单里静态注册 PACKAGE_FULLY_REMOVED，界面打开时再动态注册 PACKAGE_REMOVED，
 * 两者共用这个类：解析后只记录监听目标的事件。
 */
class RemovalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = RemovalParser.parse(intent, EventStore.target(context), System.currentTimeMillis()) ?: return
        EventStore.add(context, event)
    }
}
