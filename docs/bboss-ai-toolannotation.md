## AI 工具注解功能使用教程

本教程介绍如何通过 `@Tool` 和 `@ToolParam` 注解，将普通的 Java 方法快速定义为 AI 可调用的工具（Tool/Function Calling），并在 bboss AI 工作流智能体中使用。

---

### 一、核心注解概览

#### 1.1 @Tool 注解

`@Tool` 标注在方法上，用于声明该方法是一个 AI 可调用的工具。

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {
    String name() default "";           // 工具名称，默认为方法名
    String description();                // 工具描述，AI 根据此描述判断何时调用该工具
    String type() default "function";    // 工具类型，默认为 function
    boolean strict() default true;       // 是否启用严格模式校验参数
    boolean additionalProperties() default false; // 是否允许额外属性
}
```


| 属性 | 说明 |
|------|------|
| `name` | 工具的标识名称。若留空，默认使用方法名。 |
| `description` | **关键字段**。AI 模型通过该描述理解工具的用途，决定是否需要调用。 |
| `type` | 工具类型，通常保持默认 `function` 即可。 |
| `strict` | 是否对参数进行严格校验。 |
| `additionalProperties` | 参数对象是否允许传入未定义的属性。 |

#### 1.2 @ToolParam 注解

`@ToolParam` 可标注在**方法参数**和**类字段**上，用于描述每个参数/字段的类型、含义和约束，最终会被转换为 JSON Schema 供 AI 模型识别。当参数为复杂对象或数组时，框架会递归解析对象内部带有 `@ToolParam` 注解的字段作为子参数。

```java
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {
    String type() default "object";     // 参数类型：string/integer/number/array/object 等
    String name();                       // 参数名称
    String description();                // 参数描述，帮助 AI 理解如何填写该参数

    // ===== 数组元素配置（适用于原生数组类型，如 String[]） =====
    String elementType() default "";          // 数组元素类型
    String elementDescription() default "";   // 数组元素描述

    // ===== 字符串格式约束 =====
    String format() default "";          // 格式校验：email、hostname、ipv4、ipv6、uuid 等
    String pattern() default "";         // 正则表达式约束字符串格式

    // ===== 数组 items 配置（适用于简单类型数组，如 List<String>） =====
    String arrayItemType() default "";
    String arrayItemDescription() default "";

    // ===== 枚举值约束 =====
    String[] enumValues() default {};    // 枚举值列表，限制参数只能取指定值

    // ===== 数值类型约束（适用于 number/integer 类型） =====
    String constValue() default "";         // 固定为常数
    String defaultValue() default "";       // 默认值
    String minimum() default "";            // 最小值
    String maximum() default "";            // 最大值
    String exclusiveMinimum() default "";   // 不小于（排他最小值）
    String exclusiveMaximum() default "";   // 不大于（排他最大值）
    String multipleOf() default "";         // 必须为该值的倍数

    boolean required() default false;    // 是否必填
}
```


| 属性 | 说明 |
|------|------|
| `name` | 参数在 JSON Schema 中的名称。 |
| `description` | 参数的业务含义描述。 |
| `type` | 参数的数据类型，如 `string`、`integer`、`array`。框架也会根据 Java 参数类型自动推断。 |
| `required` | 是否必填。 |
| `format` | 字符串格式校验，支持 `email`、`hostname`、`ipv4`、`ipv6`、`uuid`。 |
| `pattern` | 正则表达式，如 `^\d{6}$` 校验邮编。 |
| `enumValues` | 枚举值列表，限制参数只能取指定值。 |
| `elementType` | 原生数组（如 `String[]`）的元素类型。 |
| `elementDescription` | 原生数组元素的描述。 |
| `arrayItemType` | 简单集合（如 `List<String>`）的元素类型。 |
| `arrayItemDescription` | 简单集合元素的描述。 |
| `constValue` | 数值固定为常数。 |
| `defaultValue` | 数值默认值。 |
| `minimum` / `maximum` | 数值最小/最大值。 |
| `exclusiveMinimum` / `exclusiveMaximum` | 排他最小/最大值。 |
| `multipleOf` | 数值必须为该值的倍数。 |

---

### 二、参数类型自动识别机制

`BeanToolHandle` 是注解工具的核心解析器，负责将 `@Tool` / `@ToolParam` 注解转换为 OpenAI 兼容的 Function Calling JSON Schema。解析过程中，框架会根据 Java 参数的**实际类型**自动推断 JSON Schema 类型，无需手动指定 `type` 属性。

#### 2.1 Java 类型与 JSON Schema 类型映射

| Java 类型 | JSON Schema 类型 | 说明 |
|-----------|-----------------|------|
| `java.lang.Object` | `object` | 通用对象 |
| `java.lang.String` | `string` | 字符串 |
| `int` / `Integer` | `integer` | 整数 |
| `long` / `Long` | `number` | 数字 |
| `double` / `Double` | `number` | 数字 |
| `float` / `Float` | `number` | 数字 |
| `boolean` / `Boolean` | `boolean` | 布尔值 |
| `java.lang.Number` | `number` | 数字（父类） |
| `java.util.List` | `array` | 数组 |
| `java.util.Set` | `array` | 数组 |
| `T[]`（原生数组） | `array` | 数组 |
| `java.util.Map` | `map` | 映射 |
| 其他自定义类 | `object` | 复杂对象 |

#### 2.2 对象类型参数的递归解析

当框架识别参数类型为 `object`（即自定义 Java 类）时，会调用 `parserToolObjectParams()` 递归解析该类的所有字段：

1. 通过反射获取类的所有字段描述（`ClassUtil.getClassInfo` → `getPropertyDescriptors`）
2. 筛选出带有 `@ToolParam` 注解的字段
3. 对每个字段重复类型推断和约束提取（`enumValues`、`minimum`、`maximum`、`pattern` 等）
4. 如果字段本身也是复杂对象或数组，继续递归解析

这意味着 **`@ToolParam` 注解可以同时用在方法参数和类的字段上**，框架会自动递归处理嵌套结构。

#### 2.3 数组类型参数的递归解析

当框架识别参数类型为 `array`（即 `List`、`Set` 或原生数组）时，分两种情况处理：

- **原生数组**（如 `String[]`）：使用 `@ToolParam` 的 `elementType` 和 `elementDescription` 属性指定数组元素类型和描述
- **泛型集合**（如 `List<TodoItem>`）：通过反射获取泛型参数类型，递归调用 `parserToolArrayParams()` 解析元素类的 `@ToolParam` 注解字段

如果集合元素是简单类型（如 `List<String>`），可通过 `arrayItemType` 和 `arrayItemDescription` 手动指定元素类型；如果元素是自定义对象，框架会自动递归解析其字段。

---

### 三、定义工具类

通过注解，任何一个普通的 Java 类都可以被声明为 AI 工具集。

#### 示例：酒店与机票预订工具

```java
package org.frameworkset.spi.ai.tools;

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

 
public class PreOrderTool {
    @Tool(name="hotelQuery",description = "根据用户的行程需求，查询合适的酒店。"
    )
    public List<Map> hotelQuery(@ToolParam(name="startDay",description = "入驻时间,例如：5月25日",required = true) String startDay,
                                @ToolParam(name="endDay",description = "离房时间,例如：5月28日",required = true) String endDay){
        List<Map> hotels = new ArrayList<>();
        Map hotelData = new LinkedHashMap();
        hotelData.put("name","迁移山水酒店");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","配套设施：健身房、保龄球");
        hotelData.put("position","位于市中心，交通便利");
        hotels.add(hotelData);

        hotelData = new LinkedHashMap();
        hotelData.put("name","俊逸酒店");
        hotelData.put("price","400$");
        hotelData.put("score",90);
        hotelData.put("devices","配套设施：健身房、保龄球、羽毛球");
        hotelData.put("position","位于郊区，环境优雅");
        hotels.add(hotelData);


        hotelData = new LinkedHashMap();
        hotelData.put("name","华天大酒店");
        hotelData.put("price","500$");
        hotelData.put("score",95);
        hotelData.put("devices","配套设施：健身房、保龄球、羽毛球、游泳池");
        hotelData.put("position","位于郊区，环境优雅，五星级环境");
        hotels.add(hotelData);
        return hotels;

    }

