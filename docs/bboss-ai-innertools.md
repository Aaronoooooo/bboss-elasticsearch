# bboss-ai 内置工具使用文档


## 一、概述

### 1.1 文档目的

本文档旨在为开发者提供 `bboss-ai` 内置工具的完整使用指南。所有工具均位于 `org.frameworkset.spi.ai.tools` 包下，共暴露 **14 个** `@Tool` 方法，覆盖 Shell 执行、多语言代码执行、文件系统操作、操作系统信息查询四大场景。

所有工具均通过 `AIAgent.registBeanTool(...)` 注册后由模型自动调用。

### 1.2 工具总览

| 工具类                    | 功能域     | 暴露工具数 | 主要能力                                                     |
| ------------------------- | ---------- | ---------- | ------------------------------------------------------------ |
| `CLIShellFunctionTool`    | Shell 执行 | 1          | 跨平台执行 cmd/sh 脚本，超时控制                             |
| `CodeExecuteFunctionTool` | 代码执行   | 3          | 动态编译运行 Java，调用 Python/Node 执行 Python/JavaScript,可以指定工作目录，python和nodejs环境路径 |
| `FileFunctionTool`        | 文件系统   | 9          | 文件读写、拷贝、删除、属性查询、编码识别、目录遍历，可以指定base工作目录，限定只能操作指定目录下面的文件和目录 |
| `GetOSFunctionTool`       | 系统信息   | 1          | 获取 OS 名称/版本/架构及 CPU 核数/型号、获取服务器系统时间   |
| `HitlTaskcallTool`        | 人工介入   | 1          | HITL（Human-in-the-Loop）人工介入工具，当 AI 无法独立完成任务时调用 |

### 1.3 通用注册方式

所有内置工具均为普通 Java Bean，通过 `registBeanTool` 方法注册到智能体：

```java
AIAgent agent = new AIAgent();

// 注册所有内置工具（推荐方式）
agent.registBeanTool(new GetOSFunctionTool(60));          // 60 秒超时
agent.registBeanTool(new CLIShellFunctionTool(60));
agent.registBeanTool(new CodeExecuteFunctionTool(60));
agent.registBeanTool(new FileFunctionTool("/data/safe"));  // 限制文件操作基目录
agent.registBeanTool(new HitlTaskcallTool());              // 人工介入工具
```

### 1.4 通用配置约定

| 配置项       | 适用工具                                                     | 说明                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **超时**     | `CLIShellFunctionTool`、`CodeExecuteFunctionTool`、`GetOSFunctionTool` | 支持构造器 `new XxxTool(long timeout)` 或 `setTimeout(long)`，单位秒，默认 60 秒；超时后任务被取消并返回提示信息 |
| **线程池**   | Shell 与代码执行工具                                         | 使用独立的守护线程池（`cli-shell-executor` / `code-execute-executor`），避免阻塞 `ForkJoinPool.commonPool` |
| **链式配置** | 所有工具                                                     | `setTimeout` / `setBaseDirectory` / `setCompileOutputDir` 均返回 `this`，支持链式调用 |

---

## 二、CLIShellFunctionTool —— Shell 命令执行工具

### 2.1 功能说明

跨平台（Windows/Linux/Unix/Mac）执行 Shell 脚本。

- **Windows**：调用 `cmd /c`
- **其他平台**：调用 `sh -c`
- **字符编码**：自动合并 stdout 与 stderr，Windows 使用 GBK 解码，其余平台使用 UTF-8

### 2.2 配置选项

| 配置方式                             | 说明                      |
| ------------------------------------ | ------------------------- |
| `CLIShellFunctionTool()`             | 默认构造，超时 60 秒      |
| `CLIShellFunctionTool(long timeout)` | 指定超时（秒）            |
| `setTimeout(long timeout)`           | 链式设置超时，返回 `this` |

### 2.3 工具方法详解

#### 2.3.1 executeBash

**参数说明**

| 参数      | 类型     | 必填   | 说明                      |
| --------- | -------- | ------ | ------------------------- |
| `command` | `String` | **是** | 合法的可执行的 Shell 脚本 |

**返回结果（Map）**

| 字段            | 类型     | 说明                                                         |
| --------------- | -------- | ------------------------------------------------------------ |
| `executeResult` | `String` | 命令输出；命令为空时返回 `"没有输入命令，忽略执行！"`；超时返回超时提示；异常返回失败原因 |

### 2.4 使用示例

```java
// 注册 Shell 工具（30 秒超时）
agent.registBeanTool(new CLIShellFunctionTool(30));

// 模型将自动生成类似命令并通过 executeBash 执行
// 例如：用户提问 "查看当前目录下的文件列表"
// 模型自动调用：executeBash("ls -la")
```

---

## 三、CodeExecuteFunctionTool —— 多语言代码执行工具

### 3.1 功能说明

支持 **Java**、**Python**、**JavaScript** 三种语言的动态执行，统一返回标准化的执行结果结构。

### 3.2 配置选项

| 配置方式                                | 说明                                              |
| --------------------------------------- | ------------------------------------------------- |
| `CodeExecuteFunctionTool()`             | 默认构造，超时 60 秒                              |
| `CodeExecuteFunctionTool(long timeout)` | 指定超时（秒）                                    |
| `setTimeout(long timeout)`              | 链式设置超时，返回 `this`                         |
| `setCompileOutputDir(String dir)`       | 设置 Java 临时编译输出目录，默认 `java.io.tmpdir` |

