# Redis 数据结构与持久化机制

## 一、Redis 支持的数据结构

### 1. String（字符串）

最基本的类型，类似于 Java 的 String。

```java
SET key "hello"
GET key  // "hello"
```

**应用场景：**
- 缓存简单值
- 计数器
- session 存储

### 2. List（列表）

按插入顺序排序的字符串列表。

```java
LPUSH mylist "world"
LPUSH mylist "hello"
LRANGE mylist 0 -1  // ["hello", "world"]
```

**应用场景：**
- 消息队列
- 最新列表（如最新动态）
- 时间线

### 3. Hash（哈希）

键值对集合，类似于 Java 的 HashMap。

```java
HSET user:1001 name "张三"
HSET user:1001 age "25"
HGET user:1001 name  // "张三"
HGETALL user:1001  // {"name": "张三", "age": "25"}
```

**应用场景：**
- 对象存储
- 商品信息
- 用户画像

### 4. Set（集合）

无序且不重复的字符串集合。

```java
SADD myset "apple"
SADD myset "banana"
SADD myset "apple"  // 忽略，already exists
SMEMBERS myset  // {"apple", "banana"}
```

**应用场景：**
- 标签系统
- 好友关系
- 去重

### 5. ZSet / Sorted Set（有序集合）

每个元素关联一个分数，按分数排序。

```java
ZADD leaderboard 100 "player1"
ZADD leaderboard 200 "player2"
ZADD leaderboard 150 "player3"
ZRANGE leaderboard 0 -1 WITHSCORES  // ["player1", "player2", "player3"]
```

**应用场景：**
- 排行榜
- 有序队列
- 延时队列

## 二、Redis 持久化机制

### 1. RDB（Redis Database）持久化

在指定时间点生成数据集的快照。

**触发方式：**

| 方式 | 配置 | 说明 |
|------|------|------|
| 自动触发 | `save 900 1` / `save 300 10` / `save 60 10000` | 满足条件时触发 |
| 手动触发 | `BGSAVE` / `SAVE` | 后台/同步保存 |
| shutdown | `shutdown` 命令 | 关闭前自动触发 |

**工作原理：**
- 使用 `fork()` 系统调用创建子进程
- 父进程继续处理请求
- 子进程将数据写入临时 RDB 文件
- 完成后替换旧的 RDB 文件

**优点：**
- 恢复速度快
- 生成的文件紧凑
- 适合大规模数据备份

**缺点：**
- 可能丢失最后一次快照后的数据
- fork 操作开销（内存copy-on-write）

**配置示例：**
```conf
save 900 1      # 900秒内1个key变化触发
save 300 10     # 300秒内10个key变化触发
save 60 10000   # 60秒内10000个key变化触发
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /var/lib/redis
```

### 2. AOF（Append Only File）持久化

记录每个写操作命令到日志文件。

**同步策略：**

| 策略 | 安全性 | 性能 |
|------|--------|------|
| `always` | 最高 | 最差（每个命令同步） |
| `everysec` | 中等 | 较好（每秒同步） |
| `no` | 最低 | 最好（由系统决定） |

**工作原理：**
- 记录所有写操作到 AOF 文件
- 支持日志重写（rewrite）压缩文件
- 恢复时重放所有命令

**优点：**
- 数据安全性更高
- 丢失数据概率小
- 支持日志重写压缩

**缺点：**
- 文件通常比 RDB 大
- 恢复速度较慢
- 可能影响服务器性能

**配置示例：**
```conf
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-load-truncated yes
aof-use-rdb-preamble yes
```

## 三、RDB vs AOF 对比

| 特性 | RDB | AOF |
|------|-----|-----|
| 文件大小 | 紧凑 | 较大 |
| 恢复速度 | 快 | 慢 |
| 数据安全性 | 可能丢失数据 | 丢失概率小 |
| 性能影响 | fork 开销 | 持续写开销 |
| 支持 | 支持 | 支持 |
| 优先级 | 主从复制用 | 优先使用 |

## 四、混合持久化（5.0+）

Redis 5.0 引入了 RDB + AOF 混合持久化：

```conf
aof-use-rdb-preamble yes
```

恢复流程：
1. 先加载 RDB 文件
2. 再重放 RDB 后的 AOF 日志

**优势：**
- 恢复速度快（RDB）
- 数据丢失少（AOF）

## 五、生产环境建议

**高可用场景：**
- 开启 AOF + RDB 混合持久化
- 使用 Redis Sentinel 或 Cluster

**性能优先场景：**
- 开启 RDB 定期备份
- 关闭 AOF（纯内存操作）

**一般生产环境：**
```conf
# 持久化配置
save 900 1
save 300 10
save 60 10000

appendonly yes
appendfsync everysec
aof-use-rdb-preamble yes

# 压缩
rdbcompression yes
aof-rewrite-inject-fsync yes
```