    @Tool(name="flightQuery",description = "根据用户的行程需求，查询合适的航班机票。" )
    public List<Map> flightQuery(@ToolParam(name="bookDay",description = "出发时间,例如：5月25日",required = true) String bookDay,
                                 @ToolParam(name="arriveDay",description = "到达时间,例如：5月28日",required = true) String arriveDay,
                                 @ToolParam(name="fromStation",description = "出发地,例如：长沙",required = true) String fromStation,
                                 @ToolParam(name="toStation",description = "到达地,例如：北京",required = true) String toStation){
        List<Map> hotels = new ArrayList<>();
        Map hotelData = new LinkedHashMap();
        hotelData.put("name","国航6678");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","波音777");

        hotelData.put("leaveTime","14点30分");
        hotelData.put("arrivedTime","17点30分");
        hotelData.put("description","宽体大飞机，准点率99%");
        hotels.add(hotelData);

        hotelData = new LinkedHashMap();
        hotelData.put("name","南航5578");
        hotelData.put("price","400$");
        hotelData.put("score",70);
        hotelData.put("devices","空壳380");

        hotelData.put("leaveTime","15点30分");
        hotelData.put("arrivedTime","18点30分");
        hotelData.put("description","宽体大飞机，准点率90%");
        hotels.add(hotelData);


        hotelData = new LinkedHashMap();
        hotelData.put("name","厦门航空3378");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","波音730");

        hotelData.put("leaveTime","16点30分");
        hotelData.put("arrivedTime","18点30分");
        hotelData.put("description","宽体大飞机，准点率100%");
        hotels.add(hotelData);
        return hotels;

    }

    


}
```


**关键点：**
- `@Tool` 的 `description` 决定了 AI 模型在什么场景下会选择调用该方法。
- `@ToolParam` 的 `description` 和 `required` 帮助 AI 模型正确提取和填充参数。

---

### 四、复杂参数：对象与数组嵌套解析

当工具方法需要接收结构化的复杂参数时，可以定义带 `@ToolParam` 注解字段的 Java Bean 类作为方法参数或集合元素类型。框架会自动递归解析 Bean 字段的 `@ToolParam` 注解，生成嵌套的 JSON Schema。

#### 4.1 示例：TodoTools 待办任务工具

以下示例来自 bboss-ai 内置的 `TodoTools`，演示了数组对象参数、内部类字段级注解、枚举约束等高级用法：

```java
package org.frameworkset.spi.ai.tools;

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import java.util.List;

