# 🛍️ Kazever Shop — Java Backend

> self-made online eshop platform  
> Java 版重构 — 对应 [Python 原版](https://github.com/YoruAmeKaze/eshop)

一个完整的电商平台后端，提供用户认证、商品管理、购物车等功能。  
从 Python FastAPI 重构为 Java Spring Boot 3，**文件结构和功能安排与原版高度对应**，适合用来学习 Java Web 开发。

---

## 📂 项目结构 — 与原版 Python 一一对应

```
backend-java/
├── pom.xml                          ← requirements.txt (依赖管理)
├── src/main/java/com/eshop/
│   ├── EshopApplication.java        (启动类)
│   ├── CorsConfig.java              (跨域配置 — 类比 CORSMiddleware)
│   ├── MainController.java          ← main.py (所有路由 + DTO + 异常处理)
│   ├── AuthUtil.java                ← auth.py  (JWT + 密码加密)
│   └── DatabaseService.java         ← database.py (所有数据库操作 + Entity)
├── src/main/resources/
│   ├── application.yml              (配置文件)
│   └── static/                      (前端文件)
└── .gitignore
```

### 核心对照表

| 功能 | Python 位置 | Java 位置 |
|------|-----------|----------|
| 14 个 API 端点 | `main.py` 中 `@app.get/post/put/delete` | `MainController` 中 `@GetMapping/PostMapping/PutMapping/DeleteMapping` |
| 4 个请求体模型 | `main.py` 中 `class RegisterRequest(BaseModel)` 等 | `MainController` 中 4 个内部 `static class` |
| 全局异常处理 | `main.py` 中 `raise HTTPException` | `MainController` 中 `@ExceptionHandler` |
| JWT 生成与解析 | `auth.py` 中 `create_access_token` / `decode_token` | `AuthUtil` 中 `generateToken` / `getUserIdFromToken` |
| SHA-256 密码加密 | `database.py` 中 `hash(password)` | `AuthUtil.hashPassword()` |
| 用户注册/登录/查询 | `database.py` 中 `class user_database` | `DatabaseService` 中用户相关方法区 |
| 商品 CRUD + 分页 + 随机 | `database.py` 中 `class shop` | `DatabaseService` 中商品相关方法区 |
| 购物车增删改查 | `database.py` 中 `class cart` | `DatabaseService` 中购物车相关方法区 |
| 分类校验 | `main.py` 中 `VALID_CATEGORIES` | `MainController` 中 `VALID_CATEGORIES` |

---

## ⚙️ 技术栈

| 层 | 技术 | 类比 Python |
|----|------|-------------|
| Web 框架 | **Spring Boot 3.2** + Spring MVC | FastAPI |
| 数据库 | **Spring Data JPA** + MySQL 8.0 | pymysql |
| 认证 | **JWT** (jjwt 0.12.x) | python-jose |
| 密码 | **SHA-256** | hashlib.sha256 |
| 构建 | **Maven** | pip |
| 前端 | 纯 HTML/CSS/JS（与原版共用） | 一致 |
| Java 版本 | **17+** | — |

---

## 🚀 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- MySQL 8.0（运行在 `localhost:3306`）

### 运行

```bash
# 1. 进入项目目录
cd backend-java

# 2. 启动（会自动建表）
mvn spring-boot:run

# 3. 打开浏览器
open http://localhost:8000
```

> `spring.jpa.hibernate.ddl-auto=update` 会自动根据 Entity 创建表结构，  
> 所以**不需要手动建表**，但 MySQL 必须已启动。

---

## 📡 API 接口

| 方法 | 路径 | 功能 | 需登录 |
|------|------|------|:----:|
| POST | `/api/register` | 注册 | ❌ |
| POST | `/api/login` | 登录 | ❌ |
| GET | `/api/user/me` | 获取当前用户 | ✅ |
| GET | `/api/merchant/products` | 商家查看商品 | ✅ |
| POST | `/api/merchant/products` | 商家添加商品 | ✅ |
| PUT | `/api/merchant/products/{id}` | 商家修改商品 | ✅ |
| DELETE | `/api/merchant/products/{id}` | 商家删除商品 | ✅ |
| GET | `/api/products` | 消费者分页浏览 | ❌ |
| GET | `/api/products/{id}` | 商品详情 | ❌ |
| GET | `/api/products/random` | 随机推荐 | ❌ |
| GET | `/api/cart` | 查看购物车 | ✅ |
| POST | `/api/cart` | 添加到购物车 | ✅ |
| DELETE | `/api/cart/{goodsId}` | 删除购物车项 | ✅ |
| PUT | `/api/cart/{goodsId}` | 调整数量 | ✅ |

---

## 📦 部署

Docker 部署可参照 Python 版的 `compose.yaml`，修改 backend 镜像即可。

```bash
# 构建镜像
mvn package -DskipTests
docker build -t eshop-backend-java .
```

---

## 🧑‍💻 学习建议

如果你想用这个项目学 Java，建议按这个顺序看代码：

1. **`EshopApplication.java`** — Spring Boot 入口，5 行代码
2. **`MainController.java`** — API 路由怎么写，对比 main.py
3. **`AuthUtil.java`** — JWT 原理 + 密码加密
4. **`DatabaseService.java`** — Java 怎么做数据库操作（JPQL + 原生 SQL）
5. **`application.yml`** — 配置文件格式

---

## 📜 许可证

MIT
