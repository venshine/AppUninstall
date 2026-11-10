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

/** 对比页的一行：每个方案的原理、时效、误判、需要的账号与权限，以及官方出处 */
data class Approach(
    val id: String,
    val title: String,
    val principle: String,
    val latency: String,
    val falsePositive: String,
    val requires: String,
    /** 出处：标题加链接，没有一手来源的方案必须在 [falsePositive] 或 [principle] 里写明「自行设计」 */
    val sources: List<Pair<String, String>>,
)

/**
 * 各方案对比数据。每一句结论都要能在 [Approach.sources] 里找到出处；
 * 官方文档没写的内容写「官方未说明」，不凭印象补。
 */
object CompareData {

    val approaches: List<Approach> = listOf(
        Approach(
            id = "fcm",
            title = "FCM 令牌失效",
            principle = "服务端向已登记的令牌发消息，令牌失效时 FCM HTTP v1 返回 UNREGISTERED（HTTP 404），据此判定该安装已不在。",
            latency = "只有发送时才知道，不是实时回调。Android 注册 270 天不活跃会过期；默认一个月没连接过的令牌视为 stale。",
            falsePositive = "官方对 UNREGISTERED 的解释是令牌不再有效，原因包括应用卸载、令牌过期、应用更新后收不到消息，不等于用户一定卸载。INVALID_ARGUMENT 也可能是令牌格式或载荷错误，不能当卸载。",
            requires = "Firebase 项目、google-services.json、服务端持有服务账号密钥并调用 HTTP v1；客户端无需额外权限。",
            sources = listOf(
                "错误码表" to "https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode",
                "错误排查与 UNREGISTERED" to "https://firebase.google.com/docs/cloud-messaging/error-codes",
                "管理令牌" to "https://firebase.google.com/docs/cloud-messaging/manage-tokens",
            ),
        ),
        Approach(
            id = "analytics",
            title = "Analytics app_remove",
            principle = "Firebase Analytics 自动收集的 Android 事件，官方描述为「应用包被移除（卸载）时」。",
            latency = "按聚合报表和 BigQuery 导出使用：关联 BigQuery 后通常要等到下一次每日同步才有数据。单次事件的到达延迟官方未说明。",
            falsePositive = "统计所有安装来源的移除，不限 Google Play；与 Play 管理中心的卸载指标口径不同，两边数字对不上是预期现象。",
            requires = "Firebase 项目与 Analytics；导出 BigQuery 需在控制台关联（Spark 计划可用沙盒，限制见官方）。无需额外权限，无逐用户回调。",
            sources = listOf(
                "自动收集的事件" to "https://support.google.com/analytics/answer/9234069",
                "关联 BigQuery" to "https://firebase.google.com/docs/projects/bigquery-export",
                "导出表结构" to "https://support.google.com/analytics/answer/7029846",
            ),
        ),
        Approach(
            id = "heartbeat",
            title = "自建心跳",
            principle = "App 定期向自己的服务端上报，服务端超过阈值没收到就标记为「疑似流失」。这是自行设计的启发式，没有官方出处。",
            latency = "取决于阈值。WorkManager 周期任务最小间隔 15 分钟，实际执行还受省电策略影响，阈值要留足余量。",
            falsePositive = "分不清「卸载」和「只是不用了 / 长时间离线 / 被系统限制后台」，只能叫疑似流失。",
            requires = "自己的服务端；客户端需要 INTERNET 权限。",
            sources = listOf(
                "WorkManager 周期任务" to "https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work",
            ),
        ),
        Approach(
            id = "observer",
            title = "观察者 App 监听广播",
            principle = "另一个已安装的 App 监听 PACKAGE_FULLY_REMOVED：被卸载的包自己收不到 PACKAGE_REMOVED，但其他 App 可以。该广播在 Android 8 的隐式广播限制中有例外，可在清单里静态注册。",
            latency = "近实时，系统发出广播时即触发。",
            falsePositive = "应用归档（Android 15）会发 PACKAGE_REMOVED 且带 EXTRA_ARCHIVAL，用户数据保留；更新替换带 EXTRA_REPLACING。只认 PACKAGE_FULLY_REMOVED 可排除这两种。",
            requires = "用户必须同时安装观察者 App，且它不处于被强行停止的状态；Android 11+ 建议声明 <queries> 包可见性（官方未明确说明对广播的影响，待真机确认）。",
            sources = listOf(
                "PACKAGE_REMOVED / FULLY_REMOVED / EXTRA_ARCHIVAL" to "https://developer.android.com/reference/android/content/Intent",
                "隐式广播例外清单" to "https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions",
                "包可见性" to "https://developer.android.com/training/package-visibility",
            ),
        ),
        Approach(
            id = "play",
            title = "Google Play 管理中心",
            principle = "管理中心的用户流失 / 设备流失指标，统计卸载以及超过 30 天不再使用设备的情况。",
            latency = "按天汇总。",
            falsePositive = "官方定义里把「卸载」和「30 天以上没再使用设备」都算作流失，不是纯卸载。",
            requires = "通过 Google Play 分发的应用和管理中心账号；只有汇总数据，没有逐用户回调。",
            sources = listOf("Play 管理中心帮助" to "https://support.google.com/googleplay/android-developer/answer/139628"),
        ),
        Approach(
            id = "legacy",
            title = "fork + inotify（2016 旧方案）",
            principle = "fork 原生子进程监听应用数据目录被删除。见仓库里的 legacy 模块。",
            latency = "原设计为卸载瞬间。",
            falsePositive = "在现代 Android 上不可靠：作者在一台 Android 17 真机上实测，监听进程随卸载一起被杀，反馈页没有打开；其他版本未验证。",
            requires = "NDK 原生代码，无需账号。",
            sources = listOf("仓库 README 的限制说明与来源" to "https://github.com/venshine/AppUninstall/tree/master/legacy#limitations-on-modern-android"),
        ),
    )
}
