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

import android.view.View
import android.widget.LinearLayout
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 回归：真机上按钮只有文字高。Ui.add 用 WRAP_CONTENT 覆盖了 Ui.button 设好的 54dp 高度。
 */
@RunWith(RobolectricTestRunner::class)
class ButtonHeightTest {

    @Test
    fun buttonKeepsItsHeightWhenAddedToAPage() {
        val context = RuntimeEnvironment.getApplication()
        val page = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val button = Ui.button(context, "Fetch token")
        page.add(button)

        val width = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        page.measure(width, height)

        assertEquals(Ui.dp(context, 54), button.measuredHeight)
    }
}
