# 留学吧后端

Python 3.11+、FastAPI、PostgreSQL、SQLAlchemy 和 Alembic。Android 工程继续位于仓库的 `app/`，本目录只负责服务端。

## 目录职责

```text
app/
  api/           路由、HTTP 参数、身份依赖
  schemas/       请求校验和对外响应
  services/      注册、登录、当前用户的业务规则
  repositories/  用户查询、写入及事务回滚
  models/        数据表映射与约束
  db/            数据库连接池和会话
  core/          环境配置、密码散列和令牌
  application.py 应用组装、生命周期、统一错误处理
  main.py        服务入口
alembic/         数据库版本迁移
tests/           输入、安全、接口及真实数据库测试
```

依赖从路由指向服务，再指向数据访问。密码和令牌不在路由中处理；数据库表由迁移创建，应用启动不自动删表或重建表。

## 安装与配置

在本目录的 PowerShell 中执行：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e '.[dev]'
Copy-Item .env.example .env
.\.venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(48))"
```

把生成的随机值填入 `.env` 的 `LIUXUEBA_JWT_SECRET_KEY`，并填写数据库连接。`.env`、虚拟环境和测试缓存均已排除在 Git 外。已有配置时保留 `.env`，不要重复覆盖。

首次使用自己的 PostgreSQL 时，用管理账号创建独立应用账号和两个数据库。例如，替换下面的密码后执行：

```sql
CREATE ROLE liuxueba LOGIN PASSWORD 'REPLACE_WITH_YOUR_RANDOM_PASSWORD';
CREATE DATABASE liuxueba OWNER liuxueba;
CREATE DATABASE liuxueba_test OWNER liuxueba;
```

本机已配置的独立 PostgreSQL 位于 `%LOCALAPPDATA%\LiuXueBa\`，仅监听 `127.0.0.1:55432`，数据库文件和运行时都在 Git 仓库外。可用以下脚本查看状态、启动或停止；使用自己安装的数据库服务时按其方式启停即可。

```powershell
.\scripts\database.ps1 Status
.\scripts\database.ps1 Start
.\scripts\database.ps1 Stop
```

## 迁移与启动

```powershell
.\.venv\Scripts\python.exe -m alembic upgrade head
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

健康检查：`http://127.0.0.1:8000/health`；交互接口文档：`http://127.0.0.1:8000/docs`。后续迁移可用 `python -m alembic revision --autogenerate -m "describe change"` 创建，检查后再升级。

## 账号接口

| 接口 | 请求 | 成功响应 |
| --- | --- | --- |
| `GET /health` | 无 | `200`，服务与数据库状态 |
| `POST /auth/register` | JSON：`username`、`email`、`password` | `201`，用户公开资料 |
| `POST /auth/login` | JSON：`email`、`password` | `200`，`access_token`、`token_type`、`expires_in` |
| `GET /auth/me` | `Authorization: Bearer <access_token>` | `200`，当前账号公开资料 |

用户名去除首尾空白后为 2—20 个字符，不含内部空白，唯一性忽略大小写。邮箱规范为小写并校验格式，密码为 8—64 个字符且保留原始空格。确认密码在 App 表单校验，不发送给服务端。

密码使用 Argon2id 散列；公开响应不包含密码及其散列。注册只创建普通用户，不接受角色字段。令牌默认有效期 60 分钟，可通过 `.env` 调整；客户端在登录接口获得令牌后，可粘贴到接口文档的 Authorize 中测试 `/auth/me`。

错误状态：`422` 输入无效，`409` 账号重复，`401` 登录失败或令牌无效，`503` 数据库暂不可用。校验错误不会回显输入的密码。

## 校验

在 `.env` 配置 `LIUXUEBA_TEST_DATABASE_URL`，指向已创建、名称以 `_test` 结尾的独立 PostgreSQL 测试库：

```powershell
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m ruff check app tests alembic
.\.venv\Scripts\python.exe -m ruff format --check app tests alembic
.\.venv\Scripts\python.exe -m alembic check
```

数据库测试会迁移表结构、清理测试用户，并验证迁移回退/升级，必须使用专用测试库。未配置测试地址时，测试会失败并提示配置，不会自动改连应用数据库。

## Android 联调

App 已通过 `AuthRepository` 接入注册、登录和当前用户接口。Android 的 `domain/auth` 定义业务模型与接口，`data` 负责 HTTP 和响应转换，`ui/auth` 负责表单状态与交互，`di/AppContainer` 负责组装依赖。

先启动数据库和上面的 API 服务，再运行 Android App。模拟器默认使用 `http://10.0.2.2:8000/` 访问本机服务；注册成功后返回登录页并填入邮箱，登录成功后在“我的”显示账号资料。当前登录状态保留在内存中，退出登录会清除；关闭进程后恢复登录属于后续任务。

需要修改服务地址时，在仓库根目录构建：

```powershell
.\gradlew.bat :app:assembleDebug -PliuxuebaApiBaseUrl=http://YOUR_HOST:8000/
```

地址必须以 `/` 结尾。真机需要使用可访问的电脑局域网地址，并让服务监听对应网卡。Android 17 及以上的 Debug 构建在首次提交时会申请附近设备权限，允许后才能访问本地服务；拒绝后可在系统设置中开启。HTTP 明文访问和本地网络权限声明仅在 Debug 构建中开放；Release 构建应配置实际的 HTTPS 服务地址。

Android 单元测试不依赖后端。`AuthIntegrationTest` 则使用真实 API 和数据库，覆盖注册、登录、读取资料、退出登录和错误密码后重试；运行它前需确保上述服务可用。
