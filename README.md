# PYGISServer

> 基于 GeoServer / GeoTools / GeoWebCache 深度二次开发的一体化 GIS 服务发布与管理平台

[![Java](https://img.shields.io/badge/JDK-1.8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![GeoServer](https://img.shields.io/badge/GeoServer-2.19.2-blue.svg)](https://geoserver.org/)
[![GeoTools](https://img.shields.io/badge/GeoTools-25.2-blue.svg)](https://geotools.org/)
[![GeoWebCache](https://img.shields.io/badge/GeoWebCache-1.19.1-blue.svg)](https://www.geowebcache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 项目简介

PYGISServer 是一套自研的 GIS 服务发布与管理平台，对标并扩展了 GeoServer 的核心能力。平台支持矢量、栅格、DEM、三维模型（倾斜摄影 / glTF）等多源空间数据的**服务发布、瓦片缓存、空间分析与可视化管理**，对外提供 WMS / WMTS / TMS / REST 等标准 OGC 协议接口，并内置一套完整的管理后台。

**核心能力：**

- 🗺️ **地图服务发布**：Shapefile、GeoTIFF、PostGIS、MBTiles 等数据源一键发布为 WMS / WMTS / TMS / REST 服务
- ⚡ **瓦片缓存引擎**：内嵌 GeoWebCache，独立瓦片服务进程（PYTC），支持手动/自动切片种子任务、切片量预估、TMS 缓存包导出、WebSocket 实时推送切片进度
- ⛰️ **地形分析**：基于 GDAL 的 DEM 高程 / 坡度 / 坡向分析（点、线、面），异步任务模式
- 🧭 **网络分析**：基于 PostGIS / pgRouting 的最短/最优路径规划、地理编码与逆地理编码
- 🏗️ **三维模型服务**：倾斜摄影、glTF 模型的发布、在线预览与文件流式访问
- 🔐 **平台化管理**：用户 / 角色 / 权限（Shiro）、SLD 样式与图例管理、License 授权（truelicense）、服务访问统计、POI 查询、数据上传与 PostGIS 数据源管理

## 系统架构

```
┌────────────────────────────────────────────────────────────┐
│                      客户端 / 管理后台 (Vue)                 │
└──────────────────────────┬─────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────▼─────────────────────────────────┐
│              mapserver（主服务 · war 包）                     │
│  OGC 协议入口(WMS/WMTS/TMS/REST) │ 服务发布管理 │ 用户权限     │
│  地形/网络分析入口 │ 三维模型服务 │ License │ 统计分析          │
└───────┬──────────────────┬──────────────────┬──────────────┘
        │ Dubbo RPC        │ Dubbo RPC         │
┌───────▼───────┐  ┌───────▼────────┐  ┌──────▼────────┐
│  tileserver    │  │ render         │  │ 分析模块        │
│  (PYTC · 8091) │  │ (渲染引擎库)     │  │ terrainAnalysis│
│  GeoWebCache   │  │ GeoTools SLD   │  │ netAnalysis    │
│  切片/种子任务  │  │ 矢量/栅格渲染    │  │ GDAL/pgRouting │
└───────┬───────┘  └────────────────┘  └──────────────┘
        │
┌───────▼───────────────────────────────────────────────────┐
│  PostgreSQL/PostGIS │ Redis │ MongoDB │ SQLite/MBTiles     │
│  Zookeeper (Dubbo 注册中心) │ GDAL                          │
└────────────────────────────────────────────────────────────┘
```

## 技术栈

| 分类 | 技术 |
|---|---|
| 基础框架 | JDK 1.8 · Spring Boot 2.4.3 · Maven 多模块 |
| GIS 核心 | GeoServer 2.19.2 · GeoTools 25.2 · GeoWebCache 1.19.1 · JTS 1.18.1 · GDAL 2.2.0 |
| 服务通信 | Dubbo 2.7.8 · Zookeeper 3.6.2 · WebSocket（切片进度推送） |
| 数据存储 | PostgreSQL + PostGIS · Redis · MongoDB · SQLite / MBTiles |
| 持久层 | MyBatis 3.5.5 · Druid 连接池 |
| 安全授权 | Shiro 1.4.0 · truelicense（License 授权） · jasypt（配置加密） |
| 其他 | Swagger（API 文档） · POI（数据导入导出） · IKAnalyzer（中文分词） · SLD 样式渲染 |

## 模块说明

| 模块 | 说明 |
|---|---|
| `mapserver` | 主服务（war 包）：OGC 协议入口、服务发布管理、用户权限、三维模型服务、统计分析、内置 Vue 管理后台 |
| `tileserver` | 瓦片缓存服务 PYTC（独立可执行 jar，端口 8091）：内嵌 GeoWebCache，负责切片/种子任务与 TMS 缓存导出 |
| `render` | 地图渲染引擎库：基于 GeoTools 的 WMS / SLD 渲染、矢量与栅格数据加载 |
| `serverspi` | SPI 扩展点：定义跨模块服务接口，供 Dubbo 远程调用 |
| `terrainAnalysis` | 地形分析实现：DEM 高程 / 坡度 / 坡向（GDAL） |
| `netAnalysis` | 网络分析实现：路径规划（PostGIS/pgRouting + JTS）、地理编码 |
| `common` | 公共基础库：实体、工具类、Redis/SQLite/MBTiles 封装、MyBatis Mapper |

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.5+
- PostgreSQL（含 PostGIS 扩展，默认端口 5436）
- Redis 5.0+
- Zookeeper 3.6+
- MongoDB（可选，瓦片服务使用）
- GDAL 2.2.0（地形分析需要配置本地库）

### 数据库初始化

```bash
# 创建数据库 py_data 并启用 PostGIS，然后执行初始化脚本
psql -d py_data -f tileserver/src/sql/tile_create.sql
psql -d py_data -f tileserver/src/sql/tile_init.sql
```

### 构建

```bash
mvn clean package -DskipTests
```

构建产物：

- `mapserver/target/pygisserver.war` —— 主服务
- `tileserver/target/pytc.jar` —— 瓦片缓存服务

### 运行

```bash
# 1. 启动瓦片缓存服务（默认端口 8091）
java -jar tileserver/target/pytc.jar

# 2. 部署主服务：将 pygisserver.war 放入 Tomcat webapps 目录启动
#    访问 http://localhost:8080/pygisserver
```

> 数据库、Redis、Zookeeper 等连接配置见各模块 `src/main/resources/application.yml`。

## 核心 API 概览

### OGC 协议服务

| 接口 | 说明 |
|---|---|
| `GET /wms` | WMS 服务（GetMap / GetCapabilities） |
| `GET /pytc/service/wmts` | WMTS 瓦片服务 |
| `GET /pytc/service/tms/{version}/{layer}/{z}/{x}/{y}` | TMS 瓦片获取 |
| `GET /pytc/service/rest/{serverName}/{page}/{size}` | REST 要素分页查询 |

### 服务发布管理 `/dataServer`

| 接口 | 说明 |
|---|---|
| `POST /dataServer/publishServerData` | 发布地图服务 |
| `POST /dataServer/queryServerData` | 查询服务列表 |
| `GET /dataServer/updateServerStatus/{id}` | 启停服务 |
| `POST /dataServer/group` | 发布图层组 |

### 地形分析 `/analysis`

| 接口 | 说明 |
|---|---|
| `GET /analysis/dem/position|line|region` | 高程分析（点/线/面） |
| `GET /analysis/slope/position|line|region` | 坡度分析 |
| `GET /analysis/aspect/position|line|region` | 坡向分析 |
| `GET /analysis/result/{taskId}` | 获取异步分析结果 |

### 网络分析 `/netanalysis`

| 接口 | 说明 |
|---|---|
| `GET /netanalysis/routing/plan` | 最短/最优路径规划 |
| `GET /netanalysis/geocoding` | 地理编码 |
| `GET /netanalysis/reverseGeocoding` | 逆地理编码 |

### 瓦片缓存 `/pytc/tile`

| 接口 | 说明 |
|---|---|
| `GET /pytc/tile/calculate/size/{layer}/{gridSet}/{start}/{end}` | 预估切片数据量 |
| `POST /pytc/tile/manual/seed/task` | 创建手动切片任务 |
| `GET /pytc/tile/downloadServerData/{taskName}` | 下载 TMS 缓存包 |

完整接口文档：启动服务后访问 Swagger UI `/swagger-ui.html`。

## 项目截图

<!-- TODO: 替换为实际截图 -->
| 管理后台首页 | 服务发布 | 瓦片缓存任务 |
|---|---|---|
| ![home](docs/images/home.png) | ![publish](docs/images/publish.png) | ![tile](docs/images/tile.png) |

## 技术亮点

- **自研 WMS 实现**：未直接使用 GeoServer WMS 模块，基于 GeoTools 渲染管线自实现 WMS 协议解析与出图，支持 SLD 样式热更新
- **瓦片服务进程隔离**：切片服务独立部署（PYTC），与主服务通过 Dubbo 通信，避免大规模切片任务拖垮主服务
- **异步分析任务**：地形分析采用任务化设计，支持进度查询与任务终止，WebSocket 实时推送
- **多源数据统一发布**：Shapefile / GeoTIFF / PostGIS / MBTiles / 海图 S57 等统一抽象为服务发布模型
- **生产级权限与授权**：Shiro 细粒度权限控制 + truelicense 离线授权机制，支持商业化交付

## License

本项目仅供学习与技术交流使用。

---

> 📌 简历描述参考：
>
> **PYGISServer —— GIS 服务发布与管理平台**（Java / Spring Boot / GeoServer / GeoTools / PostGIS / Dubbo）
> 基于 GeoServer、GeoTools、GeoWebCache 深度二次开发的一体化 GIS 服务平台，实现矢量/栅格/DEM/三维模型数据的 WMS/WMTS/TMS/REST 服务发布、瓦片缓存切片（独立进程 + WebSocket 进度推送）、地形分析（高程/坡度/坡向）、网络分析（路径规划/地理编码）等核心能力；采用 Dubbo + Zookeeper 实现模块间服务治理，Shiro + truelicense 实现权限与商业授权。
