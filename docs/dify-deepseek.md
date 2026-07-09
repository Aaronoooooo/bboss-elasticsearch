# Dify工作流+DeepSeek实战

基于Dify工作流+DeepSeek，实现可视化运营数据分析和图表生成，高铁查询功能。

1.数据运营分析：基于Dify工作流+DeepSeek，实现自然语言转SQL并调用工具从可视化运营Clickhouse数据库查询数据，生成实用的业务统计分析报告以及美观漂亮的Echart图表

2.高铁查询：基于Dify工作流+DeepSeek+MCP，实现高铁信息查询

## 一、环境说明

1. diffy账号

http://172.24.176.18

yinbp/yin123456

dify官方介绍

https://github.com/langgenius/dify/blob/main/README_CN.md

2. LLM模型（无法本地部署LLM模型服务，采用Deepseek官方模型）

https://api.deepseek.com/v1

调用Deepseek公网云服务，需从Deepseek官网获取apiKey

3. 向量模型和Rerank模型（采用Xinference进行本地部署）

   Xinference服务地址：

http://172.24.176.18/

Embedding向量模型  bge-large-zh-v1.5或者gte-Qwen2 

Rerank模型 bge-reranker-base



## 二、准备工作

### 1.环境安装

基于windows 11内置linux环境wsl部署dify和Xinference

#### 1.1 Xinference安装和启动

需事先安装好conda和和名称为xinference的python 3.10或以上虚拟环境

切换到xinference

conda activate xinference

![image-20260113142204724](images\dify\conda.png)

安装Xinference

pip install 'xinference==1.16.0' 

![image-20260113142317704](images\dify\installXinference.png)

启动Xinference

nohup xinference-local --host 0.0.0.0 --port 9997 >xinference.log &

![image-20260113143722524](images\dify\startXinference.png)

#### 1.2 安装和启动模型

安装向量模型：

![image-20260113144301018](images\dify\bge.png)

安装rerank模型![image-20260113144557218](images\dify\rerank.png)

#### 1.3 基于源码和docker部署安装dify

 事先在服务器上安装好docker环境

下载源码

git clone https://github.com/langgenius/dify.git

基于docker安装和启动dify

cd dify/docker

sudo docker compose up -d

![image-20260113141428381](images\dify\difystart.png)

停止dify

sudo docker compose stop

![image-20260113125721014](images\dify\difystop.png)





### 2.数据准备

准备渠道业务订单指标数据、渠道业务退订指标数据

通过可视化运营数据集成和流计算工具，每天定时从FTP服务器下载boss订单数据文件；从文件中采集渠道订单数据和退订数据，进行加工处理，进行指标预聚合计算；将加工后的订单、退订数据以及指标计算结果保存到Clickhouse中的渠道订单指标数据表和渠道退订指标数据表。

![image-20260113163803036](images\dify\数据准备作业.png)

可视化运营数据采集&流计算工具参考文档  https://esdoc.bbossgroups.com/#/db-es-tool

### 3.知识库准备

准备渠道业务订单指标数据表和渠道业务退订指标数据表结构说明文档，作为文本转sql的知识库文档

![image-20260113163559136](images\dify\knowledge.png)

### 4.数据查询服务工具准备

准备通用的Clickhouse数据库查询服务，服务采用java语言开发，基于可视化运营持久层实现，接收文本转换生成的sql语句，访问Clickhouse执行查询，以Json格式返回查询结果：

http://192.168.137.1:8080/visualops/channelfullview/queryData.api?sql=select chnl_name ,count(*) as orderbacks from iops_channel_orderback group by chnl_name order by orderbacks desc

![image-20260113174044481](images\dify\dbtool.png)

返回结果示例：

```json
[{"orderbacks":3488,"chnl_name":"泰移在线服务有限公司越州分公司"},{"orderbacks":2165,"chnl_name":"湖南丛茂科技有限公司"},{"orderbacks":556,"chnl_name":"湖南省君隆盛信息科技有限公司"},{"orderbacks":289,"chnl_name":"深圳市腾讯计算机系统有限公司"},{"orderbacks":258,"chnl_name":"广州易尊网络科技股份有限公司"},{"orderbacks":72,"chnl_name":"湖南快乐阳光互动娱乐传媒有限公司"},{"orderbacks":8,"chnl_name":"湖南幻之城网络科技有限公司"},{"orderbacks":7,"chnl_name":"广州骏伯网络科技有限公司"},{"orderbacks":1,"chnl_name":"长沙倍那网络科技有限公司"}]
```

