# Grep 文件检索工具使用指南

## 工具简介

`GrepFunctionTool` 提供两个内置工具：**grep**（检索内容）和 **grepCount**（统计匹配行数），跨平台兼容 Linux/Mac（调用 grep 命令）和 Windows（调用 findstr/PowerShell）。

## 快速接入

```java
// 创建工具实例，参数 60 为命令执行超时（秒）
GrepFunctionTool grepTool = new GrepFunctionTool(60)
    .addBaseDirectory("C:\\workspace\\docs");  // 限定搜索根目录（安全沙箱）

// 注册到 AIAgent
AIAgent aiAgent = new AIAgent(message);
aiAgent.registBeanTool(grepTool);
```

> `addBaseDirectory` 设置搜索的根目录，工具会校验用户指定的 path 必须在允许的目录范围内。

## grep — 检索匹配内容

在文件或目录中搜索匹配指定模式的文本行，返回匹配的文件名、行号和内容。

### 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `pattern` | String | 是 | — | 搜索模式，支持正则表达式 |
| `path` | String | 是 | — | 搜索起始路径，可以是文件或目录 |
| `recursive` | Boolean | 否 | true | 路径为目录时是否递归搜索子目录 |
| `caseSensitive` | Boolean | 否 | true | 是否区分大小写 |
| `fileExtensions` | String | 否 | 空（搜索所有文件） | 限定文件扩展名，如 `"*.java"` 或 `"*.java,*.xml"` |
| `maxCountPerFile` | Integer | 否 | 0（不限制） | 每个文件最多返回的匹配行数 |
| `showLineNumbers` | Boolean | 否 | true | 是否显示匹配行的行号 |

### 完整案例（GrepToolTest）

```java
public class GrepToolTest {
	static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GrepToolTest.class);
	public static void main(String[] args) {
		//初始化maas平台服务
		HttpRequestProxy.startHttpPools("application-stream.properties");
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
		chatAgentMessage.setModel("deepseek-v4-pro");//指定模型
		chatAgentMessage.setMaas("deepseek");//指定对应的maas平台名称
		String question = "检索包含关键字多轮会话的文件";
		chatAgentMessage.setPrompt(question).setSystemPrompt("你是一个文件检索专家，可以根据用户要求从文件中检索包含用户要求关键字的文件内容");
		
		chatAgentMessage.setStream( true).setThinking(false);//.addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		String message = "根据用户问题：#[input.query]，调用文件检索工具grep，检索包含用户问题的文件内容。如果用户问题中没有指定文件目录，则将目录设置为空";
		AIAgent aiAgent = new AIAgent(message);
		aiAgent.registBeanTool(new GrepFunctionTool(60).addBaseDirectory("C:\\workspace\\bbossgroups\\bboss-elasticsearch\\docs"));//指定检索根目录
		
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = aiAgent.streamChat(chatAgentMessage);
		
		flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
				.doOnNext(chunk -> {
					if(chunk.isStepType()){
						System.out.println();
					}
					
					if(chunk.getData() != null) {
						System.out.print(chunk.getData());
					}
//					
				}) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {countDownLatch.countDown();System.out.println();logger.info("\n=== 流完成 ===");})
				.doOnError(error ->{countDownLatch.countDown(); logger.error("错误: " + error.getMessage(),error);})
				.subscribe();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
```

案例要点：
- `setSystemPrompt` 设定智能体角色为「文件检索专家」
- `AIAgent(message)` 中的 message 通过 `#[input.query]` 占位符引用用户提问，引导模型调用 grep 工具
- 流式输出通过 `Flux<ServerEvent>` 逐块打印，`chunk.isStepType()` 判断是否为工具调用步骤
- maas平台服务初始化：`HttpRequestProxy.startHttpPools("application-stream.properties");`,参考文档：https://esdoc.bbossgroups.com/#/bboss-ai?id=_24-maas%E6%9C%8D%E5%8A%A1%E9%85%8D%E7%BD%AE


## grepCount — 统计匹配行数

与 grep 参数基本一致，但**只返回匹配总行数，不返回具体内容**，适合只需要统计不需要看具体内容的场景。

### 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `pattern` | String | 是 | — | 搜索模式，支持正则表达式 |
| `path` | String | 是 | — | 搜索起始路径 |
| `recursive` | Boolean | 否 | true | 是否递归搜索子目录 |
| `caseSensitive` | Boolean | 否 | true | 是否区分大小写 |
| `fileExtensions` | String | 否 | 空 | 限定文件扩展名 |

