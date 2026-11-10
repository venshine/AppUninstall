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

import android.os.Bundle
import android.widget.LinearLayout
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.R as CR
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add

/**
 * 演示四：各方案对比。内容来自 [CompareData]，每个方案列出原理、时效、误判、账号与权限，并附官方出处链接。
 *
 * @author venshine
 */
class CompareActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = buildPage(getString(R.string.compare_title), "${CompareData.approaches.size}")
        page.add(Ui.body(this, getString(R.string.compare_desc)), 16)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            id = R.id.compare_list
        }
        page.add(list, 0)
        for (a in CompareData.approaches) list.add(approachCard(a), 12)
    }

    private fun approachCard(a: Approach): LinearLayout {
        val card = Ui.card(this)
        card.add(Ui.text(this, CR.style.Text_CardTitle, a.title), 0)
        row(card, R.string.compare_principle, a.principle)
        row(card, R.string.compare_latency, a.latency)
        row(card, R.string.compare_false, a.falsePositive)
        row(card, R.string.compare_requires, a.requires)
        card.add(Ui.caption(this, getString(R.string.compare_sources)), 12)
        for ((label, url) in a.sources) {
            card.add(Ui.text(this, CR.style.Text_Mono_Chip, label).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }, 6, wrap = true)
        }
        return card
    }

    private fun row(card: LinearLayout, label: Int, value: String) {
        card.add(Ui.caption(this, getString(label)), 12)
        card.add(Ui.text(this, CR.style.Text_CardDesc, value), 2)
    }
}
