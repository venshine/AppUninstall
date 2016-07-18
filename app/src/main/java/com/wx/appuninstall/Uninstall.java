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
package com.wx.appuninstall;

import android.content.Context;

import com.wx.android.common.util.AppUtils;

/**
 * 卸载
 *
 * @author venshine
 */
public class Uninstall {

    private static Uninstall mUninstall = null;

    private Uninstall() {
    }

    /**
     * 调用Native程序
     *
     * @param context
     * @return
     */
    public static Uninstall getInstance(Context context) {
        if (mUninstall == null) {
            mUninstall = new Uninstall();
            watch(create(context));
        }
        return mUninstall;
    }

    /**
     * 构造上传统计数据
     *
     * @param context
     * @return
     */
    private static UploadInfo create(Context context) {
        UploadInfo uploadInfo = new UploadInfo();
        uploadInfo.setVersionCode(AppUtils.getVersionCode(context));
        uploadInfo.setVersionName(AppUtils.getVersionName(context));
        uploadInfo.setBrowser(true);
        return uploadInfo;
    }

    /**
     * 监控卸载
     *
     * @param info
     * @return
     */
    public native static int watch(UploadInfo info);

    /**
     * 打开浏览器
     * @param serial
     */
    public native static void browser(String serial);

    // Used to load the 'uninstall' library on application startup.
    static {
        System.loadLibrary("uninstall");
    }

}