public class TodoTools {

    private static final String DESCRIPTION =
        "Create and maintain a structured task list for the current session. Tracks progress,\n" +
        "organizes multi-step work, and surfaces status to the user. Pass the COMPLETE updated\n" +
        "list every call — this tool replaces the whole list (it does not merge).\n" +
        "\n" +
        "## When to use\n" +
        "Use proactively when:\n" +
        "- The task needs 3+ distinct steps or actions\n" +
        "- The work is non-trivial and benefits from planning\n" +
        "- The user gives multiple tasks (numbered or comma-separated) or asks for a todo list\n" +
        "- New instructions arrive — capture them as todos\n" +
        "- You start a task — mark it in_progress (only one at a time) before working\n" +
        "- You finish a task — mark it completed and add any follow-ups you discovered\n" +
        "\n" +
        "## States\n" +
        "- pending     — not started\n" +
        "- in_progress — actively working (exactly ONE at a time)\n" +
        "- completed   — finished successfully\n";

    /**
     * 工具方法：接收 List<TodoItem> 参数
     * 框架识别 List 类型为 array，递归解析泛型 TodoItem 类的字段注解
     */
    @Tool(
            name = "todo_write",
            description = DESCRIPTION)
    public String todoWrite(
            @ToolParam(
                    name = "todos",
                    description = "The COMPLETE updated todo list. Replaces the existing list entirely.",
                    required = true)
            List<TodoItem> todos) {

        // 业务逻辑：处理 todos 列表
        // ...
        return "Todo list updated.";
    }

