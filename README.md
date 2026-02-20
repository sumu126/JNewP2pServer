# EP2P Server - P2P 文件分享信令服务器

基于 Spring Boot + Socket.IO + MyBatis-Plus 构建的高性能 P2P 文件分享信令服务器。

## 项目简介

EP2P Server 是一个去中心化的 P2P 文件共享系统的信令服务器，用于帮助客户端发现文件、建立 WebRTC 连接并实现多源下载。

## 主要特性

- 🚀 **高性能实时通信** - 基于 Netty 的 Socket.IO 服务器，支持 Socket.IO 3.x/4.x 客户端
- 📁 **分布式文件索引** - 全局文件索引，支持文件名和 SHA-256 哈希值搜索
- 🔗 **WebRTC 信令转发** - 帮助节点之间建立点对点连接
- 💾 **数据持久化** - 使用 MySQL + MyBatis-Plus 存储节点和文件信息
- ⚡ **多源下载支持** - 自动发现拥有同一文件的所有节点
- 🔍 **智能节点过滤** - 只返回在线活跃节点

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.6.13 | 后端框架 |
| netty-socketio | 2.0.11 | Socket.IO 服务器 (支持 v3/v4) |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Lombok | 最新 | 代码简化工具 |
| FastJSON | 2.0.24 | JSON 解析 |
| OkHttp | 4.9.3 | HTTP 客户端 |

## 项目结构

```
EP2pServer/
├── src/main/java/com/sumu/japdemo/
│   ├── JApDemoApplication.java          # 启动类
│   ├── config/
│   │   ├── SocketIOConfig.java          # Socket.IO 配置
│   │   └── SocketIOStarter.java         # 服务启动器
│   ├── socketio/
│   │   └── SignalingSocketIOModule.java # Socket.IO 事件处理
│   ├── entity/
│   │   ├── NodeInfo.java                # 节点实体
│   │   ├── FileInfo.java                # 文件实体
│   │   ├── NodeFile.java                # 节点-文件关联
│   │   └── dto/                         # 数据传输对象
│   ├── mapper/                          # MyBatis-Plus Mapper
│   │   ├── NodeInfoMapper.java
│   │   ├── FileInfoMapper.java
│   │   └── NodeFileMapper.java
│   ├── service/                         # 业务逻辑层
│   │   ├── NodeManagerService.java      # 节点管理接口
│   │   ├── FileIndexService.java        # 文件索引接口
│   │   └── impl/                        # 实现类
│   └── utils/                           # 工具类
├── src/main/resources/
│   ├── application.yaml                 # 应用配置
│   └── db/schema.sql                    # 数据库表结构
└── pom.xml                              # Maven 配置
```

## 快速开始

### 1. 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE ep2p CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构
mysql -u root -p ep2p < src/main/resources/db/schema.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ep2p?serverTimezone=Asia/Shanghai
    username: root
    password: your_password

socketio:
  port: 11451
  host: 0.0.0.0
```

### 4. 构建并运行

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/JApDemo-0.0.1-SNAPSHOT.jar

# 或直接在 IDE 中运行 JApDemoApplication.java
```

## Socket.IO 事件 API

### 客户端连接

```javascript
// 客户端示例 (JavaScript)
import { io } from 'socket.io-client';

const socket = io('http://localhost:11451', {
  transports: ['websocket', 'polling']
});
```

### 事件列表

| 事件名 | 方向 | 说明 |
|--------|------|------|
| `register-files` | Client → Server | 注册本地文件到全局索引 |
| `search-files` | Client → Server | 搜索文件（支持文件名/哈希） |
| `search-results` | Server → Client | 返回搜索结果 |
| `request-download` | Client → Server | 请求下载节点信息 |
| `download-nodes-found` | Server → Client | 返回可用下载节点 |
| `download-nodes-not-found` | Server → Client | 未找到下载节点 |
| `webrtc-signal` | Bidirectional | WebRTC 信令转发 |
| `heartbeat` | Client → Server | 心跳保活 |
| `heartbeat-ack` | Server → Client | 心跳响应 |

### 详细事件说明

#### 1. 注册文件

```javascript
// 发送
socket.emit('register-files', [
  {
    hash: 'sha256-hash-value',
    fileName: 'example.zip',
    fileSize: 1048576
  }
]);
```

