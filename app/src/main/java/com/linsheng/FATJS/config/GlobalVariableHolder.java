package com.linsheng.FATJS.config;

import static com.linsheng.FATJS.node.AccUtils.printLogMsg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.caoccao.javet.interop.V8Runtime;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linsheng.FATJS.activitys.aione_editor.MainActivity;
import com.linsheng.FATJS.utils.FileUtils;
import com.linsheng.FATJS.utils.StringUtils;
import com.linsheng.FATJS.node.AccUtils; // For loadScriptFromAssets
import com.linsheng.FATJS.node.TaskBase;
import com.linsheng.FATJS.node.UiObject;
import com.linsheng.FATJS.node.App; // Assuming App class is com.linsheng.FATJS.node.App
import com.linsheng.FATJS.okhttp3.HttpUtils;
import com.linsheng.FATJS.okhttp3.WebSocketUtils;
import com.linsheng.FATJS.ntptime.NtpService;
import android.content.Intent; // For Intent class
import android.util.Log;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.converters.JavetProxyConverter;

import java.io.IOException;
import java.util.HashMap;

public class GlobalVariableHolder {
    private static final String TAG = "GlobalVariableHolder";
    public static boolean CRON_TASK = false; // 定时任务是否开启
    public static String CRON_TASK_FILE = "定时任务配置.txt"; // 定时任务配置
    public static String CRON_TASK_FILE_TEST = "test_demo.js"; // 定时任务测试
    public static boolean DEV_MODE = false; // FATJS 的开发者模式
    public static V8Runtime v8Runtime;
    @SuppressLint("SdCardPath")
    public static final String ABSOLUTE_PATH = "/sdcard/fatjs/";
    public static final String PATH = "/fatjs/";
    public static String FATJS_INFO = "\n\n\n\n\n\n\n\n\n\nauthor: 林笙\n\nwx: FATJS_Lin\n\nGitHub: FATJS";
    public static String ANDROID_ID = "";
    public static String pseudoID = "";
    public static String guid = "";
    public static String OAID = "";
    public static String PHONE_NAME = "";

    // 悬浮窗展示的文字
    public static TextView btnTextView = null;
    // 上下文
    public static Context context = null;
    public static MainActivity mainActivity = null;
    // ll
    public static LinearLayout ll = null;
    public static String checkedFileName = ""; // 当前选中的脚本文件名称
    // 当前ActivityName
    public static volatile String currentActivityName = "";
    // 屏幕宽高
    public static int text_size = 11;
    public static int mWidth = 1440;
    public static int mHeight = 3040;
    public static int __mHeight = -1; //去掉导航栏和状态栏的高度
    public static int statusBarHeight = -1; //状态栏的高度
    public static int navigationBarHeight = -1; //导航栏的高度
    public static boolean navigationBarOpen = true; //导航栏是否开启
    // 悬浮球位置
    public static int float_x = 0;
    public static int float_y = 0;

    public static String tag = "fatjslog";
    // 停顿时长
    public static final int waitHrefOfSecond  =   500;
    public static final int waitOneSecond     =   1000;
    public static final int waitTwoSecond     =   2000;
    public static final int waitThreeSecond   =   3000;
    public static final int waitFourSecond    =   4000;
    public static final int waitFiveSecond    =   5000;
    public static final int waitSixSecond     =   6000;
    public static boolean isStop = false; // 暂停标识
    public static boolean isRunning = false; // 是否有任务在运行
    public static boolean killThread = false; // 是否强制关闭
    public static boolean isOpenFloatWin = false; // 悬浮窗是否打开
    public static void saveConfig() {

        // 使用Gson的JsonObject来构建和填充数据
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("DEV_MODE", DEV_MODE);
        jsonObject.addProperty("CRON_TASK", CRON_TASK);
        jsonObject.addProperty("CHECKED_FILE_NAME", checkedFileName);

        // 将JsonObject转换为字符串
        String configString = jsonObject.toString();
        printLogMsg(configString, 0);
        FileUtils.writeToTxt("/sdcard/fatjs/config.json", configString);
    }