    /**
     * 内部类：作为数组元素类型
     * 字段上的 @ToolParam 注解会被框架递归解析为 JSON Schema 的子属性
     */
    public static final class TodoItem {
        @ToolParam(name = "content",
                   description = "Brief, specific, actionable description of the task.",
                   required = true)
        private String content;

        @ToolParam(name = "status",
                   description = "One of: pending, in_progress, completed.",
                   enumValues = {"pending", "in_progress", "completed"},
                   required = true)
        private String status;

        @ToolParam(name = "priority",
                   description = "Optional priority: high, medium, or low.",
                   enumValues = {"high", "medium", "low"})
        private String priority;

        public String getContent() { return content; }
        public String getStatus() { return status; }
        public String getPriority() { return priority; }
    }
}
```

#### 4.2 生成的 JSON Schema 结构

上述 `todo_write` 工具经 `BeanToolHandle` 解析后，生成如下 JSON Schema：

```json
{
  "type": "function",
  "function": {
    "name": "todo_write",
    "description": "Create and maintain a structured task list ...",
    "parameters": {
      "type": "object",
      "properties": {
        "todos": {
          "type": "array",
          "description": "The COMPLETE updated todo list. Replaces the existing list entirely.",
          "items": {
            "type": "object",
            "properties": {
              "content": {
                "type": "string",
                "description": "Brief, specific, actionable description of the task."
              },
              "status": {
                "type": "string",
                "description": "One of: pending, in_progress, completed.",
                "enum": ["pending", "in_progress", "completed"]
              },
              "priority": {
                "type": "string",
                "description": "Optional priority: high, medium, or low.",
                "enum": ["high", "medium", "low"]
              }
            },
            "required": ["content", "status"]
          }
        }
      },
      "required": ["todos"]
    }
  }
}
```

**解析过程说明：**

1. `@Tool(name="todo_write")` → 映射为 `function.name`
2. `@ToolParam(name="todos") List<TodoItem>` → `getParamType(List.class)` 返回 `array`，属性 `type` 设为 `array`
3. 框架检测到泛型参数 `TodoItem`，调用 `parserToolArrayParams(TodoItem.class)` 递归解析
4. `TodoItem` 中三个字段带有 `@ToolParam` 注解：
   - `content` → `type: string`（String 类型自动推断），`required: true`
   - `status` → `type: string`，`enum: ["pending","in_progress","completed"]`，`required: true`
   - `priority` → `type: string`，`enum: ["high","medium","low"]`，`required` 未设置（默认 false）
5. `content` 和 `status` 被加入 `required` 数组；`priority` 因未设置 `required` 而不加入

#### 4.3 字段级注解说明

`@ToolParam` 的 `@Target` 同时包含 `PARAMETER` 和 `FIELD`，因此可以直接标注在 Java 类的字段上。当参数类型为 `object`（自定义类）或 `array`（集合元素为自定义类）时，框架会自动递归扫描这些字段注解并生成嵌套 Schema。框架通过类型推断自动处理，无需额外配置。

---

### 五、在工作流中注册和使用工具

定义好工具类后，需要将其注册到 AI 智能体工作流中。

#### 5.1 注册工具

使用 `BeanToolsRegist` 将工具类的实例注册为工具集：

```java
// 1. 实例化工具类
PreOrderTool preOrderTool = new PreOrderTool();

