/*
 * Copyright (c) 2016-present. 贵州纳雍穿青人李裕江 and All Contributors.
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.linsheng.FATJS;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.linsheng.FATJS.config.GlobalVariableHolder; // Import GlobalVariableHolder
import com.linsheng.FATJS.server.FatJSHttpServer;
import java.io.IOException;

public class WrapperApplication extends Application {
    private static final String TAG = "WrapperApplication";
    private boolean privacyPolicyAgreed = true;
    private FatJSHttpServer httpServer;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        privacyPolicyAgreed = true; // Assuming agreement for now, real app would check this
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up global context as early as possible
        GlobalVariableHolder.context = getApplicationContext();

        //注意APP合规性，若最终用户未同意隐私政策则不要调用
        if (privacyPolicyAgreed) {
            // Ensure V8 runtime is initialized
            GlobalVariableHolder.ensureV8RuntimeInitialized();

            // Initialize and start the HTTP server
            try {
                httpServer = new FatJSHttpServer(); // Uses default port
                // httpServer = new FatJSHttpServer(8081); // Or specify a custom port
                Log.i(TAG, "FatJSHttpServer started after V8 init.");
            } catch (IOException e) {
                Log.e(TAG, "Failed to start FatJSHttpServer", e);
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (httpServer != null) {
            httpServer.stopServer();
            Log.i(TAG, "FatJSHttpServer stopped.");
        }
    }
}
