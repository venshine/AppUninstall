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

/**
 * 上传信息
 *
 * @author venshine
 */
public class UploadInfo {

    private String versionName;
    private int versionCode;
    private boolean isBrowser;

    public UploadInfo() {
    }

    public UploadInfo(String versionName, int versionCode, boolean isBrowser) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.isBrowser = isBrowser;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public boolean isBrowser() {
        return isBrowser;
    }

    public void setBrowser(boolean browser) {
        isBrowser = browser;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UploadInfo{");
        sb.append("versionName='").append(versionName).append('\'');
        sb.append(", versionCode=").append(versionCode);
        sb.append(", isBrowser=").append(isBrowser);
        sb.append('}');
        return sb.toString();
    }
}