// 2. 创建工具注册器
ToolsRegist toolsRegist = new BeanToolsRegist(preOrderTool);
```


#### 5.2 将工具绑定到智能体节点

在 `AINodeAgent` 中通过 `setToolsRegist()` 绑定工具：

```java
planAgent.addRouteChoiceAgent(
    new AINodeAgent("请根据用户的行程需求查询并推荐合适的酒店...")
        .setAgentId("hotelAgent")
        .setAgentName("酒店查询智能体")
        .setToolsRegist(toolsRegist)   // 绑定工具
);
```


**说明：** 当该智能体节点执行时，如果 AI 模型判断需要调用外部工具，就会根据 `@Tool` 的描述自动选择 `hotelBook` 或 `flightBook` 方法，并提取用户问题中的参数进行调用。

---

### 六、完整工作流示例（非流式）

以下是一个完整的酒店+机票预订智能体工作流，演示了从路由判断到工具调用的完整流程：

```java
public class BookingTest {
        public static void main(String[] args) throws InterruptedException, IOException {
        // 初始化HTTP连接池
        HttpRequestProxy.startHttpPools("application-stream.properties");
        HttpRequestProxy.startHttpPools("mcpserver.properties");

        // 场景1：只查询酒店
//        bookingWorkflowStream("kimi", "帮我预定北京市中心5月25日到5月28日的五星级酒店", "kimi-k2.6", null);

        // 场景2：只查询机票
//        bookingWorkflowStream("kimi", "帮我预定5月25日上海到北京的机票，要上午的航班", "kimi-k2.6", null);

        // 场景3：酒店和机票都要（路由到并行查询）
//        bookingWorkflowStream("kimi", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "kimi-k2.6", null);

        bookingWorkflow("qwenvlplus", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "qwen3.6-plus", null);
    }
    public static void bookingWorkflow(String maas, String prompt, String model, String sessionId) throws InterruptedException {
        // 1. 定义会话实体：设置模型、maas平台，用户问题，开启流式输
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
            .setModel(model)
            .setMaas(maas)
            .setPrompt(prompt);

        //定义一个输出接口，可以用于输出各个智能体的执行结果
       AgentOutput agentOutput = new AgentOutput() {
            @Override
            public void output(ServerEvent message) {
                System.out.println("............................");
                if(message.getData() != null) {
                    System.out.println(message.getData());
                }
                else {

                    System.out.println(message.getFullStreamData());
                }
            }
        };
        // 定义工作流智能体，设置会话存储机制为DB
        AIPlanAgent planAgent = new AIPlanAgent(new StoreContext()
                .setSessionId(sessionId).setUserId("user123").setSessionSize(100)
                .setStoreType(StoreContext.STORE_TYPE_DB)
                .setDataSource("visualops"))
                .setAgentMessage(chatAgentMessage)
                .setAgentName("预定工作流智能体").setAgentId("bookingWorkflowAgent");

        // ====================  路由智能体（判断用户意图） ====================
        
        // 路由智能体判断用户意图：酒店、机票、都要
        planAgent.addAgent(new AIRouteAgent()
                .setAgentId("bookingRouter").setAgentName("预定路由智能体")
                .setSystemPrompt("你是一个行程预定路由智能体。请分析用户的问题，判断用户需要预定什么，注意你不需要直接回答用户的问题，只需要做路由判断。")
                .addRoutingChoice("hotelAgent", "用户只需要预定酒店")
                .addRoutingChoice("flightAgent", "用户只需要预定机票")
                .addRoutingChoice("bothAgent", "用户需要同时预定酒店和机票")
        );
        // 定义注册工具
        ToolsRegist toolsRegist = new BeanToolsRegist(new PreOrderTool());
        // ==================== 阶段2：分支查询智能体 ====================
        // 酒店查询智能体（当路由到hotelAgent时执行）
        planAgent.addRouteChoiceAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的酒店。" +
                        "需要考虑：地理位置、价格区间、用户评分、配套设施等因素。" +
                        "给出至少3个推荐选项，并说明理由。")
                .setAgentId("hotelAgent")
                .setAgentName("酒店查询智能体")
                
