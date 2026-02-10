# 黑马点评 (HM-DianPing)

基于 Spring Boot + Redis 的高性能点评类项目，深度实践 Redis 在各种业务场景下的应用。

## 🚀 技术栈
*   **后端**: Spring Boot 2.7.x, MyBatis Plus, Lombok, Hutool
*   **数据库**: MySQL 8.0
*   **缓存/中间件**: Redis (核心), Redisson
*   **其他**: Lua 脚本, Redis Stream (消息队列)

## 🛠️ 核心功能模块与 Redis 应用

### 1. 短信登录 (Session 共享)
*   使用 Redis 保存验证码及用户信息，解决多服务 Session 共享问题。
*   基于 Token 机制实现用户身份验证。

### 2. 商铺缓存 (高性能查询)
*   **缓存穿透**: 存储空对象防止数据库压力过大。
*   **缓存击穿**: 分别使用 **互斥锁 (Mutex)** 和 **逻辑过期 (Logical Expiration)** 两种方案实现热点 Key 重建。
*   **缓存雪崩**: 基础数据预热及过期时间随机化。

### 3. 秒杀下单 (高并发处理)
*   **同步抢购**: 使用 Redis + Lua 脚本实现库存扣减和一人一单的原子性判断。
*   **异步解耦**: 使用 **Redis Stream** 作为消息队列，将订单入库操作异步化，极大提升系统吞吐量。
*   **分布式锁**: 引入 **Redisson** 解决集群环境下的一人一单安全问题。

### 4. 社交互动
*   **点赞排行榜**: 基于 Redis 的 **SortedSet** 实现，记录点赞时间并按权重排序。
*   **共同关注**: 利用 Redis 的 **Set** 集合运算 (交集) 实现。
*   **Feed 流**: 采用 **Push (推模式)** 配合 **SortedSet** 实现关注后的滚动分页查询。

### 5. 地理位置功能
*   使用 **Redis GEO** 数据结构实现商铺的地理坐标存储及附近商铺按距离排序、分页查询。

### 6. 数据统计 (大数据量优化)
*   **签到功能**: 使用 **Bitmaps** 记录用户签到状态，节省存储空间并支持快速位运算。
*   **UV 统计**: 使用 **HyperLogLog** 进行基数统计，实现超大数据量的去重计数。

## 📦 快速开始
1.  **环境准备**: JDK 1.8, MySQL 8.0, Redis 6.2+
2.  **数据库配置**: 导入 `src/main/resources/db/hmdp.sql` 到 MySQL。
3.  **配置文件**: 修改 `application.yaml` 中的 MySQL 和 Redis 连接信息。
4.  **启动**: 运行 `HmDianPingApplication`。

---
*注：由于项目涉及 Redis 6.2 新引入的 GEO 命令，请务必确保 Redis Server 版本不低于 6.2，否则附近商铺查询可能抛出 `ERR unknown command GEOSEARCH`。*
