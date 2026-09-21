# 心理健康 AI 助手 · 后端

心理健康 AI 助手的后端服务，提供 AI 对话、心理状态评估、情绪日记与知识库接口。前端仓库见 [mental-health-frontend](https://github.com/hearhearwo/mental-health-frontend)。

> 本产品是心理支持 / 自助工具，不是医疗产品。AI 建议仅供参考，不构成医学诊断。

## 技术栈

- Java 17
- Spring Boot 3.0.2
- MyBatis-Plus 3.5.3
- MySQL
- JWT（jjwt 0.11.5）
- BCrypt（spring-security-crypto，仅取加密能力）
- Caffeine（验证码内存缓存，自带过期淘汰）
- JavaMail（SMTP 发送注册验证码）
- DeepSeek（AI 对话，未配置 API Key 时自动回退本地规则）

## 接口一览

统一响应格式：`{ code, data, message }`，`code === 0` 表示成功。
鉴权方式：请求头 `Authorization: Bearer <JWT>`（除 login / register / email-code 外均需登录）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/auth/login | 账号密码登录 |
| POST | /api/auth/register | 邮箱 + 密码 + 验证码注册，成功直接返回 token |
| POST | /api/auth/email-code | 发送 6 位验证码到邮箱（60 秒冷却） |
| POST | /api/auth/logout | 登出 |
| GET | /api/profile/status | 心理状态评估 |
| GET | /api/conversations | 会话列表 |
| POST | /api/conversations | 新建会话 |
| GET | /api/conversations/{id}/messages | 会话消息 |
| POST | /api/conversations/{id}/messages | 发送消息 |
| DELETE | /api/conversations/{id} | 删除会话 |
| GET | /api/diaries | 情绪日记列表 |
| POST | /api/diaries | 新建日记 |
| GET | /api/articles | 知识文章列表 |
| GET | /api/articles/{id} | 文章详情 |
| POST | /api/articles/{id}/favorite | 收藏 / 取消收藏 |

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p mental_health_assistant < sql/batch1.sql
mysql -uroot -p mental_health_assistant < sql/batch2.sql
mysql -uroot -p mental_health_assistant < sql/batch3.sql
```

- `batch1.sql`：建库建表 + admin 账号
- `batch2.sql`：情绪日志扩展
- `batch3.sql`：用户表新增 `email` 列（邮箱注册）

### 2. 配置环境变量

所有敏感配置均从环境变量读取，未提供时使用安全的本地开发默认值。

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| DB_USERNAME | 否（默认 root） | MySQL 用户名 |
| DB_PASSWORD | 是 | MySQL 密码 |
| JWT_SECRET | 生产必填 | JWT 签名密钥（≥32 字节），不设则用本地开发默认值 |
| DEEPSEEK_API_KEY | 否 | DeepSeek API Key，不设则回退本地规则回复 |
| SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD | 否 | 邮箱验证码发信配置，不设则验证码打印到控制台 |

> ⚠️ 部署上线务必设置 `JWT_SECRET` 与 `DEEPSEEK_API_KEY`，否则使用仓库内可见的默认值签发 token 可被伪造。

### 3. 运行

```bash
# 开发
mvn spring-boot:run

# 打包运行
mvn clean package
java -jar target/mental-health-0.0.1-SNAPSHOT.jar
```

默认监听 `8080` 端口。

## 目录结构

```
src/main/java/com/example/mentalhealth/
├── ai/          # AI 服务（DeepSeek + 规则兜底、危机检测）
├── common/      # 统一响应、异常、用户上下文
├── config/      # Web/CORS、密码、AI、异步配置
├── controller/  # 控制器
├── dto/         # 请求/响应对象
├── entity/      # 实体
├── interceptor/ # JWT 鉴权拦截器
├── mapper/      # MyBatis Mapper
├── service/     # 业务逻辑
└── util/        # JWT 工具
```

## 协议

[MIT](LICENSE)
