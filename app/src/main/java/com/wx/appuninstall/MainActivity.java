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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.wx.android.common.util.PackageUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Demo
 *
 * @author venshine
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void watch(View view) {
        Uninstall.getInstance(this);
    }

    public void browser(View view) {
        Uninstall.browser(getUserSerial());
    }

    public void uninstall(View view) {
        PackageUtils.uninstall(this, getPackageName());
    }

    // 由于targetSdkVersion低于17，只能通过反射获取
    @Nullable
    private String getUserSerial() {
        Object userManager = getSystemService(Context.USER_SERVICE);
        if (userManager == null) {
            return null;
        }
        try {
            Method myUserHandleMethod = android.os.Process.class.getMethod(
                    "myUserHandle", (Class<?>[]) null);
            Object myUserHandle = myUserHandleMethod.invoke(
                    android.os.Process.class, (Object[]) null);

            Method getSerialNumberForUser = userManager.getClass().getMethod(
                    "getSerialNumberForUser", myUserHandle.getClass());
            long userSerial = (Long) getSerialNumberForUser.invoke(userManager,
                    myUserHandle);
            return String.valueOf(userSerial);
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return null;
    }

}
