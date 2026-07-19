# Skills使用文档


## 一、概述

### 1.1 文档目的

本文档为开发者提供 `bboss-ai` 技能（Skill）机制的完整使用指南。技能是 `bboss-ai` 智能体（AIAgent）扩展能力的一种轻量级机制，通过 Markdown 文件（`SKILL.md`）描述专家知识与工作流程，并由框架注册为一个可被大模型调用的 `Skill` 工具。

技能相关代码位于 `org.frameworkset.spi.ai.skill` 包下，核心组件如下：

| 组件 | 职责 |
| ---- | ---- |
| `SkillsToolRegist` | 技能工具注册器，将所有技能聚合并暴露为 `Skill` 函数工具 |

### 1.2 适用场景

技能适用于以下场景：

- 将一段**固定的专家流程**（如代码审查、运维巡检、配置生成）封装为可复用的"技能书"，由模型在合适时机主动加载。
- 通过 Markdown + Front Matter 定义**可读、可版本化、可扩展**的提示词工程资产。
- 结合 `AIAgent` 的多工具循环调用机制（`setEnableLoopToolCall(true)`），让模型按技能描述的工作流逐步调用其他工具（如 `FileFunctionTool`、`CLIShellFunctionTool`、业务自定义工具）完成任务。

### 1.3 与内置工具的关系

| 维度 | 内置工具（`tools` 包） | 技能（`skill` 包） |
| ---- | ---------------------- | ------------------ |
| 形态 | Java Bean + `@Tool` 注解 | Markdown 文件 + Front Matter |
| 调用方式 | `registBeanTool(...)` | `registTools(new SkillsToolRegist()...)` |
| 暴露给模型的函数 | 每个方法对应一个 function | 统一为单个 `Skill` 函数，参数为 `skillName` |
| 适用场景 | 原子能力（Shell、文件、代码执行等） | 流程性专家知识、可加载的提示词资产 |

两者可以、也推荐**组合使用**：技能负责告诉模型"该怎么做"，内置工具负责"具体执行"。

---

## 二、SKILL.md 文件规范

### 2.1 文件结构

一个技能对应一个目录，目录下必须包含 `SKILL.md` 文件，目录名即技能的逻辑名。典型结构如下：

```
my-skill/
├── SKILL.md              # 必需，技能定义文件
└── references/           # 可选，参考资料目录
    └── checklist.md      # 可选，技能正文中可引用的补充资料
```

### 2.2 Front Matter 规范

`SKILL.md` 顶部以 `---` 包围的 YAML 风格头部为 Front Matter，用于声明技能元数据。框架内置识别以下两个关键字段：

| 字段          | 类型   | 必填 | 说明 |
| ------------- | ------ | ---- | ---- |
| `name`        | String | 是   | 技能唯一标识，模型通过该名称调用技能；须与目录名保持一致 |
| `description` | String | 是   | 技能用途描述，框架会将其拼接到 `Skill` 工具的描述中，模型据此判断是否调用该技能 |

其他自定义字段（如 `version`、`author`、`tags`、`category` 等）会被解析到 `Skill.getFrontMatter()` 中，并随 `Skill.toXml()` 一并暴露给模型，可用于业务自定义扩展。

> **说明**：`MarkdownParser` 以 `:` 作为字段分隔符，值两端的双引号或单引号会被自动剥离。

### 2.3 正文规范

Front Matter 之后的内容为技能正文，由 `Skill.getContent()` 返回。正文没有强制格式约束，但推荐遵循以下结构（参考 `code-review-skill`）：

```markdown
# 技能标题

## 工作流程
### Step 1：...
### Step 2：...
### Step 3：调用其他工具（如 reviewJavaCode）
### Step 4：输出格式

## 边界
- 不确定的问题要说明"不确定"

## 参考资料
读取文件 `references/xxx.md` 补充更细的检查项。
```

### 2.4 完整示例：roll-dice

<a id="chapter-2-4"></a>

一个最小化的技能定义：

```markdown
---
name: roll-dice
description: Roll dice using a random number generator. Use when asked to roll a die (d6, d20, etc.), roll dice, or generate a random dice roll.
---

To roll a die, use the following command that generates a random number from 1
to the given number of sides:

​bash
echo $((RANDOM % <sides> + 1))
​

​powershell
Get-Random -Minimum 1 -Maximum (<sides> + 1)
​

Replace `<sides>` with the number of sides on the die (e.g., 6 for a standard
die, 20 for a d20).

```