#### 2. 搜索文件

```javascript
// 发送
socket.emit('search-files', '关键词或SHA256哈希');

// 接收
socket.on('search-results', (results) => {
  // results: [{ hash, fileName, fileSize, nodeCount, nodes, isExactMatch }]
});
```

#### 3. 请求下载节点

```javascript
// 发送
socket.emit('request-download', 'file-hash');

// 成功接收
socket.on('download-nodes-found', (data) => {
  // data: { fileHash, fileName, fileSize, nodes, nodeCount }
});

// 失败接收
socket.on('download-nodes-not-found', (data) => {
  // data: { fileHash, error }
});
```

#### 4. WebRTC 信令转发

```javascript
// 发送信令
socket.emit('webrtc-signal', {
  targetUserId: '目标节点ID',
  signal: { /* WebRTC offer/answer/ice */ }
});

// 接收信令
socket.on('webrtc-signal', (data) => {
  // data: { fromUserId, signal }
});
```

## 数据库设计

### t_node_info (节点信息表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| node_id | VARCHAR(64) | 节点唯一标识 (Socket.IO session ID) |
| is_active | TINYINT(1) | 是否在线 |
| ip_address | VARCHAR(50) | IP 地址 |
| last_heartbeat | DATETIME | 最后心跳时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### t_file_info (文件信息表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_hash | VARCHAR(64) | SHA-256 哈希值 (唯一) |
| file_name | VARCHAR(500) | 文件名 |
| file_size | BIGINT | 文件大小 (字节) |
| node_count | INT | 拥有该文件的节点数 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### t_node_file (节点-文件关联表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| node_id | VARCHAR(64) | 节点 ID |
| file_hash | VARCHAR(64) | 文件哈希 |
| create_time | DATETIME | 创建时间 |

**索引：** `(node_id, file_hash)` 唯一约束

## 与 JS 版本对比

| 特性 | JS 版本 | Java 版本 |
|------|---------|-----------|
| 数据存储 | 内存 (Map) | MySQL 持久化 |
| 文件索引 | 重启后丢失 | 永久保存 |
| 节点信息 | 内存管理 | 数据库管理 |
| 可扩展性 | 单机 | 支持分布式扩展 |
| Socket.IO 版本 | v3 | v3/v4 兼容 |
| 多源下载 | 支持 | 支持 |
| 性能 | 高 | 高 (基于 Netty) |

## 工作流程图

```
用户 A                    信令服务器                     用户 B
   │                           │                           │
   │── register-files ────────>│                           │
   │   (注册本地文件)           │                           │
   │                           │<────── register-files ───│
   │                           │   (注册相同文件)           │
   │                           │                           │
   │<─ search-results ────────│                           │
   │   (找到2个节点拥有该文件)   │                           │
   │                           │                           │
   │── webrtc-signal ─────────>│── webrtc-signal ────────>│
   │   (WebRTC offer)          │   (转发信令)              │
   │                           │                           │
   │<─ webrtc-signal ──────────│<── webrtc-signal ────────│
   │   (WebRTC answer)         │   (转发信令)              │
   │                           │                           │
   │═══════════════════════════ WebRTC P2P 连接 ═══════════│
   │                    直接传输文件数据                     │
```

## 配置参数说明

### Socket.IO 配置

```yaml
socketio:
  port: 11451                    # 监听端口
  host: 0.0.0.0                  # 绑定地址
```

### 数据库配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ep2p?serverTimezone=Asia/Shanghai
    username: root
    password: 1234
```

### MyBatis-Plus 配置

```yaml
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.sumu.japdemo.entity
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰
  global-config:
    db-config:
      id-type: auto                     # 主键自增
```

## 常见问题

### Q1: 客户端无法连接？

确保使用 Socket.IO 3.x 或 4.x 客户端。检查防火墙是否开放 11451 端口。

### Q2: 数据库连接失败？

检查 MySQL 是否启动，数据库 `ep2p` 是否已创建，用户名密码是否正确。

### Q3: 搜索结果为空？

确保至少有一个客户端已注册文件。检查 `t_node_file` 和 `t_file_info` 表中是否有数据。

### Q4: 只找到一个下载节点？

检查其他节点是否在线（Socket 连接是否保持）。系统会自动过滤离线节点。

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

项目地址: https://github.com/yourusername/EP2pServer
