# FATJS MCP (Machine Control Protocol) 功能列表

本文档列出了可通过 HTTP MCP 服务调用的主要 JavaScript 功能。这些功能最终映射到 FATJS 应用的内部操作。

**服务地址**: `http://<安卓设备IP地址>:8080/run-script` (默认端口为 8080)
**请求方法**: `POST`
**请求格式**: JSON (`Content-Type: application/json`)

**请求体结构**:

*   使用 `functionName` 和 `args`:
    ```json
    {
        "functionName": "<JS函数名>",
        "args": [<参数1>, <参数2>, ...]
    }
    ```
*   或者使用 `script` 直接执行JS代码:
    ```json
    {
        "script": "<你的JavaScript代码>"
    }
    ```

## 主要功能列表

以下列表基于 `base.js` 和 `FATJS-code-snippets.txt` 中的核心功能。注意，任何在 `base.js` 中定义的全局函数，或者可以通过 `engines` 对象 (`task` 变量在JS中) 调用的方法理论上都可以通过 MCP 执行。

### 1. 日志与悬浮窗操作

*   `showLog()`: 显示悬浮窗日志（正常尺寸）。
    *   请求: `{"functionName": "showLog"}`
*   `hideLog()`: 隐藏悬浮窗日志（最小化）。
    *   请求: `{"functionName": "hideLog"}`
*   `fullScreenLog()`: 全屏显示悬浮窗日志（最大化）。
    *   请求: `{"functionName": "fullScreenLog"}`
*   `clearLog()`: 清除悬浮窗日志内容。
    *   请求: `{"functionName": "clearLog"}`
*   `print(message)`: 在悬浮窗日志中打印信息。
    *   请求: `{"functionName": "print", "args": ["日志内容"]}`
*   `floatLocation()`: 获取悬浮球最右侧坐标。返回 `[x, y]`。
    *   请求: `{"functionName": "floatLocation"}`

### 2. 设备控制与导航

*   `home()`: 返回桌面。
    *   请求: `{"functionName": "home"}`
*   `back()`: 执行返回操作。
    *   请求: `{"functionName": "back"}`
*   `backToDesk()`: 连续返回，直到桌面。
    *   请求: `{"functionName": "backToDesk"}`
*   `sleep(milliseconds)`: 线程休眠指定毫秒数。
    *   请求: `{"functionName": "sleep", "args": [2000]}`
*   `capture(filePath)`: 截屏并保存到指定路径 (如 `/sdcard/screenshot.png`)。
    *   请求: `{"functionName": "capture", "args": ["/sdcard/myscreen.png"]}`
*   `lockScreen()`: 锁屏。
    *   请求: `{"functionName": "lockScreen"}`
*   `open(appName)`: 根据应用名称打开App。
    *   请求: `{"functionName": "open", "args": ["微信"]}`
*   `openPkName(packageName)`: 根据应用包名打开App。
    *   请求: `{"functionName": "openPkName", "args": ["com.tencent.mm"]}`
*   `activityName()`: 获取当前 Activity 名称。
    *   请求: `{"functionName": "activityName"}`
*   `width`: 获取屏幕宽度 (这是一个变量，可以通过 `{"script": "width"}` 获取)。
*   `height`: 获取屏幕高度 (同上, `{"script": "height"}`).
*   `screenSize()`: 获取屏幕宽和高。返回 `[width, height]`。
    *   请求: `{"functionName": "screenSize"}`

### 3. UI交互：点击与滑动

*   `click(x, y)`: 手势点击坐标。
    *   请求: `{"functionName": "click", "args": [100, 200]}`
*   `clickExactPoint(x, y, duration)`: 手势点击精确坐标及时长。
    *   请求: `{"functionName": "clickExactPoint", "args": [100, 200, 150]}`
*   `clickPercentPoint(xPercent, yPercent, duration)`: 手势点击百分比坐标及时长 (如0.5代表50%)。
    *   请求: `{"functionName": "clickPercentPoint", "args": [0.5, 0.3, 100]}`
*   `doubleClick(x, y)`: 双击坐标。
    *   请求: `{"functionName": "doubleClick", "args": [100, 200]}`
*   `swipe(x1, y1, x2, y2, duration)`: 手势滑动。
    *   请求: `{"functionName": "swipe", "args": [100, 800, 100, 200, 500]}`
*   `scrollUp()`: 向上滑动整个屏幕。
    *   请求: `{"functionName": "scrollUp"}`

### 4. UI交互：控件查找与操作 (通常通过 `script` 方式执行链式调用)

这些选择器函数 (`text`, `id`, `desc` 等) 返回一个 `UiSelector` 对象，需要链式调用 `findOne()`, `exists()`, `click()` 等方法。

*   **选择器 (Selector Starters):**
    *   `text(str)`
    *   `textContains(str)`
    *   `textStartsWith(prefix)`
    *   `textEndsWith(suffix)`
    *   `textMatches(regex)`
    *   `desc(str)` (对应 contentDescription)
    *   `descContains(str)`
    *   `id(resourceId)`
    *   `className(str)`
    *   `packageName(str)`
    *   `clickable(boolean)`
    *   `bounds(left, top, right, bottom)`
    *   ... (更多选择器条件)