### 3.3 工具方法详解

#### 3.3.1 executeJava

编译并执行 Java 代码。

**参数说明**

| 参数   | 类型     | 必填   | 说明        |
| ------ | -------- | ------ | ----------- |
| `code` | `String` | **是** | Java 源代码 |

**自动包装规则**

- **含完整类定义**：若代码包含 `public class XXX` 则直接编译运行
- **仅含方法体**：自动包装到 `Main` 类的 `main` 方法中
- **自动导入**：预导入 `java.util.*`、`java.io.*`、`java.math.*`、`java.nio.*`、`java.time.*`、`java.util.stream.*`

**返回结果（Map）**

| 字段       | 类型      | 说明                      |
| ---------- | --------- | ------------------------- |
| `success`  | `Boolean` | 是否成功（exitCode == 0） |
| `exitCode` | `Integer` | 退出码                    |
| `output`   | `String`  | 标准输出（含 STDERR）     |
| `message`  | `String`  | 成功/失败/超时/中断描述   |

**使用示例**

```java
// 模型自动生成并执行 Java 代码
// 场景：用户提问 "用 Java 计算斐波那契数列第 10 项"
// 模型生成的代码：
String code = """
public class Main {
    public static void main(String[] args) {
        int n = 10;
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        System.out.println("F(10) = " + b);
    }
}
""";
// 工具自动编译执行并返回结果
```

#### 3.3.2 executePython

通过系统 Python 解释器执行 Python 代码。

**参数说明**

| 参数   | 类型     | 必填   | 说明          |
| ------ | -------- | ------ | ------------- |
| `code` | `String` | **是** | Python 源代码 |

**执行环境依赖**

- 自动探测系统 PATH 中的 `python3` 或 `python` 命令
- 未安装 Python 时返回错误信息

**返回结果**

同 `executeJava` 返回结构。

**使用示例**

```java
// 模型自动生成并执行 Python 代码
// 场景：用户提问 "用 Python 读取 JSON 文件"
// 模型生成的代码：
String code = """
import json
with open('data.json', 'r') as f:
    data = json.load(f)
    print(json.dumps(data, indent=2))
""";
```

#### 3.3.3 executeJavaScript

执行 JavaScript 代码。

**参数说明**

| 参数   | 类型     | 必填   | 说明              |
| ------ | -------- | ------ | ----------------- |
| `code` | `String` | **是** | JavaScript 源代码 |

**执行引擎选择**

| 优先级    | 引擎                  | 说明                     |
| --------- | --------------------- | ------------------------ |
| 1（首选） | JDK 内置 Nashorn 引擎 | 支持 `eval` 返回值捕获   |
| 2（回退） | 系统 `node` 命令      | Nashorn 不可用时自动回退 |

**返回结果**

同 `executeJava` 返回结构，但 JavaScript 引擎执行时若 `eval` 有返回值，会在 `output` 中附加 `[返回值]` 前缀。

**使用示例**

```java
// 模型自动生成并执行 JavaScript 代码
// 场景：用户提问 "用 JS 计算数组去重"
// 模型生成的代码：
String code = """
const arr = [1, 2, 2, 3, 3, 4];
const unique = [...new Set(arr)];
console.log(unique);
""";
```

### 3.4 执行机制详解

#### 3.4.1 Java 执行机制

1. 使用 `JavaCompiler` 进行内存编译
2. 通过 `URLClassLoader` 加载编译后的字节码
3. 反射调用 `main` 方法
4. 捕获 `System.out/err` 输出
5. 执行后自动清理临时编译目录

#### 3.4.2 Python 执行机制

1. 将代码写入临时 `.py` 文件
2. 调用系统 Python 解释器执行
3. 捕获标准输出和错误输出
4. 执行完成后删除临时文件

#### 3.4.3 JavaScript 执行机制

- **Nashorn 模式**：直接调用 `ScriptEngine.eval()`，捕获 `System.out/err` 与 `eval` 返回值
- **Node 模式**：写入临时 `.js` 文件，调用 `node` 命令执行，执行后删除临时文件

---

## 四、FileFunctionTool —— 文件系统操作工具

### 4.1 功能说明

提供文件与目录的增删改查、内容读写、编码识别、属性获取等 9 个工具方法，支持通过 `baseDirectory` 限制操作范围以防止路径穿越攻击,可以指定多个限制目录。

### 4.2 配置选项

| 配置方式                                    | 说明                            |
| ------------------------------------------- | ------------------------------- |
| `FileFunctionTool()`                        | 默认构造，**不限制**操作目录    |
| `FileFunctionTool(String baseDirectory)`    | 限制只能操作该基目录及其子目录  |
| `addBaseDirectory(String... baseDirectory)` | 设置一到多个基目录，返回 `this` |

> **⚠️ 路径校验警告**：设置 `baseDirectory` 后，所有路径经规范化（统一分隔符、去重复斜杠）后必须以基目录开头，否则抛出 `IllegalArgumentException`。

#### 4.2.1 限制工具操作目录

配置文件操作工具允许操作和访问的目录：可以指定多个

