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

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 观察目标和事件记录，存在 SharedPreferences 里（最近 [MAX] 条，新的在前）。
 * 事件由广播接收器在主线程写入，[onChange] 同样在主线程回调。
 */
object EventStore {

    const val DEFAULT_TARGET = "com.wx.appuninstall"
    const val MAX = 100
    private const val FILE = "observer"

    /** 界面可见时设置，用于刷新列表 */
    var onChange: (() -> Unit)? = null

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun target(context: Context): String = prefs(context).getString("target", null) ?: DEFAULT_TARGET

    fun setTarget(context: Context, packageName: String) {
        prefs(context).edit().putString("target", packageName.trim().ifEmpty { DEFAULT_TARGET }).apply()
    }

    fun add(context: Context, event: RemovalEvent) {
        val list = JSONArray().put(toJson(event))
        val old = JSONArray(prefs(context).getString("events", "[]"))
        for (i in 0 until minOf(old.length(), MAX - 1)) list.put(old.get(i))
        prefs(context).edit().putString("events", list.toString()).apply()
        onChange?.invoke()
    }

    fun all(context: Context): List<RemovalEvent> {
        val array = JSONArray(prefs(context).getString("events", "[]"))
        return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove("events").apply()
        onChange?.invoke()
    }

    private fun toJson(e: RemovalEvent) = JSONObject()
        .put("type", e.type.name).put("pkg", e.packageName).put("uid", e.uid).put("time", e.timeMillis)

    private fun fromJson(o: JSONObject) =
        RemovalEvent(RemovalType.valueOf(o.getString("type")), o.getString("pkg"), o.getInt("uid"), o.getLong("time"))
}