> 相比 grep，grepCount 少了 `maxCountPerFile` 和 `showLineNumbers` 两个参数。

### 完整案例（GrepCountToolTest）

```java
public class GrepCountToolTest {
	static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GrepCountToolTest.class);
	public static void main(String[] args) {
		//初始化maas平台服务
		HttpRequestProxy.startHttpPools("application-stream.properties");
		//设置模型调用参数，
		ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
//		chatAgentMessage.setModel("MiniMax-M2.7").setMaas("minimax").setRetry(3);
//        chatAgentMessage.setModel("qwen3.7-plus").setMaas("qwenvlplus").setRetry(3);
		chatAgentMessage.setModel("deepseek-v4-pro");//指定模型
		chatAgentMessage.setMaas("deepseek");//指定对应的maas平台名称
		String question = "统计包含关键字多轮会话的文本行数";
		chatAgentMessage.setPrompt(question).setSystemPrompt("你是一个文件检索统计专家，可以根据用户要求从文件中统计包含用户要求关键字的文本行数");
		
		chatAgentMessage.setStream( true).setThinking(false);//.addParameter("max_tokens", 2048);
		
		CountDownLatch countDownLatch = new CountDownLatch(1);
		String message = "根据用户问题：#[input.query]，调用工具grepCount，统计包含用户问题的文本行数。如果用户问题中没有指定文件目录，则将目录设置为空";
		AIAgent aiAgent = new AIAgent(message);
		aiAgent.registBeanTool(new GrepFunctionTool(60).addBaseDirectory("C:\\workspace\\bbossgroups\\bboss-elasticsearch\\docs"));//指定检索根目录
		
		//通过bboss httpproxy响应式异步交互接口，请求Deepseek模型服务，提交问题
		Flux<ServerEvent> flux = aiAgent.streamChat(chatAgentMessage);
		
		flux.doOnSubscribe(subscription -> logger.info("开始订阅流..."))
				.doOnNext(chunk -> {
					if(chunk.isStepType()){
						System.out.println();
					}
					
					if(chunk.getData() != null) {
						System.out.print(chunk.getData());
					}
//					
				}) //打印流式调用返回的问题答案片段
				.doOnComplete(() -> {countDownLatch.countDown();System.out.println();logger.info("\n=== 流完成 ===");})
				.doOnError(error ->{countDownLatch.countDown(); logger.error("错误: " + error.getMessage(),error);})
				.subscribe();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
```

案例要点：
- `setSystemPrompt` 设定智能体角色为「文件检索统计专家」
- `AIAgent(message)` 中的 message 引导模型调用 grepCount 工具而非 grep
- 两个案例唯一差异：系统提示词（角色定位）和 AIAgent message（指定调用哪个工具），其余模型配置、流式处理逻辑完全一致
- maas平台服务初始化：`HttpRequestProxy.startHttpPools("application-stream.properties");`,参考文档：https://esdoc.bbossgroups.com/#/bboss-ai?id=_24-maas%E6%9C%8D%E5%8A%A1%E9%85%8D%E7%BD%AE

## 构造方式汇总

```java
new GrepFunctionTool()                          // 无超时限制
new GrepFunctionTool(60)                        // 超时 60 秒
new GrepFunctionTool(60, auditor)               // 带审计
new GrepFunctionTool("C:/base/dir")           // 指定基础目录
new GrepFunctionTool(auditor, "C:/base/dir")  // 带审计 + 基础目录

// 链式配置
new GrepFunctionTool(60)
    .addBaseDirectory("C:/dir1") 
    .setFollowSymlinks(true)    // 递归时跟随符号链接，默认 false
    .setTimeout(120);           // 修改超时时间
```

## 注意事项

- **Windows 正则限制**：Windows 下 findstr 不支持 `+`、`?`、`|`、`{n,m}`、`()` 等高级正则元字符；搜索中文等非 ASCII 字符时自动切换为 PowerShell 执行
- **path 参数**：模型调用时如果用户未指定目录，应传空字符串或基础目录下的路径，默认在`addBaseDirectory`限定的目录下检索
- **沙箱安全**：通过 `addBaseDirectory` 限定可搜索范围，防止越权访问，如用户问题中没有指定文件路径或者目录，则在指定的基础目录下搜索（目前只支持一个基础目录）