```java
//注册文件操作工具，用于读取文件,需要设置运行文件工具操作的目录清单，禁止文件工具操作清单之外的目录
String[] baseDirs = agentBootrap.getHitlFileToolDasedirs();
FileFunctionTool fileFunctionTool = new FileFunctionTool( );
if(baseDirs != null && baseDirs.length > 0){
    fileFunctionTool.addBaseDirectory(baseDirs);
}
```

### 4.3 工具方法详解

#### 4.3.1 文件操作类

##### copyFile —— 拷贝文件或目录

| 参数        | 类型      | 必填             | 说明               |
| ----------- | --------- | ---------------- | ------------------ |
| `source`    | `String`  | **是**           | 源路径             |
| `target`    | `String`  | **是**           | 目标路径           |
| `overwrite` | `Boolean` | 否（默认 false） | 是否覆盖已存在文件 |

**返回字段**：`success`、`message`、`source`（成功时）、`target`（成功时）

---

##### deleteFile —— 删除文件或目录

| 参数        | 类型      | 必填             | 说明                       |
| ----------- | --------- | ---------------- | -------------------------- |
| `path`      | `String`  | **是**           | 目标路径                   |
| `recursive` | `Boolean` | 否（默认 false） | 是否递归删除目录及其子内容 |

**返回字段**：`success`、`message`

---

##### createFile —— 创建文件或目录

| 参数          | 类型      | 必填             | 说明                              |
| ------------- | --------- | ---------------- | --------------------------------- |
| `path`        | `String`  | **是**           | 目标路径                          |
| `isDirectory` | `Boolean` | 否（默认 false） | true 时创建目录，false 时创建文件 |

**返回字段**：`success`、`message`、`path`

---

##### fileExists —— 检查文件或目录是否存在

| 参数   | 类型     | 必填   | 说明     |
| ------ | -------- | ------ | -------- |
| `path` | `String` | **是** | 目标路径 |

**返回字段**：`success`、`message`、`exists`

---

#### 4.3.2 文件读写类

##### readFile —— 读取文件内容

| 参数      | 类型     | 必填   | 说明                       |
| --------- | -------- | ------ | -------------------------- |
| `path`    | `String` | **是** | 文件路径                   |
| `charset` | `String` | 否     | 指定编码；未指定时自动识别 |

**返回字段**：`success`、`message`、`content`、`charset`

---

##### writeFile —— 写入文件

| 参数      | 类型      | 必填             | 说明                             |
| --------- | --------- | ---------------- | -------------------------------- |
| `path`    | `String`  | **是**           | 文件路径（父目录不存在自动创建） |
| `content` | `String`  | **是**           | 写入内容                         |
| `charset` | `String`  | 否（默认 UTF-8） | 字符编码                         |
| `append`  | `Boolean` | 否（默认 false） | true 时追加，false 时覆盖        |

**返回字段**：`success`、`message`、`path`

---

##### readDirectoryFiles —— 遍历目录读取文件

| 参数        | 类型      | 必填             | 说明                       |
| ----------- | --------- | ---------------- | -------------------------- |
| `path`      | `String`  | **是**           | 目录路径                   |
| `recursive` | `Boolean` | 否（默认 false） | 是否递归遍历子目录         |
| `charset`   | `String`  | 否               | 指定编码；未指定时自动识别 |

**返回字段**：`success`、`message`、`files`（List&lt;Map&gt;，含 path/content/charset）、`count`

---

#### 4.3.3 文件信息类

##### getFileAttributes —— 获取文件属性

| 参数   | 类型     | 必填   | 说明           |
| ------ | -------- | ------ | -------------- |
| `path` | `String` | **是** | 文件或目录路径 |

**返回字段**

| 字段                                  | 说明                               |
| ------------------------------------- | ---------------------------------- |
| `path` / `name`                       | 完整路径 / 文件名                  |
| `isFile` / `isDirectory`              | 是否为文件 / 目录                  |
| `size` / `sizeReadable`               | 字节大小 / 可读格式（如 "1.2 MB"） |
| `lastModified` / `creationTime`       | 最后修改时间 / 创建时间            |
| `canRead` / `canWrite` / `canExecute` | 读/写/执行权限                     |
| `isHidden`                            | 是否隐藏                           |
| `parent`                              | 父目录路径                         |
| `childrenCount`                       | 子项数量（目录有效）               |

---

##### detectFileEncoding —— 检测文件编码

| 参数   | 类型     | 必填   | 说明     |
| ------ | -------- | ------ | -------- |
| `path` | `String` | **是** | 文件路径 |

**检测原理**：通过 BOM（字节顺序标记）与字节特征识别编码（UTF-8、UTF-16、GBK 等）

**返回字段**：`success`、`message`、`encoding`

---

### 4.4 通用返回结构说明

所有 `FileFunctionTool` 方法均包含以下基础字段：

| 字段      | 类型      | 说明                 |
| --------- | --------- | -------------------- |
| `success` | `Boolean` | 操作是否成功         |
| `message` | `String`  | 成功或失败的详细描述 |