                .setToolsRegist(toolsRegist));

        // 机票查询智能体（当路由到flightAgent时执行）
        planAgent.addRouteChoiceAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的航班。" +
                        "需要考虑：出发时间、到达时间、航空公司、价格、准点率等因素。" +
                        "给出至少3个推荐选项，并说明理由。")
                .setAgentId("flightAgent").setAgentName("机票查询智能体") 
                .setToolsRegist(toolsRegist));

        // ==================== 阶段3：并行查询智能体（都要的场景） ====================
        // 当用户同时需要酒店和机票时，并行执行查询
        AIParrelAgent bothAgent = new AIParrelAgent(planAgent)
                .setAgentId("bothAgent").setAgentName("并行查询智能体");

        bothAgent.addAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的酒店。" +
                        "需要考虑：地理位置（尽量靠近市中心或商务区）、价格区间、用户评分、配套设施等因素。" +
                        "给出至少3个推荐选项，并说明理由。")
                .setAgentId("parrelHotelAgent").setAgentName("并行酒店查询")
                .setToolsRegist(toolsRegist));

        bothAgent.addAgent(new AINodeAgent(
                "请根据用户的行程需求:#[input.query]，查询并推荐合适的航班。" +
                        "需要考虑：出发时间、到达时间、航空公司、价格、准点率等因素。" +
                        "给出至少3个推荐选项，并说明理由。")
                .setAgentId("parrelFlightAgent").setAgentName("并行机票查询")
                .setToolsRegist(toolsRegist));