*   **查找方法 (Selector Actions):**
    *   `.findOne()`: 查找满足条件的单个控件，返回 `UiObject`。
    *   `.find()`: 查找所有满足条件的控件，返回 `UiCollection`。
    *   `.exists()`: 判断是否存在满足条件的控件，返回 `boolean`。
    *   `.waitFor()`: 等待出现符合条件的控件。
    *   `.untilFindOne(timeout)`: 在超时时间内查找单个控件。

*   **UiObject 上的操作:**
    *   `.click()`: 点击控件。
    *   `.longClick()`: 长按控件。
    *   `.setText(text)`: (对于可编辑控件) 设置文本。
    *   `.text()`: 获取控件文本。
    *   `.desc()`: 获取控件描述。
    *   `.bounds()`: 获取控件的屏幕范围 (Rect)。
    *   `.getPoint()`: 获取控件中心点坐标 `[x,y]`。
    *   `.children()`: 获取子控件集合。
    *   `.parent()`: 获取父控件。
    *   ... (更多 `UiObject` 方法)

*   **示例 (使用 `script`):**
    *   查找文本为 "设置" 的控件并点击:
        `{"script": "text('设置').findOne().click();"}`
    *   判断是否存在ID为 "com.example:id/button" 的控件:
        `{"script": "id('com.example:id/button').exists();"}` (返回值会在 `result` 字段)
    *   获取文本为 "用户名" 的控件的边界:
        `{"script": "text('用户名').findOne().bounds();"}`

### 5. 文件系统操作

*   `readFile(filePath)`: 读取文件内容。
    *   请求: `{"functionName": "readFile", "args": ["/sdcard/fatjs/data.txt"]}`
*   `writeFile(filePath, content)`: 向文件写入内容 (覆盖)。
    *   请求: `{"functionName": "writeFile", "args": ["/sdcard/fatjs/output.txt", "Hello World"]}`
*   `appendFile(filePath, content)`: 向文件追加内容。
    *   请求: `{"functionName": "appendFile", "args": ["/sdcard/fatjs/log.txt", "New log entry\n"]}`
*   `deleteFile(filePath)`: 删除文件。
    *   请求: `{"functionName": "deleteFile", "args": ["/sdcard/fatjs/old_data.txt"]}`
*   `mkdir(dirPath)`: 创建目录。
    *   请求: `{"functionName": "mkdir", "args": ["/sdcard/fatjs/new_folder/"]}`
*   `mvFile(fromPath, toPath)`: 移动文件。
    *   请求: `{"functionName": "mvFile", "args": ["/sdcard/fatjs/a.txt", "/sdcard/fatjs/b.txt"]}`
*   `lsFolder(folderPath)`: 列出目录下的文件和文件夹，返回一个字符串列表。
    *   请求: `{"functionName": "lsFolder", "args": ["/sdcard/fatjs/"]}`
*   `renameFile(sourcePath, targetPath)`: 重命名文件。
    *   请求: `{"functionName": "renameFile", "args": ["/sdcard/fatjs/oldname.txt", "/sdcard/fatjs/newname.txt"]}`

### 6. 其他实用功能

*   `clip(text)`: 将文本复制到剪贴板。
    *   请求: `{"functionName": "clip", "args": ["要复制的文本"]}`
*   `execCommand(command)`: 执行 shell 命令 (需要设备root权限或应用有相应权限)。
    *   请求: `{"functionName": "execCommand", "args": ["ls /sdcard/"]}`
*   `killTask()`: 尝试停止当前正在执行的 JS 任务 (通过 FATJS 自身机制)。
    *   请求: `{"functionName": "killTask"}`
*   `findMultiColorInRegionFuzzy(mainColor, subColors, distance, x1, y1, x2, y2)`: 多点找色 (参数较复杂，请参考JS具体用法)。
*   `http.get(url)`: (JS内部的) 发送GET请求。注意：这是从JS环境发起的，不是指MCP服务本身。
    *   请求: `{"script": "http.get('https://www.example.com');"}`
*   `http.post(url, jsonString)`: (JS内部的) 发送POST请求。
    *   请求: `{"script": "http.post('https://api.example.com/data', JSON.stringify({key: 'value'}));"}`
*   `engines.execScript(scriptContent)`: 执行另一个JS脚本字符串 (不常用，因为MCP本身就执行脚本)。
    *   请求: `{"script": "engines.execScript('print(\"Dynamic script!\");');"}`

## 注意事项

*   **坐标系统**: 所有坐标通常是屏幕绝对坐标。
*   **文件路径**: 文件路径通常是设备上的绝对路径，如 `/sdcard/....`。
*   **权限**: 许多操作（如截屏、文件读写、辅助功能点击）依赖于应用已获取相应 Android 权限和无障碍服务已开启。
*   **返回值**: `result` 字段的内容和类型取决于被调用的JS函数/脚本的实际返回值。Javet 会尝试进行类型转换。
*   **错误处理**: 仔细检查响应中的 `status` 和 `message` 字段以了解执行结果。
*   **同步/异步**: 当前 MCP 调用是同步的，HTTP请求会等待JS执行完成（或超时）。长时间运行的JS任务可能会导致HTTP请求超时，需要谨慎使用。

此列表并非详尽无遗，但涵盖了最常用和核心的功能。具体参数和行为请参考 `base.js` 中的函数定义以及 FATJS 的相关文档。