各方法附加字段详见 [4.3 工具方法详解](#43-工具方法详解)。

---

## 五、HitlTaskcallTool —— 人工介入工具（HITL）

### 5.1 功能说明

HITL（Human-in-the-Loop）人工介入工具允许在智能体执行过程中，当遇到需要人工决策的关键节点时，暂停智能体执行并等待人工介入处理。

**适用场景**：

| 场景 | 说明 |
|------|------|
| 复杂问题需要人类专业判断 | AI 无法独立完成，需要领域专家介入 |
| 敏感操作需要人工确认 | 如代码修改、数据删除等高危操作 |
| 任务执行结果不符合预期 | 需要人工调整后继续执行 |
| 超出 AI 权限范围的操作 | 需要人工授权或审批 |

### 5.2 配置选项

| 配置方式 | 说明 |
|----------|------|
| `HitlTaskcallTool()` | 默认构造，无需额外配置 |

> **前置要求**：使用该工具前需先初始化 `HitlTaskHelper`，配置数据源和（可选）Redis 集群通道。

### 5.3 工具方法详解

#### 5.3.1 hitlTaskTool

**参数说明**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `hitlTaskReason` | `String` | **是** | 人工介入原因，需包含：1.任务背景与已执行步骤 2.当前卡住的具体原因（技术障碍/权限限制/信息缺失等）3.建议人类关注的关键点或待决策事项 4.期望人类提供的具体帮助；格式清晰，精简聚焦，便于人类快速理解 |

**返回结果（Map）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskId` | `String` | 任务 ID，用于后续处理和查询 |
| `status` | `String` | 任务状态（pending/processed/refused/timeout） |
| `message` | `String` | 任务创建结果描述 |
| `error` | `String` | 错误信息（仅失败时返回） |

### 5.4 完整使用示例

#### 5.4.1 单节点模式配置

```java
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.hitl.HitlTaskHelper;
import org.frameworkset.spi.ai.service.AgentSessionService;
import org.frameworkset.spi.ai.service.impl.AgentSessionServiceImpl;
import org.frameworkset.spi.ai.tools.HitlTaskcallTool;
import org.frameworkset.util.SimpleStringUtil;

public class HitlSingleNodeExample {
    public static void main(String[] args) {
        // 1. 初始化数据源
        SQLUtil.startPool("visualops",
                "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://localhost:3306/bboss",
                "root", "password",
                "select 1");

        // 2. 配置 AgentSessionService
        AgentSessionService agentSessionService = new AgentSessionServiceImpl();
        agentSessionService.setDatasource("visualops");

        // 3. 初始化 HitlTaskHelper（单节点模式）
        HitlTaskHelper.getHitlTaskHelper()
                .setAgentSessionService(agentSessionService)
                .init();

        // 4. 创建智能体并注册 Hitl 工具
        AIAgent agent = new AIAgent();
        agent.setEnableLoopToolCall(true);
        agent.setMaxLoopToolCalls(80);
        agent.registBeanTool(new HitlTaskcallTool());

        // 5. 后续：构建消息并调用...
    }
}
```

#### 5.4.2 集群模式配置（需要 Redis）

```java
// 1. 配置 Redis（集群模式需要）
RedisConfig redisConfig = new RedisConfig();
redisConfig.setName("test")
        .setAuth("password")
        .setServers("101.13.6.7:6381,101.13.6.7:6382")
        .setMode(RedisDB.mode_cluster);
RedisFactory.builRedisDB(redisConfig);

// 2. 初始化 HitlTaskHelper（集群模式）
HitlTaskHelper.getHitlTaskHelper()
        .setAgentSessionService(agentSessionService)
        .setRedisChannel("test", RedisHitlTaskCallListener.DEFAULT_CHANNEL)
        .init();
```

#### 5.4.3 人工处理任务

```java
import org.frameworkset.spi.ai.hitl.HitlTaskHelper;

// 模拟人工处理（同意）
Map<String, Object> hitlTaskData = new LinkedHashMap<>();
hitlTaskData.put("confirm", "确认修改");
hitlTaskData.put("otherData", "用户补充意见");
HitlTaskHelper.handleHitlCallTask(hitlTaskData, null, hitlTaskId);

// 模拟人工拒绝
// HitlTaskHelper.refuseHitlCallTask(hitlTaskData, null, hitlTaskId);
```

### 5.5 异常处理机制

`HitlTaskcallTool` 内部实现了多层异常保护：

| 保护机制 | 说明 |
|----------|------|
| **参数 null/空白校验** | 防止空任务传递给人工 |
| **chatObject null 检查** | 防御非对话上下文调用 |
| **HitlTaskHelper null 检查** | 防御组件未初始化场景 |
| **异常捕获** | 记录错误日志并返回友好提示，不会中断智能体执行 |

### 5.6 技能中声明使用

在 SKILL.md 技能文件中，通过自然语言描述触发条件：

```markdown
## step 5: 问题修复
- 请生成修复后的代码
- 调用文件处理工具保存修改后的代码
- 如果存在人工介入工具 hitlTaskTool，则调用人工介入任务工具 hitlTaskTool，通知人工介入确认后，才能保存修复后的代码到原始文件，否则忽略保存代码到原始文件。
- 修复问题时，要指出问题的位置和原因。
- 修复后，要检查是否解决了问题。
```

**hitlTaskReason 内容模板**：

```
【任务背景】代码审查任务已完成，发现并生成了修复方案
【已执行步骤】
1. 分析了用户提交的 Java 代码
2. 检测到空指针异常风险（第 45 行）
3. 生成了修复代码
【待确认操作】将修复后的代码保存到原始文件 /path/to/OriginalFile.java
【建议关注】修复涉及业务逻辑变更，请确认修复方案正确
【期望决策】是否确认保存修复代码？（是/否）
```

---

## 六、GetOSFunctionTool —— 操作系统信息查询工具

### 6.1 功能说明

获取当前运行环境的操作系统及 CPU 信息，**无入参**。

### 6.2 配置选项

| 配置方式                          | 说明                      |
| --------------------------------- | ------------------------- |
| `GetOSFunctionTool()`             | 默认构造                  |
| `GetOSFunctionTool(long timeout)` | 指定超时（秒）            |
| `setTimeout(long timeout)`        | 链式设置超时，返回 `this` |

### 6.3 工具方法详解

#### 6.3.1 getOS2ndCpu

**参数**：无

**返回字段**

| 字段        | 类型      | 说明                                 | 数据来源                           |
| ----------- | --------- | ------------------------------------ | ---------------------------------- |
| `os`        | `String`  | 操作系统名称（如 Windows 11、Linux） | `System.getProperty("os.name")`    |
| `osVersion` | `String`  | 操作系统版本                         | `System.getProperty("os.version")` |
| `osArch`    | `String`  | 操作系统架构（如 amd64、aarch64）    | `System.getProperty("os.arch")`    |
| `cpuCores`  | `Integer` | CPU 逻辑核数                         | `Runtime.availableProcessors()`    |
| `cpuName`   | `String`  | CPU 型号（详见下方获取机制）         | 平台特定命令                       |

**CPU 型号获取机制**

| 平台       | 命令/来源                            |
| ---------- | ------------------------------------ |
| Windows    | `wmic cpu get Name`                  |
| Linux      | 读取 `/proc/cpuinfo` 的 `model name` |
| Mac / Unix | `uname -p`                           |
| 失败回退   | `os.arch`                            |

---

## 七、综合使用示例

### 7.1 多工具协作场景

以下示例展示结合 OS 信息查询、脚本生成执行、代码执行、文件读写的多步骤任务。

### 7.2 完整代码示例

```java
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.entity.ChatAgentMessage;
import org.frameworkset.spi.ai.entity.ServerEvent;
import org.frameworkset.spi.ai.tools.*;
import reactor.core.publisher.Flux;

public class MultiToolExample {
    public static void main(String[] args) {
        // 1. 创建智能体
        AIAgent agent = new AIAgent();
        
        // 2. 启用循环工具调用（支持多步骤任务）
        agent.setEnableLoopToolCall(true);
        agent.setMaxLoopToolCalls(80);  // 最大循环 80 次
        
        // 3. 注册全部内置工具
        agent.registBeanTool(new GetOSFunctionTool(60));
        agent.registBeanTool(new CLIShellFunctionTool(60));
        agent.registBeanTool(new CodeExecuteFunctionTool(60);
                             //    				.setPythonPath("C:/environment/ml/anaconda3") //设置python环境路径，用于执行python代码
//				.setNodejsPath("C:/Program Files/nodejs/") //设置nodejs环境路径，用于执行javascript代码; );
        agent.registBeanTool(new FileFunctionTool("/data/workspace"));
        
        // 4. 构建用户消息
        ChatAgentMessage message = new ChatAgentMessage();
        message.setModel("deepseek-v4-pro");
        message.setMaas("deepseek");
        message.setRetry(3);
        message.setPrompt(
            "查询当前系统信息，将 CPU 型号写入 /data/workspace/cpu.txt，" +
            "再读取并核对内容。"
        );
        message.setSystemPrompt(
            "你是一个运维专家，可调用工具完成多步骤任务，" +
            "确保每步执行成功后再继续。"
        );
        
        // 5. 执行流式对话
        Flux<ServerEvent> flux = agent.streamChat(message);
        flux.subscribe(event -> System.out.print(event.getData()));
    }
}
```

### 7.3 预期工具调用链

| 步骤 | 工具调用                                        | 目的               |
| ---- | ----------------------------------------------- | ------------------ |
| 1    | `getOS2ndCpu()`                                 | 获取 CPU 型号信息  |
| 2    | `writeFile("/data/workspace/cpu.txt", cpuName)` | 写入 CPU 型号      |
| 3    | `readFile("/data/workspace/cpu.txt")`           | 读取并核对内容     |
| 4    | 最终输出                                        | 向用户报告核对结果 |

---

### 7.4 代码生成与执行案例

本案例演示如何借助工具检索机制，让大模型根据用户指令动态生成 Java / Python / JavaScript 三种语言的代码并执行，最后将生成的代码与执行结果以 Markdown 格式写入文件。

#### 7.4.1 涉及的内置工具

| 工具类 | 说明 |
|--------|------|
| `GetOSFunctionTool` | 获取当前服务器 OS、OS 版本、OS 架构以及 CPU 信息、获取服务器时间（生成结果文档时，需要提供当前系统时间） |
| `CodeExecuteFunctionTool` | 多语言代码执行工具，内置 `executeJava` / `executePython` / `executeJavaScript` 三个工具方法 |
| `FileFunctionTool` | 文件操作工具，提供"将内容写入到指定文件"等能力 |

`CodeExecuteFunctionTool` 的三个工具方法描述如下（摘自其 `@Tool` 注解）：

- **executeJava**：编译并执行 Java 代码。若代码中已包含 `public class`，则直接编译运行；否则自动包装到 `Main` 类的 `main` 方法中执行。
- **executePython**：通过系统 `python` / `python3` 解释器执行 Python 代码，返回标准输出与标准错误。
- **executeJavaScript**：执行 JavaScript 代码。优先使用 JDK 内置 Nashorn 引擎，引擎不可用时回退到系统 `node` 命令。

该工具支持通过构造方法设置执行超时时间（秒）和工作目录：

```java
new CodeExecuteFunctionTool(60)                  // 设置超时时间为 60 秒
        .setWorkspaceDir("C:\\data\\ai\\aigenfiles\\tools\\temp") // 设置临时工作目录
//    				.setPythonPath("C:/environment/ml/anaconda3") //设置python环境路径，用于执行python代码
//				.setNodejsPath("C:/Program Files/nodejs/") //设置nodejs环境路径，用于执行javascript代码; 
```

#### 7.4.2 任务指令示例

任务指令通过资源文件 `#[codeexecute-prompt.txt,type=resource]` 加载，可包含如下需求：

- 获取 OS 版本信息与 CPU 信息
- 生成并执行 Java、Python、JavaScript 三种语言的 Hello World 代码
- 将生成的代码及执行结果以 Markdown 格式写入指定文件

提示词工程文件内容：[codeexecute-prompt.txt](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/resources/codeexecute-prompt.txt)

```markdown
请依次完成以下任务：

# 生成并执行 Java 代码
1.生成一段java代码，输出hello world
2.执行生成的java代码

# 生成并执行 python 代码
1.生成一段python代码，输出hello world
2.执行生成的python代码

# 生成并执行 javascript 代码
1.生成一段javascript代码，输出hello world
2.执行生成的javascript代码

# 输出代码和执行结果
将前面生成的代码和执行结果以Markdown格式写入文件：C:\data\ai\aigenfiles\tools\code-gen-execute.md
```

#### 7.4.3 完整使用示例

参考 [CodeExecuteDBTest.java](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/tools/CodeExecuteDBTest.java)：

```java
public static void executeCodeTest( ) throws InterruptedException {
    // 设置模型调用参数
    ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
    chatAgentMessage.setMaas("deepseek").setModel("deepseek-v4-pro");
    chatAgentMessage.setRetry(3);
    String message = "#[codeexecute-prompt.txt,type=resource]";
    chatAgentMessage.setPrompt(message).setSystemPrompt("你是一个专业的多语言代码生成和执行工具，可以根据用户要求生成符合要求的、完整的、可执行的代码，并且执行生成的代码，能够将生成的代码和执行结果以Markdown格式写入文件");

    chatAgentMessage.setStream(true).setThinking(false).setTemperature(0.7);
    chatAgentMessage.setStoreContext(new StoreContext()
            .setUserId("user123").setSessionSize(100).setRequestId("request123")
            .setStoreType(StoreContext.STORE_TYPE_DB)
            .setDataSource("visualops"));

    CountDownLatch countDownLatch = new CountDownLatch(1);
    AIAgent agent = new AIAgent();
    agent.setEnableLoopToolCall(true);//启用智能体多次调用工具机制
    agent.setMaxLoopToolCalls(80);
    // 注册获取当前操作系统OS信息工具：框架内置工具
    agent.registBeanTool(new GetOSFunctionTool(60));
    // 注册多语言代码执行工具：框架内置工具，可执行 Java/Python/JavaScript 代码
    agent.registBeanTool(new CodeExecuteFunctionTool(60)
            .setWorkspaceDir("C:\\data\\ai\\aigenfiles\\tools\\temp"));
                             //    				.setPythonPath("C:/environment/ml/anaconda3") //设置python环境路径，用于执行python代码
//				.setNodejsPath("C:/Program Files/nodejs/") //设置nodejs环境路径，用于执行javascript代码; );
    agent.registBeanTool(new FileFunctionTool("C:\\data\\ai\\aigenfiles\\tools\\"))
            // 通过关键词检索机制，只向大模型暴露与本次任务相关的工具方法
            .setKeywordToolSearcher("获取OS、OS版本、OS架构以及CPU信息", "获取服务器时间",
                    "将内容写入到指定文件",
                    "编译并执行 Java 代码", "执行 JavaScript 代码", "执行 Python 代码");

    // 通过bboss响应式异步交互接口，请求Deepseek模型服务，提交问题
    Flux<ServerEvent> flux = agent.streamChat(chatAgentMessage);

    flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
            .doOnNext(chunk -> {
                if (chunk.getData() != null) {
                    System.out.print(chunk.getData());
                }
            }) // 打印流式调用返回的问题答案片段
            .doOnComplete(() -> {
                countDownLatch.countDown();
                System.out.println();
                logger.info("\n=== 流完成 ===");
            })
            .doOnError(error -> {
                countDownLatch.countDown();
                logger.error("错误: " + error.getMessage(), error);
            })
            .subscribe();

    // 等待异步操作完成
    countDownLatch.await();
}
```

#### 7.4.4 关键说明

1. **工具检索关键词与 `@Tool` description 对应**：`setKeywordToolSearcher` 中传入的关键词需与工具方法的 `description` 文本相匹配。例如 `"编译并执行 Java 代码"` 与 `executeJava` 的描述完全一致，可确保该工具方法被准确命中。
2. **多工具协同**：本案例同时注册了三个工具组件（`GetOSFunctionTool`、`CodeExecuteFunctionTool`、`FileFunctionTool`），通过关键词检索机制从所有注册的工具方法中筛选出本次任务所需的方法子集，避免向大模型传递无关工具，降低上下文负担。
3. **多语言执行能力**：`CodeExecuteFunctionTool` 通过独立线程池执行代码，支持超时控制与异常捕获，自动清理临时编译产物，适用于智能体动态生成并执行代码的场景。
4. **循环工具调用**：`setEnableLoopToolCall(true)` 与 `setMaxLoopToolCalls(80)` 启用智能体多次调用工具机制，支持生成代码 → 执行代码 → 收集结果 → 写入文件的多步骤工作流。

#### 7.4.5 执行结果示例

完整的执行结果文档（生成于 `C:\data\ai\aigenfiles\tools\code-gen-execute.md`）如下：

~~~markdown
# 代码生成与执行结果

> 生成时间：2026-07-05 16:38:47

---

## 一、Java 代码

### 1. 生成的代码

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```

### 2. 执行结果

```
Hello World
```

---

## 二、Python 代码

### 1. 生成的代码

```python
print("Hello World")
```

### 2. 执行结果

```
Hello World
```

---

## 三、JavaScript 代码

### 1. 生成的代码

```javascript
console.log("Hello World");
```

### 2. 执行结果

```
Hello World
```

---

## 总结

三种语言的代码均成功生成，功能均为输出 "Hello World"。

| 语言       | 代码行数 | 输出结果      |
|------------|----------|---------------|
| Java       | 5行      | Hello World   |
| Python     | 1行      | Hello World   |
| JavaScript | 1行      | Hello World   |
~~~


## 八、注意事项与安全建议

### 8.1 安全风险

#### 8.1.1 Shell/代码执行风险

| 工具方法            | 风险等级   | 说明                                 |
| ------------------- | ---------- | ------------------------------------ |
| `executeBash`       | 🔴 **高危** | 会真实执行任意 Shell 命令            |
| `executeJava`       | 🔴 **高危** | 会动态编译并执行任意 Java 代码       |
| `executePython`     | 🔴 **高危** | 会调用系统解释器执行任意 Python 代码 |
| `executeJavaScript` | 🔴 **高危** | 会执行任意 JavaScript 代码           |

> **🚨 安全建议**：务必在受控沙箱环境部署，并对智能体可触达的权限进行最小化裁剪。生产环境建议：
> - 运行在隔离容器中
> - 使用专用低权限系统账户
> - 限制网络访问
> - 开启严格的资源配额

#### 8.1.2 路径穿越防护

| 防护措施               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| **设置 baseDirectory** | 强烈建议为 `FileFunctionTool` 设置基目录，限制模型只能操作指定目录树，可以指定多个不同的目录 |
| **路径规范化校验**     | 自动统一分隔符、去除重复斜杠，防止 `../` 路径穿越攻击        |
| **违规行为拦截**       | 越权访问时抛出 `IllegalArgumentException`，不会执行操作      |

```java
// ✅ 正确做法：限制操作范围
FileFunctionTool safeTool = new FileFunctionTool("/data/safe");
safeTool.addBaseDirectory("d1","d2");

// ❌ 错误做法：不限制操作范围（危险！）
FileFunctionTool unsafeTool = new FileFunctionTool();
```

### 8.2 环境依赖

#### 8.2.1 运行时环境要求

| 工具                | 依赖要求                                         | 验证方式                                                     |
| ------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| `executeJava`       | **JDK**（非 JRE），需提供 `JavaCompiler`         | 调用 `ToolProvider.getSystemJavaCompiler()`，为 `null` 则不可用 |
| `executePython`     | 系统 PATH 中包含 `python3` 或 `python`           | 自动探测，未找到则返回错误                                   |
| `executeJavaScript` | JDK 内置 Nashorn 引擎 或 系统 PATH 中包含 `node` | Nashorn 不可用时自动回退到 Node                              |
| `executeBash`       | Windows 需 `cmd.exe`；其他平台需 `/bin/sh`       | 平台标准组件                                                 |

#### 8.2.2 环境检查示例

```java
// 启动时检查环境
public void checkEnvironment() {
    // 检查 Java 编译环境
    if (ToolProvider.getSystemJavaCompiler() == null) {
        logger.warn("JavaCompiler 不可用，请使用 JDK 而非 JRE 运行");
    }
    
    // 检查 Python
    try {
        Process p = Runtime.getRuntime().exec(new String[]{"python3", "--version"});
        if (p.waitFor() == 0) {
            logger.info("Python 环境可用");
        }
    } catch (Exception e) {
        logger.warn("Python 未安装或不可用");
    }
}
```

### 8.3 超时与循环控制

#### 8.3.1 超时保护

| 配置建议     | 说明                                                      |
| ------------ | --------------------------------------------------------- |
| 设置合理超时 | 长耗时命令/代码务必通过构造器或 `setTimeout` 设置合理超时 |
| 超时行为     | 超时后底层 `Process` 会被 `destroyForcibly()` 强制终止    |
| 默认超时     | 60 秒，可根据业务场景调整                                 |

```java
// 为不同工具设置不同超时
CLIShellFunctionTool shellTool = new CLIShellFunctionTool(120);  // 长任务 2 分钟
CodeExecuteFunctionTool codeTool = new CodeExecuteFunctionTool(30); // 短任务 30 秒
GetOSFunctionTool osTool = new GetOSFunctionTool(10);            // 快速任务 10 秒
```

#### 8.3.2 循环调用限制

| 配置项                        | 说明                               | 推荐值                   |
| ----------------------------- | ---------------------------------- | ------------------------ |
| `setEnableLoopToolCall(true)` | 启用循环工具调用，支持多步骤任务   | 复杂任务开启             |
| `setMaxLoopToolCalls(int)`    | 最大循环次数，防止模型陷入无限循环 | 20-100，视任务复杂度而定 |

```java
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(50);  // 50 次后强制停止
```

### 8.4 临时资源管理

| 资源类型            | 清理策略                   | 注意事项                                                  |
| ------------------- | -------------------------- | --------------------------------------------------------- |
| Java 编译产物       | 执行后自动清理临时编译目录 | 若自定义 `compileOutputDir`，确保目录可写且有独立清理策略 |
| Python 临时脚本     | 执行后自动删除 `.py` 文件  | —                                                         |
| JavaScript 临时脚本 | 执行后自动删除 `.js` 文件  | —                                                         |
| 执行进程            | 超时或完成后自动释放       | 确保 `destroyForcibly()` 被正确调用                       |

```java
// 自定义编译输出目录（需确保有清理策略）
CodeExecuteFunctionTool codeTool = new CodeExecuteFunctionTool(60);
codeTool.setCompileOutputDir("/tmp/ai-compile");
// 建议配合定时清理任务：
// 0 0 * * * rm -rf /tmp/ai-compile/*
```

---

## 九、附录

### 9.1 工具方法快速索引

| 序号 | 工具类                    | 方法名               | 功能简述             |
| ---- | ------------------------- | -------------------- | -------------------- |
| 1    | `CLIShellFunctionTool`    | `executeBash`        | 执行 Shell 命令      |
| 2    | `CodeExecuteFunctionTool` | `executeJava`        | 编译并执行 Java 代码 |
| 3    | `CodeExecuteFunctionTool` | `executePython`      | 执行 Python 代码     |
| 4    | `CodeExecuteFunctionTool` | `executeJavaScript`  | 执行 JavaScript 代码 |
| 5    | `FileFunctionTool`        | `copyFile`           | 拷贝文件或目录       |
| 6    | `FileFunctionTool`        | `deleteFile`         | 删除文件或目录       |
| 7    | `FileFunctionTool`        | `createFile`         | 创建文件或目录       |
| 8    | `FileFunctionTool`        | `fileExists`         | 检查路径是否存在     |
| 9    | `FileFunctionTool`        | `readFile`           | 读取文件内容         |
| 10   | `FileFunctionTool`        | `writeFile`          | 写入文件内容         |
| 11   | `FileFunctionTool`        | `readDirectoryFiles` | 遍历目录读取文件     |
| 12   | `FileFunctionTool`        | `getFileAttributes`  | 获取文件属性         |
| 13   | `FileFunctionTool`        | `detectFileEncoding` | 检测文件编码         |
| 14   | `GetOSFunctionTool`       | `getOS2ndCpu`        | 获取 OS 和 CPU 信息  |
| 15   | `HitlTaskcallTool`        | `hitlTaskTool`       | 发起人工介入请求     |

### 9.2 常见问题（FAQ）

#### Q1: 执行 `executeJava` 时报错 "JavaCompiler 不可用"？

**A**：请确保运行环境为 JDK 而非 JRE。JDK 提供了 `tools.jar` 中的 `JavaCompiler` 实现。

#### Q2: Python 代码执行时提示 "python3: command not found"？

**A**：系统未安装 Python 或未配置 PATH。请安装 Python 或将 Python 路径添加到系统 PATH 中。

#### Q3: 文件操作报错 "路径越权，不在基目录下"？

**A**：`FileFunctionTool` 设置了 `baseDirectory`，而传入的路径不在该目录下。请检查：
1. 确认 `baseDirectory` 设置正确
2. 传入的路径是否完整且以基目录开头

#### Q4: 工具执行超时后会发生什么？

**A**：超时后底层 `Process` 会被 `destroyForcibly()` 强制终止，并返回超时提示信息。超时后的进程无法恢复，需要重新执行。

#### Q5: 如何防止模型调用危险 Shell 命令？

**A**：建议采取以下措施：
1. 使用低权限系统账户运行应用程序
2. 在容器或虚拟机中运行
3. 对 `CLIShellFunctionTool` 设置较短的超时
4. 监控和审计所有工具调用日志

#### Q6: `executeJavaScript` 使用 Nashorn 还是 Node？

**A**：优先使用 JDK 内置的 Nashorn 引擎，若不可用则自动回退到系统 `node` 命令。可通过日志查看实际使用的引擎。

#### Q7: 多步骤任务中如何确保步骤顺序？

**A**：启用循环工具调用（`setEnableLoopToolCall(true)`），并设置合适的系统提示词（`SystemPrompt`），引导模型按正确顺序执行。

### 9.3 版本更新日志

| 版本  | 日期       | 更新内容                                 |
| ----- | ---------- | ---------------------------------------- |
| 1.0.1 | 2026-07-19 | 新增 HitlTaskcallTool 人工介入工具，共 5 个工具类、15 个工具方法 |
| 1.0.0 | 2026-07-04 | 初始版本，包含 4 个工具类、14 个工具方法 |

---

**文档结束**