    public static void reviewConfig() {
        String readFile = FileUtils.readFile("/sdcard/fatjs/config.json");
        if (StringUtils.isEmpty(readFile)) {
            printLogMsg("reviewConfig empty", 0);
            saveConfig();
            return;
        }
        // 使用Gson的JsonParser来解析字符串
        JsonObject parseObject = JsonParser.parseString(readFile).getAsJsonObject();

        // 从解析后的JsonObject中获取特定的值
        if (parseObject.has("DEV_MODE")) {
            DEV_MODE = parseObject.get("DEV_MODE").getAsBoolean();
        }
        if (parseObject.has("CRON_TASK")) {
            CRON_TASK = parseObject.get("CRON_TASK").getAsBoolean();
        }
        if (parseObject.has("CHECKED_FILE_NAME")) {
            checkedFileName = parseObject.get("CHECKED_FILE_NAME").getAsString();
        }
    }
    public static HashMap<String, Object> hashMapBuffer;

    private static volatile boolean isV8RuntimeInitialized = false;
    private static final Object v8InitLock = new Object();

    public static void ensureV8RuntimeInitialized() {
        if (isV8RuntimeInitialized && v8Runtime != null) {
            return;
        }
        synchronized (v8InitLock) {
            if (isV8RuntimeInitialized && v8Runtime != null) {
                return;
            }
            try {
                Log.i(TAG, "Initializing V8 runtime...");
                if (context == null) {
                    Log.e(TAG, "Context is null. Cannot initialize V8 runtime. Ensure Application context is set first.");
                    // Consider throwing an exception or handling this state more gracefully
                    // For now, we'll return and subsequent calls will try again if context gets set.
                    return;
                }

                v8Runtime = V8Host.getV8Instance().createV8Runtime();
                v8Runtime.setConverter(new JavetProxyConverter()); // Configure to call Java methods

                // Register global objects, similar to TaskBase.initJavet()
                v8Runtime.getGlobalObject().set("engines", TaskBase.class);
                v8Runtime.getGlobalObject().set("http", HttpUtils.class);
                v8Runtime.getGlobalObject().set("websocket", WebSocketUtils.class);
                v8Runtime.getGlobalObject().set("UiObject", UiObject.class);
                // Assuming App class is com.linsheng.FATJS.node.App, if not, this needs correction
                v8Runtime.getGlobalObject().set("app", com.linsheng.FATJS.node.App.class);
                v8Runtime.getGlobalObject().set("Intent", Intent.class);
                v8Runtime.getGlobalObject().set("ntpService", NtpService.class);
                // Add any other global objects that are usually set up

                // Load and execute base.js
                String baseJsContent = AccUtils.loadScriptFromAssets("base.js");
                if (baseJsContent != null && !baseJsContent.isEmpty()) {
                    v8Runtime.getExecutor(baseJsContent).executeVoid();
                    Log.i(TAG, "base.js loaded and executed in V8 runtime.");
                } else {
                    Log.e(TAG, "base.js is empty or could not be loaded.");
                    // Handle this error - perhaps throw an exception
                }

                isV8RuntimeInitialized = true;
                Log.i(TAG, "V8 runtime initialized successfully.");

            } catch (JavetException e) {
                Log.e(TAG, "JavetException during V8 runtime initialization: " + e.getMessage(), e);
                isV8RuntimeInitialized = false; // Reset flag on failure
                if (v8Runtime != null) {
                    try {
                        v8Runtime.close();
                    } catch (JavetException je) {
                        Log.e(TAG, "JavetException while closing runtime after init failure: " + je.getMessage(), je);
                    }
                    v8Runtime = null;
                }
                // Optionally, rethrow or handle more specifically
            } catch (IOException e) {
                Log.e(TAG, "IOException during V8 runtime initialization (loading base.js): " + e.getMessage(), e);
                isV8RuntimeInitialized = false; // Reset flag on failure
                 if (v8Runtime != null) {
                    try {
                        v8Runtime.close();
                    } catch (JavetException je) {
                         Log.e(TAG, "JavetException while closing runtime after init failure: " + je.getMessage(), je);
                    }
                    v8Runtime = null;
                }
                // Optionally, rethrow or handle more specifically
            } catch (Exception e) {
                Log.e(TAG, "Generic Exception during V8 runtime initialization: " + e.getMessage(), e);
                isV8RuntimeInitialized = false; // Reset flag on failure
                if (v8Runtime != null) {
                    try {
                        v8Runtime.close();
                    } catch (JavetException je) {
                        Log.e(TAG, "JavetException while closing runtime after init failure: " + je.getMessage(), je);
                    }
                    v8Runtime = null;
                }
                // Optionally, rethrow or handle more specifically
            }
        }
    }
}