### 2.5 完整示例：code-review-skill



带参考资料与工作流的技能定义：

```markdown
---
name: code-review-skill
description: 按固定流程审查 Java 代码，优先发现 bug、安全风险、边界条件、可维护性问题和缺失测试。当用户要求 review、审查、检查 Java 代码或判断代码有没有风险时使用。
---

# Java Code Review Skill
这个 Skill 用来审查 Java 代码。

## 工作流程
### Step 1：先判断代码场景
### Step 2：按风险优先级审查
### Step 3：调用代码审查工具
当用户提供 Java 代码时，当存在 `reviewJavaCode` 工具时，才调用 `reviewJavaCode` 工具。
### Step 4：输出格式

## 边界
- 不确定的问题要说明"不确定"，不要编造上下文。

## 参考资料
读取文件 `references/checklist.md` 补充更细的检查项。
```

### 2.6 扩展示例：带完整 Front Matter 的工作流技能

参考 `bboss-ai-flow` 中的 `business-config-workflow`，可声明丰富的扩展字段：

```yaml
---
name: business-config-workflow
displayName: 业务配置智能体工作流
description: 基于 ECOP 产品配置场景，实现业务配置的智能选择、生成、评估、修复和审核全流程。
category: workflow
version: 1.0.0
author: bboss
license: Apache-2.0
tags:
  - workflow
  - business-config
dependencies:
  - bboss-ai-flow
  - bboss-ai-model
---
```

上述 `name`、`description` 之外的字段会被放入 `frontMatter` Map，并通过 `Skill.toXml()` 输出为 XML 节点暴露给模型，便于业务侧按需识别。

---

## 三、核心 API 说明

### 3.1 SkillsToolRegist 注册器

`SkillsToolRegist` 实现 `ToolsRegist`，负责把多个技能聚合为一个 `Skill` 函数工具。核心方法：

| 方法 | 说明 |
| ---- | ---- |
| `addClasspathSkills(String resourceSkillsDir)` | 从 classpath 加载技能（如 `"skills"`） |
| `addDirectorySkills(String dir)` | 从文件系统目录加载技能 |
| `setToolDescriptionTemplate(String tpl)` | 自定义工具描述模板（默认使用内置模板） |

> **关键点**：技能返回的内容包含 `basePath`，模型可基于该路径使用 `FileFunctionTool` 等工具读取 `references/*.md` 等补充资料。

---

## 四、SkillsToolRegist 使用指南

### 4.1 注册流程

`SkillsToolRegist` 通过 `AIAgent.registTools(...)` 注册，支持链式添加多个技能来源：

```java
AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);   // 启用多轮工具调用，技能场景强烈建议开启
agent.setMaxLoopToolCalls(80);

SkillsToolRegist skillsRegist = new SkillsToolRegist()
        .addClasspathSkills("skills");  // 从 classpath 的 skills/ 目录加载
agent.registTools(skillsRegist);
```

### 4.2 两种技能加载方式

#### 4.2.1 从 Classpath 加载（推荐用于打包发布）

```java
// skills 目录位于 classpath 根下，例如 src/main/resources/skills 或 src/test/resources/skills
new SkillsToolRegist().addClasspathSkills("skills");
```

`addClasspathSkills` 底层调用 `SkillUtils.loadResource(new ClassPathResource(resourceSkillsDir))`，支持以下三种 classpath 形态：

1. **文件系统 classpath**（开发态、解压后的部署态）
2. **JAR 内带显式目录条目**的 classpath
3. **JAR 内无目录条目**的 classpath（框架自动回退到手动 JAR 扫描，扫描所有 `META-INF/MANIFEST.MF` 对应的 JAR）

#### 4.2.2 从文件系统目录加载（推荐用于本地开发调试）

```java
// 直接指向磁盘上的技能根目录，可动态修改无需重新打包
new SkillsToolRegist().addDirectorySkills("C:\\workspace\\bbossgroups\\bboss-ai\\.trae\\skills");
```

`addDirectorySkills` 底层调用 `SkillUtils.loadDirectory(dir)`，会递归扫描该目录下所有 `SKILL.md` 文件。

#### 4.2.3 混合加载

`SkillsToolRegist` 支持链式调用，可同时从多个来源加载技能：

