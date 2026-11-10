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
import android.content.pm.PackageManager
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

/**
 * 启动卸载监听并读取监听进程的状态。原生逻辑与监听原理不变：只是把 [Uninstall.watch] 的返回值
 * 和监听进程写进 observedFile 的 pid 取出来给界面显示。
 *
 * @author venshine
 */
object WatchController {

    /** native 里写 pid 的文件名，和 uninstall.cpp 的 APP_OBSERVED_FILE 对应 */
    private const val OBSERVED_FILE = "observedFile"

    /**
     * 启动监听的入口，返回 native `watch` 的返回值（父进程拿到的是 fork 出的子进程 pid）。
     * 原生库只能在设备上加载，JVM 单元测试里替换成假实现。
     */
    var starter: (Context) -> Int = ::startNative

    fun start(context: Context): Int = starter(context)

    /** 监听进程写进 observedFile 的 pid；文件还没有（或还没写完）时为 null */
    fun watcherPid(context: Context): Int? {
        val file = File(context.filesDir, OBSERVED_FILE)
        // native 用 "%d/n" 写入，后面跟着字面量 "/n"，只取开头的数字
        val digits = runCatching { file.readText() }.getOrNull()?.takeWhile { it.isDigit() }
        return digits?.toIntOrNull()?.takeIf { it > 1 }
    }

    /** 进程是否存活：向它发 0 号信号，只检测不打扰 */
    fun isAlive(pid: Int): Boolean = try {
        Os.kill(pid, 0)
        true
    } catch (e: ErrnoException) {
        // 没有权限发信号说明进程存在
        e.errno == OsConstants.EPERM
    }

    private fun startNative(context: Context): Int {
        val info = UploadInfo()
        try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode.toInt()
            else @Suppress("DEPRECATION") pkg.versionCode
            info.versionName = pkg.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            info.versionName = ""
        }
        info.isBrowser = true
        return Uninstall.watch(info)
    }
}
