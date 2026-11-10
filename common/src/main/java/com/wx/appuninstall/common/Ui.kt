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
package com.wx.appuninstall.common

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes

/**
 * 用代码搭界面的小工具：样式全部来自 styles.xml，各页面不再重复写 XML 布局
 *
 * @author venshine
 */
object Ui {

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun text(context: Context, @StyleRes style: Int, value: CharSequence): TextView =
        TextView(context, null, 0, style).apply { text = value }

    fun body(context: Context, value: CharSequence) = text(context, R.style.Text_Body, value)

    fun note(context: Context, value: CharSequence) = text(context, R.style.Text_Note, value)

    fun mono(context: Context, value: CharSequence) = text(context, R.style.Text_Mono, value)

    fun section(context: Context, value: CharSequence) = text(context, R.style.Text_Section, value)

    fun caption(context: Context, value: CharSequence) = text(context, R.style.Text_Caption, value)

    /** 圆角描边卡片，纵向排列 */
    fun card(context: Context, padding: Int = 16): LinearLayout =
        LinearLayout(context, null, 0, R.style.Card).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(context, padding)
            setPadding(p, p, p, p)
        }

    fun button(context: Context, label: CharSequence, outline: Boolean = false): TextView =
        TextView(context, null, 0, if (outline) R.style.Button_Outline else R.style.Button).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 54))
        }

    /** 状态标签：ok 为绿色，否则为红色 */
    fun chip(context: Context, label: CharSequence, ok: Boolean): TextView =
        text(context, R.style.Text_Mono, label).apply {
            setBackgroundResource(if (ok) R.drawable.bg_chip_ok else R.drawable.bg_chip_bad)
            setTextColor(context.getColor(if (ok) R.color.ok_ink else R.color.bad_ink))
            textSize = 11f
            gravity = Gravity.CENTER
            val h = dp(context, 8)
            val v = dp(context, 4)
            setPadding(h, v, h, v)
        }

    /** 给视图加上上边距后加入纵向容器；[wrap] 为 true 时宽度按内容，否则占满 */
    fun LinearLayout.add(child: View, topDp: Int = 12, wrap: Boolean = false): View {
        addView(child, LinearLayout.LayoutParams(
            if (wrap) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT,
            // 保留子视图自己设好的高度（如按钮的 54dp），没有设时才按内容
            child.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = dp(context, topDp) })
        return child
    }
}