```java
new SkillsToolRegist()
    .addClasspathSkills("skills")                                  // 内置技能
    .addDirectorySkills("D:\\custom-skills");                       // 用户自定义技能
```

### 4.3 自定义工具描述模板

默认工具描述模板位于 classpath 的 `skill/TOOL_DESCRIPTION_TEMPLATE.txt`，内容如下：

```
Execute a skill within the main conversation

<skills_instructions>
When users ask you to perform tasks, check if any of the available skills below can help complete the task more effectively. Skills provide specialized capabilities and domain knowledge.

How to use skills:
- Invoke skills using this tool with the skill name only (no arguments)
- When you invoke a skill, you will see <command-message>The "{name}" skill is loading</command-message>
- The skill's prompt will expand and provide detailed instructions on how to complete the task

NOTE: Response always starts start with the base directory of the skill execution environment. You can use this to retrieve additional files of call shell commands.
Skill description follows after the base directory line.

Important:
- Only use skills listed in <available_skills> below
- Do not invoke a skill that is already running
</skills_instructions>

<available_skills>
#{skills}
</available_skills>
```

其中 `#{skills}` 占位符会在 `init()` 时被所有技能的 XML 描述拼接替换。若需自定义模板，可通过 `setToolDescriptionTemplate(String)` 替换。

### 4.4 暴露给模型的工具定义

`SkillsToolRegist.init()` 会构造一个函数工具：

| 项 | 值 |
| -- | -- |
| 函数名 | `Skill` |
| 函数描述 | 模板替换 `#{skills}` 后的完整内容，包含所有可用技能列表 |
| 必填参数 | `skillName` |
| 参数类型 | `string` |
| 参数描述 | `The skill name (no arguments). E.g., "pdf" or "xlsx"` |
| 执行器 | `SkillFunctionCall`（持有 `skillName -> Skill` 映射） |

---

## 五、完整使用案例

### 5.1 案例一：roll-dice —— 简单技能 + Shell 执行

本案例展示一个最小化的技能工作流：模型加载 `roll-dice` 技能后，按技能说明通过 Shell 工具执行骰子命令。

#### 5.1.1 技能文件

