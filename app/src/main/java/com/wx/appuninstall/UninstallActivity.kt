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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 演示三：卸载本应用。弹出系统卸载确认页，用来录屏演示真实结果：
 * 监听进程是否存活，卸载后有没有打开反馈页。
 *
 * @author venshine
 */
class UninstallActivity : BaseActivity() {

    private val uninstallLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // 取消卸载会回到这里；确认卸载后应用已不在，不会走到这里
            findViewById<TextView>(R.id.uninstall_result).setText(
                if (result.resultCode == Activity.RESULT_OK) R.string.uninstall_done else R.string.uninstall_cancelled)
            render()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uninstall)
        setToolbar(R.string.demo_uninstall, "ACTION_UNINSTALL_PACKAGE")
        findViewById<TextView>(R.id.uninstall_button).setOnClickListener { uninstall() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val pid = WatchController.watcherPid(this)
        val alive = pid != null && WatchController.isAlive(pid)
        findViewById<TextView>(R.id.uninstall_watch_state).text =
            getString(if (alive) R.string.uninstall_watch_alive else R.string.uninstall_watch_dead, pid ?: 0)
    }

    @Suppress("DEPRECATION") // ACTION_UNINSTALL_PACKAGE 自 API 29 起标记弃用，演示用途
    private fun uninstall() {
        // targetSdk 28+ 需在清单声明 REQUEST_DELETE_PACKAGES
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$packageName"))
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
        try {
            uninstallLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            findViewById<TextView>(R.id.uninstall_result).setText(R.string.uninstall_unavailable)
        }
    }
}
