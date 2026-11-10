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

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** 后台心跳：向服务端上报「我还在」。失败最多重试两次，之后等下一个周期。 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val reply = ServerApi.heartbeat(InstallId.get(applicationContext))
        return when {
            reply.ok -> Result.success()
            runAttemptCount < 2 -> Result.retry()
            else -> Result.success()
        }
    }
}

object HeartbeatScheduler {

    private const val NAME = "heartbeat"

    /** 周期任务的最小间隔是 15 分钟（官方文档），实际执行时间还受省电策略影响 */
    const val PERIOD_MINUTES = 15L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        AppState.prefs(context).edit().putBoolean("hb_scheduled", true).apply()
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAME)
        AppState.prefs(context).edit().putBoolean("hb_scheduled", false).apply()
    }

    fun isScheduled(context: Context): Boolean = AppState.prefs(context).getBoolean("hb_scheduled", false)
}
