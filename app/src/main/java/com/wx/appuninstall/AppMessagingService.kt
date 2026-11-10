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

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM 回调：令牌刷新时重新登记到服务端；收到服务端的探测消息（仅数据，不弹通知）时记录时间。
 * 依据：https://firebase.google.com/docs/cloud-messaging/android/get-started （onNewToken）
 */
class AppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        AppState.saveToken(this, token)
        Thread { ServerApi.register(InstallId.get(applicationContext), token) }.start()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        AppState.saveLastPush(this, System.currentTimeMillis())
    }
}