//        bothAgent.setAgentOutput(agentOutput);

        planAgent.addRouteChoiceAgent(bothAgent);

        // ==================== 阶段4：默认智能体 ====================
        // 当路由匹配不上时，直接回答用户问题
        planAgent.addDefaultRouteChoiceAgent(new AINodeAgent(
                "请根据用户的问题:#[input.query]，提供有帮助的行程和预定相关建议。")
                .setAgentId("defaultAgent").setAgentName("默认智能体") );

        // ==================== 阶段5：汇总智能体 ====================
        // 汇总前面所有节点的结果，给出最终的预定建议
        planAgent.addAgent(new AINodeAgent(
                "请综合前面的查询结果，为用户提供一份完整的预定建议报告。" +
                        "报告需要包含：1)推荐的酒店及理由 2)推荐的航班及理由 3)总预算估算 4)最终操作建议。" +
                        "请用清晰的中文输出。")
                .setAgentId("summaryAgent").setAgentName("汇总建议智能体").setOutputVaribleName("aaaa", AIFlowConst.AIFLOW_VAR_SCOPE_FLOW) );

        // 通过飞书mcp接口，将汇总智能体结果创建为飞书文档
        ToolsRegist mcpToolsRegist = new FeishuMcpRegist("feishumcp");
        planAgent.addAgent(new AINodeAgent(
                "请根据用户的问题:#[input.query]，以及前面的汇总建议，创建一份详细的飞书报告。" +
                        "报告需要包含：1)推荐的酒店及理由 2)推荐的航班及理由 3)总预算估算 4)最终操作建议。" +
                        "请用清晰的中文输出。")
                .setAgentId("feishudocAgent").setAgentName("飞书文档智能体").setToolsRegist(mcpToolsRegist) );
        // 9. 执行工作流
        LastSessionMessage result = planAgent.chat();
        System.out.println(result.getData());
    }
}
```


---

### 七、流式输出版本

如果需要在工具执行过程中实时看到 AI 的推理过程和结果，可以使用流式版本：

```java
public class BookingStreamTest {
        public static void main(String[] args) throws InterruptedException, IOException {
        // 初始化HTTP连接池
        HttpRequestProxy.startHttpPools("application-stream.properties");
        HttpRequestProxy.startHttpPools("mcpserver.properties");

        // 场景1：只查询酒店
//        bookingWorkflowStream("kimi", "帮我预定北京市中心5月25日到5月28日的五星级酒店", "kimi-k2.6", null);

        // 场景2：只查询机票
//        bookingWorkflowStream("kimi", "帮我预定5月25日上海到北京的机票，要上午的航班", "kimi-k2.6", null);

        // 场景3：酒店和机票都要（路由到并行查询）
//        bookingWorkflowStream("kimi", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "kimi-k2.6", null);

        bookingWorkflowStream("qwenvlplus", "我5月25日到5月28日要去北京出差，帮我预定酒店和机票", "qwen3.6-plus", null);
    }
    public static void bookingWorkflowStream(String maas, String prompt, String model, String sessionId) throws InterruptedException {
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage()
            .setModel(model)
            .setStream(true)   // 开启流式输出
            .setMaas(maas)
            .setPrompt(prompt);

        AIPlanAgent planAgent = new AIPlanAgent(...)
            .setAgentMessage(chatAgentMessage);

        // ... 工作流定义与非流式相同 ...

        // 流式执行
        Flux<ServerEvent> flux = planAgent.chatStream();
        flux.doOnNext(event -> {
            if (event.getData() != null) {
                System.out.print(event.getData());  // 实时输出内容
            }
            if (event.isToolCallsType()) {
                System.out.println("开始执行工具："); // 捕获工具调用事件
            }
        }).subscribe();
    }
}
```


**流式优势：**
- 实时展示 AI 思考过程和工具调用状态。
- 通过 `event.isToolCallsType()` 可以感知工具开始执行。
- 通过 `event.isDone()` 可以捕获会话结束事件。

---

### 八、常见问题与最佳实践

1. **description 怎么写？**
    - `@Tool` 的 `description` 应清晰描述工具的用途、适用场景、返回内容，这是 AI 判断是否调用的唯一依据。可以使用多行字符串编写详细的说明文档，包含使用时机、参数说明、规则等。
    - `@ToolParam` 的 `description` 应说明参数的业务含义、格式示例。

2. **工具方法返回值**
    - 推荐返回 `Map`、`List<Map>` 或 JSON 字符串，便于 AI 模型理解和后续处理。

3. **多个工具类**
    - 可以创建多个工具类，分别用 `BeanToolsRegist` 注册，并绑定到不同的智能体节点。

4. **参数为复杂对象（Bean）**
    - 框架已支持复杂对象参数的自动递归解析。当方法参数为自定义 Java 类时，框架会自动识别为 `object` 类型，递归扫描类中带有 `@ToolParam` 注解的字段，生成嵌套的 JSON Schema 子属性。
    - 框架通过 Java 类型自动推断并递归处理，无需额外配置。
    - `@ToolParam` 可同时标注在方法参数（`ElementType.PARAMETER`）和类字段（`ElementType.FIELD`）上。

5. **参数为数组/集合类型**
    - 当参数为 `List<T>`、`Set<T>` 或 `T[]` 时，框架自动识别为 `array` 类型。
    - 如果元素 `T` 为自定义对象，框架会递归解析 `T` 的 `@ToolParam` 字段作为 `items` 的子属性。
    - 如果元素为简单类型（如 `List<String>`），可使用 `arrayItemType` 和 `arrayItemDescription` 指定元素类型和描述。
    - 如果为原生数组（如 `String[]`），可使用 `elementType` 和 `elementDescription` 指定元素类型和描述。

6. **参数校验**
    - 利用 `format`（`email`、`ipv4` 等）、`pattern`（正则）、`enumValues`（枚举）、`minimum`/`maximum`（数值范围）、`exclusiveMinimum`/`exclusiveMaximum`（排他范围）、`multipleOf`（倍数）、`constValue`（常量）、`defaultValue`（默认值）等属性，可以生成符合 JSON Schema 规范的参数约束，让 AI 输出更准确的参数值。

7. **参数名称**
    - `@ToolParam` 的 `name` 属性指定参数在 JSON Schema 中的名称。若留空，则使用 Java 参数/字段的反射名称（注意：编译时需保留参数名，或使用框架的 `ClassUtil` 解析字段名）。

---

### 九、总结

通过 `@Tool` 和 `@ToolParam` 注解，开发者无需编写复杂的 JSON Schema，只需以自然的 Java 注解方式定义方法，即可让 AI 模型具备调用业务系统的能力。框架通过 `BeanToolHandle` 自动完成以下工作：

- **类型自动推断**：根据 Java 参数类型（String、Integer、List、自定义类等）自动映射 JSON Schema 类型
- **嵌套递归解析**：对 `object` 类型参数和 `array` 类型参数，自动递归解析带有 `@ToolParam` 注解的类字段，生成嵌套 Schema
- **约束自动提取**：从注解中提取 `enumValues`、`minimum`、`maximum`、`pattern`、`format` 等约束，生成标准 JSON Schema
- **必填参数收集**：自动将 `required = true` 的参数/字段收集到 `required` 数组中

结合 `AIPlanAgent` 工作流，可以实现路由判断、并行查询、工具调用、结果汇总等复杂的智能体协作场景。