`src/test/resources/skills/roll-dice/SKILL.md` 内容见 [2.4 完整示例](#chapter-2-4)。

#### 5.1.2 注册与调用

```java
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.ChatAgentMessage;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.skill.SkillsToolRegist;
import org.frameworkset.spi.ai.tools.CLIShellFunctionTool;
import org.frameworkset.spi.ai.tools.GetOSFunctionTool;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

public class SkillAgentTest {
    public static void main(String[] args) throws InterruptedException {
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
        chatAgentMessage.setMaas("deepseek").setModel("deepseek-v4-pro").setRetry(3);
        // 用户指令：执行 roll-dice 技能，要求结果在 90-100 之间
        String message = "执行技能roll-dice:掷出一个骰子，结果为90-100之间的整数";
        chatAgentMessage.setPrompt(message)
                .setSystemPrompt("你是一个技能专家，可以按照用户要求执行技能。如果需要执行脚本，用shell工具执行");
        chatAgentMessage.setStream(true).setThinking(false).setTemperature(0.7);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AIAgent agent = new AIAgent();
        agent.setEnableLoopToolCall(true);  // 启用多轮工具调用
        agent.setMaxLoopToolCalls(80);
        // 注册技能工具：从文件系统目录加载
        agent.registTools(new SkillsToolRegist()
                .addDirectorySkills("C:\\workspace\\bbossgroups\\bboss-ai\\.trae\\skills"));
        // 注册内置工具：获取 OS 信息
        agent.registBeanTool(new GetOSFunctionTool(60));
        // 注册内置工具：Shell 脚本执行
        agent.registBeanTool(new CLIShellFunctionTool(60));

        Flux<ServerEvent> flux = agent.streamChat(chatAgentMessage);
        flux.doOnNext(chunk -> {
                    if (chunk.getData() != null) System.out.print(chunk.getData());
                })
                .doOnComplete(() -> { countDownLatch.countDown(); System.out.println(); })
                .doOnError(error -> { countDownLatch.countDown(); error.printStackTrace(); })
                .subscribe();
        countDownLatch.await();
    }
}
```

#### 5.1.3 预期调用链

| 步骤 | 工具调用 | 目的 |
| ---- | ------- | ---- |
| 1 | `Skill(skillName="roll-dice")` | 加载骰子技能，获取 bash/powershell 命令模板 |
| 2 | `getOS2ndCpu()` | 获取当前 OS 类型（Windows/Linux） |
| 3 | `executeBash("Get-Random -Minimum 90 -Maximum 101")` 或对应的 `echo $((RANDOM % 11 + 90))` | 按技能说明生成随机数 |
| 4 | 最终输出 | 返回骰子结果 |

---

### 5.2 案例二：code-review-skill —— 技能 + 业务自定义工具协作

本案例展示技能与业务自定义工具的深度协作：模型加载 `code-review-skill` 后，按技能定义的流程调用业务工具 `reviewJavaCode` 完成代码审查。

#### 5.2.1 技能文件

`src/test/resources/skills/code-review-skill/SKILL.md` 内容见 [2.5 完整示例](#25-完整示例code-review-skill)。

参考资料 `references/checklist.md`：

```markdown
# Java 代码审查检查清单
## Bug 和边界条件
- 参数是否可能为null
- 集合是否可能为空
- 字符串是否可能为空白
- 下标、分页、金额、数量是否有边界

## 安全风险
- 是否硬编码密码、Token、API Key
- 是否缺少权限校验
- 是否暴露敏感字段

## 异常和日志
- 是否吞掉异常
- 日志是否包含必要上下文
- 日志是否泄露敏感信息
```

#### 5.2.2 业务自定义工具

```java
import org.frameworkset.spi.ai.model.annotation.Tool;

public class CodeReviewTools {
    @Tool(name = "reviewJavaCode",
          description = "按照 code-review-skill 的审查流程审查 Java 代码。参数 code 是待审查代码，返回 Markdown 格式审查报告。")
    public String reviewJavaCode(String code) {
        // 实现见 src/test/java/org/frameworkset/spi/ai/skills/CodeReviewTools.java
        // 包含：空指针风险、吞异常、System.out、硬编码敏感信息、缺少入参校验等检查
        return buildReport(code);
    }
}
```

#### 5.2.3 注册与调用

```java
AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(80);

// 1. 注册技能工具：从 classpath 加载 code-review-skill
agent.registTools(new SkillsToolRegist().addClasspathSkills("skills"));

// 2. 注册文件操作工具：用于读取待审查的 Java 文件
agent.registBeanTool(new FileFunctionTool("C:\\data\\ai\\code"));

// 3. 注册业务自定义代码审查工具
agent.registBeanTool(new CodeReviewTools());

// 4. 构建对话
ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
chatAgentMessage.setMaas("deepseek").setModel("deepseek-v4-pro").setRetry(3);
chatAgentMessage.setPrompt("请审查Java文件中的代码,java文件路径：C:\\data\\ai\\code\\AIAgent.java")
        .setSystemPrompt(
            "你是一个 Java 代码审查助手。 " +
            "长期规则： " +
            "- 如果用户提交 Java 代码并要求审查，先调用 Skill 工具加载 code-review-skill。 " +
            "- 加载技能书后，再按照技能书里的审查顺序调用 reviewJavaCode 工具。 " +
            "- 优先指出 bug、安全风险、边界条件、异常处理和缺失测试。 " +
            "输出要求：用中文回答，使用 Markdown，先给总体结论，再列主要问题，最后给测试建议和下一步。"
        );
chatAgentMessage.setStream(true).setThinking(true).setTemperature(0.7);

Flux<ServerEvent> flux = agent.streamChat(chatAgentMessage);
```

#### 5.2.4 预期调用链

| 步骤 | 工具调用 | 目的 |
| ---- | ------- | ---- |
| 1 | `Skill(skillName="code-review-skill")` | 加载代码审查技能，获取审查流程与 `basePath` |
| 2 | `readFile("references/checklist.md")`（基于 `basePath`） | 读取参考资料获取更细的检查项 |
| 3 | `readFile("C:\\data\\ai\\code\\AIAgent.java")` | 读取待审查的 Java 源码 |
| 4 | `reviewJavaCode(code)` | 调用业务工具执行基础审查，返回 Markdown 报告 |
| 5 | 最终输出 | 在审查报告基础上补充解释，输出总体结论、主要问题、测试建议 |

---

### 5.3 案例三：ChecklistCodeViewAgentTest —— 纯技能驱动的工作流

本案例与案例二的差异：不注册业务 `CodeReviewTools`，仅依赖技能文件本身指导模型完成审查。适用于无自定义工具、完全靠 Prompt 驱动的场景。

```java
AIAgent agent = new AIAgent();
agent.setEnableLoopToolCall(true);
agent.setMaxLoopToolCalls(80);

// 仅注册技能工具
agent.registTools(new SkillsToolRegist().addClasspathSkills("skills"));

// 注册文件操作工具：既用于读取待审查代码，也用于读取技能的 references
agent.registBeanTool(new FileFunctionTool("C:\\data\\ai\\code")
        .addBaseDirectory("C:\\workspace\\bbossgroups\\bboss-ai\\bboss-ai\\out\\test\\resources\\skills\\code-review-skill\\"));
```

> **注意**：当技能位于 classpath 但又需要通过 `FileFunctionTool` 读取 `references` 时，需通过 `addBaseDirectory` 显式将技能目录加入允许访问的范围。`FileFunctionTool` 支持通过多次调用 `addBaseDirectory` 添加多个允许操作的根目录。

---

## 六、加载机制详解

### 6.1 文件系统加载流程

`SkillUtils.loadDirectory(rootDirectory)`：

1. 校验 `rootDirectory` 存在且为目录，否则抛 `RuntimeException`
2. `Files.walk(rootPath)` 递归遍历所有常规文件
3. 过滤文件名为 `SKILL.md` 的文件
4. 对每个匹配文件：
    - 以 UTF-8 读取全文
    - 构造 `MarkdownParser` 解析 Front Matter 与正文
    - 设置 `basePath` 为 `SKILL.md` 所在目录的绝对路径

### 6.2 Classpath 加载流程

`SkillUtils.loadResource(Resource)`：

1. 尝试 `resource.getFile()` 按文件系统加载 → 成功则按 6.1 处理
2. 失败时调用 `loadJarResource(resource)`：
    - 获取资源 URL，若为 `FileNotFoundException` 且为 `ClassPathResource`，回退到 `loadFromClasspath`
    - 若协议为 `jar`，通过 `JarURLConnection` 获取 `JarFile`，按 entry 前缀扫描所有 `SKILL.md` 条目
3. `loadFromClasspath(classpathPrefix)` 策略：
    - 主路径：`PathMatchingResourcePatternResolver.getResources("classpath*:" + prefix + "/**/SKILL.md")`
    - 回退路径：通过 `ClassLoader.getResources("META-INF/MANIFEST.MF")` 枚举所有 JAR，手动扫描每个 JAR 内匹配前缀的 `SKILL.md` 条目

### 6.3 basePath 推导规则

| 来源 | basePath 取值 |
| ---- | ------------- |
| 文件系统 | `SKILL.md` 所在目录的绝对路径 |
| JAR（带显式条目） | 资源 URL 去除 `jar:file:...!/` 前缀与 `/SKILL.md` 后缀 |
| JAR（无显式条目） | JAR entry 路径去除末尾 `/SKILL.md` |

---

## 七、最佳实践

### 7.1 技能设计原则

1. **单一职责**：一个技能聚焦一个领域/场景，避免在单个 `SKILL.md` 中塞入过多分支逻辑。
2. **流程显式化**：用 `Step 1 / Step 2 / ...` 明确步骤顺序，模型遵循性更好。
3. **工具调用条件**：在正文中显式声明"当存在 XXX 工具时才调用"，避免模型在不具备该工具时反复尝试调用。
4. **边界与不确定性**：明确告诉模型"不确定时说明不确定，不要编造上下文"，降低幻觉风险。
5. **参考资料外置**：将详细检查清单、样例等放到 `references/` 目录，正文通过路径引用，避免主文件过长。

### 7.2 与 AIAgent 协作的要点

1. **启用循环工具调用**：技能场景几乎都需要多步骤执行，务必设置：
   ```java
   agent.setEnableLoopToolCall(true);
   agent.setMaxLoopToolCalls(80);
   ```
2. **合理设置 System Prompt**：在 System Prompt 中明确指引模型"先调用 Skill 工具加载技能"，能显著提高技能命中率。
3. **配套工具组合**：技能负责"指导"，工具负责"执行"。常见组合：
    - 文件类技能 + `FileFunctionTool`
    - 运维类技能 + `CLIShellFunctionTool` + `GetOSFunctionTool`
    - 代码类技能 + `CodeExecuteFunctionTool` + 业务自定义工具
4. **会话存储**：配合 `StoreContext` 持久化会话，便于追踪多轮工具调用轨迹：
   ```java
   chatAgentMessage.setStoreContext(new StoreContext()
           .setUserId("user123")
           .setSessionSize(100)
           .setRequestId("request123")
           .setStoreType(StoreContext.STORE_TYPE_DB)
           .setDataSource("visualops"));
   ```

### 7.3 技能目录组织建议

#### 开发态

```
src/test/resources/skills/                # 测试用技能根目录
├── roll-dice/
│   └── SKILL.md
└── code-review-skill/
    ├── SKILL.md
    └── references/
        └── checklist.md
```

#### 生产态

将技能打包到 `src/main/resources/skills/`，随 JAR 一起发布，运行时通过 `addClasspathSkills("skills")` 加载。

#### 用户态

允许用户在磁盘上放置自定义技能，通过 `addDirectorySkills(path)` 加载，便于动态扩展而无需重新打包。

### 7.4 调试技巧

2. **查看 trace 信息**：`SkillFunctionCall` 已内置 `AgentTraceHolder` 轨迹记录，开启 tool trace 后可查看每次技能调用的入参、返回与耗时。
3. **basePath 校验**：技能返回内容的首行即 `Base directory for this skill: <path>`，可用于排查路径加载是否正确。

---

## 八、技能执行可观测性

### 8.1 可观测性概述

`SkillFunctionCall` 内置了完整的工具调用轨迹（Trace）机制，基于 `AgentTraceHolder` 与 `TraceMessage` 实现，对每次技能调用记录入参、出参、耗时与异常信息，便于线上问题定位、性能分析与调用链追踪。

可观测性具有以下特点：

1. **按需开启**：通过 `AgentTraceHolder.isToolTrace()` 判断是否启用工具轨迹，关闭时零开销。
2. **完整生命周期**：记录 `startTime` → 元数据 → `endTime` → 响应或异常，覆盖调用全流程。
3. **异常不丢轨迹**：在 `FunctionCallException` 与通用 `Exception` 两个捕获分支均会写入轨迹后再抛出，确保异常路径也有完整记录。
4. **轨迹记录容错**：轨迹记录本身发生异常会被静默吞掉，不影响业务主流程。
5. **统一消息角色**：所有轨迹记录的 `role` 字段为 `SessionMessage.MESSAGE_TYPE_TOOLCALL_MESSAGE_NAME`，与会话存储体系对齐。

### 8.2 启用与控制

技能调用的轨迹追踪由 `AgentTraceHolder` 统一管理。启用后，`SkillFunctionCall` 会在调用入口构造 `TraceMessage`，并在正常返回或异常抛出前通过 `AgentTraceHolder.trace(traceMessage)` 提交。

```java
// 启用工具轨迹（具体 API 以 AgentTraceHolder 实际方法为准）
// AgentTraceHolder 可通过全局开关或上下文控制是否记录工具调用轨迹
// 一旦开启，所有通过 SkillsToolRegist 注册的 Skill 工具调用都会被自动追踪
```

> **说明**：`AgentTraceHolder.isToolTrace()` 是轨迹记录的前置判断条件，返回 `false` 时 `SkillFunctionCall` 会跳过 `TraceMessage` 的构造与提交，性能开销接近零。

### 8.3 轨迹字段详解

#### 8.3.1 正常调用路径

当技能调用成功返回时，`TraceMessage` 中记录的字段如下：

| 字段 | 类型 | 来源 | 说明 |
| ---- | ---- | ---- | ---- |
| `startTime` | `long` | `System.currentTimeMillis()` | 调用开始时间戳（毫秒） |
| `toolName` | `String` | `functionTool.getFunctionName()` | 工具名称，对于技能工具固定为 `Skill` |
| `id` | `String` | `functionTool.getId()` | 工具调用 ID，由模型平台分配，用于关联请求与响应 |
| `type` | `String` | `functionTool.getType()` | 工具类型标识 |
| `index` | `Object` | `functionTool.getIndex()` | 工具调用序号，用于一次响应中多次工具调用的排序 |
| `toolCallArgs` | `Map<String, Object>` | `functionTool.getArguments()` | 工具入参，对于技能工具为 `{ "skillName": "<技能名>" }` |
| `role` | `String` | `SessionMessage.MESSAGE_TYPE_TOOLCALL_MESSAGE_NAME` | 消息角色，固定为工具调用消息类型 |
| `endTime` | `long` | `System.currentTimeMillis()` | 调用结束时间戳（毫秒） |
| `toolCallResponse` | `String` | 构造的结果字符串 | 工具返回内容，格式为 `Base directory for this skill: <basePath>\n\n<content>` 或 `Skill not found: <skillName>` |

> **耗时计算**：`endTime - startTime` 即为本次技能调用的总耗时，包含 `skillName` 参数解析、`skillMap` 查找与内容拼接。

#### 8.3.2 异常调用路径

当技能调用抛出异常时，`SkillFunctionCall` 分两层捕获，均会在 `traceMessage` 中补充异常信息后再重新抛出：

| 捕获分支 | 触发条件 | 记录字段 | 抛出行为 |
| -------- | -------- | -------- | -------- |
| `FunctionCallException` | `skillName` 为 null/空字符串，或框架内部抛出的业务异常 | `endTime`、`toolCallException`（`SimpleStringUtil.exceptionToString(e)`） | 原样重新抛出 |
| `Exception` | 其他未预期异常 | `endTime`、`toolCallException`（`SimpleStringUtil.exceptionToString(e)`） | 包装为 `FunctionCallException` 抛出 |

异常轨迹字段说明：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `endTime` | `long` | 异常发生时间戳（毫秒） |
| `toolCallException` | `String` | 通过 `SimpleStringUtil.exceptionToString(e)` 转换的完整异常堆栈字符串 |

#### 8.3.3 异常处理流程

```
try {
    构造 TraceMessage（若启用）并记录 startTime、toolName、id、type、index、toolCallArgs、role
    解析 skillName 参数
    查找 Skill 并构造响应内容
    记录 endTime、toolCallResponse
    提交 TraceMessage
    return result;
} catch (FunctionCallException e) {
    记录 endTime、toolCallException
    提交 TraceMessage（内部异常被吞掉）
    throw e;  // 原样抛出业务异常
} catch (Exception e) {
    记录 endTime、toolCallException
    提交 TraceMessage（内部异常被吞掉）
    throw new FunctionCallException(e);  // 包装为业务异常
}
```

### 8.4 已知异常场景

根据 `SkillFunctionCall` 实现，以下场景会触发异常路径并记录到轨迹：

| 场景 | 异常类型 | 异常消息 | 触发条件 |
| ---- | -------- | -------- | -------- |
| 参数缺失 | `FunctionCallException` | `skillName is null` | `arguments.get("skillName")` 返回 null 或空字符串 |
| 未命中技能 | **非异常** | 返回 `Skill not found: <skillName>` | `skillMap` 中不存在对应名称的技能（属于正常返回，会记录 `toolCallResponse`） |
| 其他未预期异常 | `FunctionCallException`（包装） | 包装原始异常 | 如 `Map` 操作、字符串构造等罕见运行时异常 |

> **重要**：技能未命中（`Skill not found`）**不会**抛异常，而是以正常返回路径记录到 `toolCallResponse`。可通过轨迹过滤 `toolCallResponse` 前缀 `Skill not found:` 来识别配置错误的技能调用。

### 8.5 轨迹数据使用场景

1. **调用链追踪**：结合 `id`、`index` 字段，可还原一次模型响应中多次技能调用的顺序与依赖关系。
2. **性能分析**：通过 `endTime - startTime` 统计技能调用耗时分布，识别慢调用（如大体积 `content` 拼接）。
3. **异常定位**：`toolCallException` 字段提供完整堆栈，便于定位 `skillName` 缺失、`skillMap` 未初始化等问题。
4. **配置校验**：扫描 `toolCallResponse` 以 `Skill not found:` 开头的记录，可发现模型调用了未注册的技能名（常见于 `SKILL.md` 的 `name` 字段与目录名不一致、或技能未正确加载）。
5. **审计与回放**：`toolCallArgs` 与 `toolCallResponse` 完整记录输入输出，可用于审计或会话回放。

### 8.6 使用建议

1. **生产环境建议开启**：技能调用通常涉及多轮工具协作，开启轨迹有助于快速定位"模型为什么没有调用技能"或"技能返回了什么"。
2. **结合会话存储**：`StoreContext` 的会话存储与 `AgentTraceHolder` 的工具轨迹互补，前者记录对话历史，后者记录工具调用细节。
3. **关注异常路径**：异常分支的 `toolCallException` 是排查问题的关键入口，建议接入日志/监控系统对 `FunctionCallException` 进行告警。
4. **避免依赖轨迹字段顺序**：`TraceMessage.put(...)` 采用链式 builder 模式，字段写入顺序由代码决定，业务消费时应以字段名而非顺序为准。
5. **轨迹记录异常不影响业务**：若 `AgentTraceHolder.trace()` 本身抛异常，`SkillFunctionCall` 会静默吞掉并继续抛出业务异常，避免观测机制反噬主流程。

### 8.7 轨迹记录示例

以下是一次成功的 `code-review-skill` 调用对应的 `TraceMessage` 结构示意：

```json
{
  "startTime": 1721100000000,
  "toolName": "Skill",
  "id": "call_abc123",
  "type": "function",
  "index": 0,
  "toolCallArgs": {
    "skillName": "code-review-skill"
  },
  "role": "toolcall",
  "endTime": 1721100000015,
  "toolCallResponse": "Base directory for this skill: C:\\workspace\\...\\code-review-skill\n\n# Java Code Review Skill\n这个 Skill 用来审查 Java 代码。\n..."
}
```

未命中技能的轨迹示例：

```json
{
  "startTime": 1721100000000,
  "toolName": "Skill",
  "id": "call_xyz789",
  "type": "function",
  "index": 0,
  "toolCallArgs": {
    "skillName": "non-exist-skill"
  },
  "role": "toolcall",
  "endTime": 1721100000002,
  "toolCallResponse": "Skill not found: non-exist-skill"
}
```

参数缺失的异常轨迹示例：

```json
{
  "startTime": 1721100000000,
  "toolName": "Skill",
  "id": "call_err001",
  "type": "function",
  "index": 0,
  "toolCallArgs": {},
  "role": "toolcall",
  "endTime": 1721100000001,
  "toolCallException": "org.frameworkset.spi.ai.model.FunctionCallException: skillName is null\n\tat org.frameworkset.spi.ai.skill.SkillFunctionCall.call(SkillFunctionCall.java:64)\n..."
}
```

---

## 九、API 速查表

### 9.1 SkillsToolRegist 方法速查

| 方法签名 | 返回值 | 说明 |
| -------- | ------ | ---- |
| `addClasspathSkills(String dir)` | `this` | 从 classpath 加载技能 |
| `addDirectorySkills(String dir)` | `this` | 从文件系统目录加载技能 |
| `setToolDescriptionTemplate(String tpl)` | `this` | 自定义工具描述模板 |

## 十、参考资源

- **默认模板**：[TOOL_DESCRIPTION_TEMPLATE.txt](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/main/resources/skill/TOOL_DESCRIPTION_TEMPLATE.txt)
- **测试用例**：
    - [SkillAgentTest.java](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/skills/SkillAgentTest.java)（roll-dice 案例）
    - [CodeViewAgentTest.java](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/skills/CodeViewAgentTest.java)（code-review-skill + 业务工具案例）
    - [ChecklistCodeViewAgentTest.java](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/skills/ChecklistCodeViewAgentTest.java)（纯技能驱动案例）
    - [CodeReviewTools.java](https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/skills/CodeReviewTools.java)（业务自定义工具示例）
- **技能示例**：
    - [roll-dice/SKILL.md](https://gitee.com/bboss/bboss-ai/tree/main/bboss-ai/src/test/resources/skills/roll-dice/SKILL.md)
    - [code-review-skill/SKILL.md](https://gitee.com/bboss/bboss-ai/tree/main/bboss-ai/src/test/resources/skills/code-review-skill/SKILL.md)
    - [code-review-skill/references/checklist.md](https://gitee.com/bboss/bboss-ai/tree/main/bboss-ai/src/test/resources/skillscode-review-skill/references/checklist.md)
    - [business-config-workflow/SKILL.md](https://gitee.com/bboss/bboss-ai/tree/main/bboss-ai/src/test/resources/skills/business-config-workflow/SKILL.md)（工作流类技能扩展案例）
- **配套文档**：[bboss ai内置工具使用文档](https://esdoc.bbossgroups.com/#/bboss-ai-innertools)
