# 用户与权限管理系统

## How to Run

```bash
# 启动所有服务
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并清除数据
docker-compose down -v
```

## Services

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理后台 | http://localhost:8081 | 管理员管理用户 |
| 用户端 | http://localhost:8082 | 用户注册登录及个人中心 |
| 后端API | http://localhost:8080 | Spring Boot API服务 |
| MySQL | localhost:3307 | 数据库服务 |

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | user | user123 |

默认重置密码：123456

## 题目内容

### 4.1 用户与权限管理模块

#### 4.1.1 功能概述
负责系统用户的注册、登录、权限控制及个人信息维护。

#### 4.1.2 普通用户功能
- **用户注册** - 输入用户名、密码、确认密码、昵称，用户名唯一性校验
- **用户登录** - 输入用户名和密码，身份验证，登录状态保持
- **用户退出** - 清除登录状态
- **查看个人信息** - 显示当前用户的基本信息
- **修改个人信息** - 修改昵称、手机号、邮箱，数据有效性校验
- **修改密码** - 输入原密码、新密码、确认密码，原密码验证

#### 4.1.3 管理员功能
- **添加普通用户** - 输入用户名、初始密码、昵称，用户名唯一性校验
- **修改用户信息** - 修改用户的基本信息，可修改用户角色和状态
- **重置用户密码** - 将用户密码重置为默认值
- **用户状态管理** - 启用/禁用用户账号

---

## 技术栈

- **后端**: Spring Boot 3.2 + Spring Security + JWT + JPA
- **前端**: Vue 3 + Vite + Element Plus + Pinia
- **数据库**: MySQL 8.0
- **容器化**: Docker + Docker Compose

## 项目结构

```
├── backend/                 # Spring Boot 后端
├── frontend-admin/          # 管理后台 (Vue 3)
├── frontend-user/           # 用户端 (Vue 3)
├── docker-compose.yml       # Docker 编排
├── .gitignore
└── README.md
```

## API 接口

### 认证接口 (无需登录)
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 用户接口 (需登录)
- `GET /api/user/profile` - 获取个人信息
- `PUT /api/user/profile` - 修改个人信息
- `PUT /api/user/password` - 修改密码

### 管理员接口 (需管理员权限)
- `GET /api/admin/users` - 获取用户列表
- `GET /api/admin/users/{id}` - 获取用户详情
- `POST /api/admin/users` - 添加用户
- `PUT /api/admin/users/{id}` - 修改用户
- `POST /api/admin/users/{id}/reset-password` - 重置密码
- `POST /api/admin/users/{id}/toggle-status` - 切换状态