工具配置

![image-20260113163411423](images\dify\Clickhouse.png)

### 5.MCP Server准备

基于FastMCP实现高铁查询MCP服务，定义了线路、站点、运行时长、票价检索功能

MCP服务地址：

http://192.168.137.1:8000/sse

服务配置

![image-20260113164032841](images\dify\mcpconfig.png)

### 6.模型准备

#### 6.1 LLM模型准备

准备好Deepseek官网模型服务，提前做好充值和生成Deepseek API key；

https://platform.deepseek.com/api_keys

![image-20260113160230334](images\dify\Deepseek.png)

#### 6.2 向量模型和Rerank模型准备

使用Xinference提前部署好向量模型和Rerank模型（知识库要用到）

参考章节1.2 安装和启动模型

#### 6.3 在dify中配置模型

![image-20260113165504491](images\dify\difymodelconfig.png)

## 三、Dify+Deepseek实战

### 关键步骤和要素

关键步骤

一）设计工作流

二） 调试工作流

三）发布运行

1. 在dify中直接运行工作量,进行问答

2. 将工作流发布成服务，集成到第三方应用系统

流程关键要素

LLM、知识库、工具、提示词



### 案例1 渠道订单和退订分析

流程说明：用户输入问题-》根据问题检索库表知识库-》结合检索到的库表结构，将问题转换为sql-》调用数据库查询工具，获取数据-》分析数据，生成图表和数据分析报告

定义流程

![image-20260113155252575](images\dify\订单统计退订统计.png)

![image-20260113155354506](images\dify\订单统计退订统计1.png)

![image-20260113155424050](images\dify\订单统计退订统计2.png)

运行流程

#### 问题准备

##### **1.单表查询分析**

***统计2025年1月份各渠道订单量***

**统计2025年1月份各地市退订量**

根据下单日期，统计2025年1月份渠道湖南快乐阳光互动娱乐传媒有限公司的产品资费订单量

根据退订日期，统计2025年1月份渠道湖南快乐阳光互动娱乐传媒有限公司产品资费退订量

![image-20260113175329185](images\dify\run1.png)

##### **2.多表关联查询分析**

**统计2025年1月份各渠道订单量和退订量**

**统计2025年1月份各地市订单量和退订量**

统计2025年1月份渠道湖南快乐阳光互动娱乐传媒有限公司的订单量和退订量

![image-20260113175728038](images\dify\run2.png)

![image-20260113175816003](images\dify\run3.png)

### 案例2 高铁查询

定义流程：根据用户输入问题查询高铁线路、站点信息、运行时长、票价信息

![image-20260113180518453](images\dify\tran.png)

运行流程

![image-20260113164350071](images\dify\runtrain.png)

## 附 工具定义

工具定义-mysql执行工具

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "可视化运营数据查询服务",
    "description": "可视化运营数据查询服务OpenAPI specification",
    "version": "v1.0.0"
  },
  "servers": [
    {
      "url": "http://192.168.137.1:8080/visualops/channelfullview"
    }
  ],
  "paths": {
    "/queryDataMysql.api": {
      "post": {
        "summary": "查询数据",
        "operationId": "queryDataMysql",
        "tags": [
          "数据查询"
        ],
        "requestBody": {
          "description": "查询数据的SQL语句",
          "required": true,
          "content": {
            "application/x-www-form-urlencoded": {
              "schema": {
                "type": "object",
                "properties": {
                  "sql": {
                    "type": "string"
                  }
                },
                "required": [
                  "sql"
                ]
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {}
  }
}

```

clickhouse查询工具

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "可视化运营数据查询服务",
    "description": "可视化运营数据查询服务OpenAPI specification",
    "version": "v1.0.0"
  },
  "servers": [
    {
      "url": "http://192.168.137.1:8080/visualops/channelfullview"
    }
  ],
  "paths": {
    "/queryData.api": {
      "post": {
        "summary": "查询数据",
        "operationId": "queryData",
        "tags": [
          "数据查询"
        ],
        "requestBody": {
          "description": "查询数据的SQL语句",
          "required": true,
          "content": {
            "application/x-www-form-urlencoded": {
              "schema": {
                "type": "object",
                "properties": {
                  "sql": {
                    "type": "string"
                  }
                },
                "required": [
                  "sql"
                ]
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {}
  }
}
```




