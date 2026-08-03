# 智慧鱼塘预约管理系统

一套面向鱼塘/垂钓场的预约、点餐、收银、看板一体化管理后台。支持多角色（管理员、商家、顾客）操作，覆盖从鱼塘发布、钓位预约、到店核销、餐厅点餐到营收统计的完整业务流程。

## 技术栈

- **后端**：Spring Boot 2.7 + MyBatis-Plus + MySQL 8 + Redis
- **前端**：Vue 3 + TypeScript + Vite + Element Plus
- **其他**：JWT 登录鉴权、SSE 实时通知、定时任务（预约过期释放）

## 功能模块

| 模块 | 说明 |
|------|------|
| 鱼塘管理 | 发布/编辑鱼塘、上传平面图、设置时段与价格 |
| 钓位管理 | 批量创建钓位、拖拽定位、钓位状态看板 |
| 预约管理 | 在线预约、支付、核销、退单 |
| 餐厅点餐 | 按鱼种定价 + 做法加价（如清蒸、红烧、酸菜鱼），支持绑定钓位配送 |
| 商城 | 渔具、饵料等商品售卖 |
| 会员/员工 | 商家子账号、邀请码、权限控制 |
| 数据统计 | 营收报表、预约统计、抽奖活动 |
| 分享看板 | 钓位实时状态可通过链接分享给顾客 |

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+

### 1. 创建数据库

```sql
CREATE DATABASE fishing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `src/main/resources/application.yml` 中的数据库与 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fishing?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
  redis:
    host: localhost
    port: 6379
```

### 2. 启动后端

```bash
# 方式一：Maven
mvn spring-boot:run

# 方式二：IDE 直接运行 FishingReservationApplication.java
```

后端默认端口：`8080`

启动时会自动建表并写入演示数据（可在 `application.yml` 中关闭 `app.demo-data-enabled`）。

### 3. 启动前端

```bash
cd fishing-pc
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 默认账号

项目启动后会自动初始化以下账号：

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 系统管理员 |
| 18800000001 | 123456 | 商家（老板） |
| 18800000002 | 123456 | 普通用户 |

## 项目目录

```
fish/
├── src/main/java/com/example/fishing    # 后端源码
│   ├── controller                       # 控制器
│   ├── service                          # 业务逻辑
│   ├── entity                           # 实体类
│   ├── mapper                           # MyBatis-Plus Mapper
│   ├── config                           # 配置类、初始化数据
│   └── vo/dto                           # 视图/传输对象
├── src/main/resources                   # 配置文件
├── fishing-pc/                          # 前端源码
│   ├── src/api                          # 接口请求
│   ├── src/views                        # 页面
│   ├── src/components                   # 公共组件
│   └── src/router                       # 路由
└── README.md
```

## 注意事项

1. **首次启动**：建议保持 `app.demo-data-enabled: true`，以便快速体验完整功能。
2. **商家权限**：商家账号角色为 `2`（ROLE_MERCHANT），管理员角色为 `1`（ROLE_ADMIN）。
3. **餐厅做法加价**：菜单支持 `cooking_methods` JSON 字段配置不同做法及加价，例如 `[{"name":"清蒸","price":0},{"name":"红烧","price":500}]`。
4. **图片资源**：前端页面中的示例图片路径以 `/demo-assets/` 开头，开发时可将对应图片放到 `fishing-pc/public/demo-assets/` 下。

## 许可证

本项目仅供学习交流使用。
