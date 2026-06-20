# 用户与权限管理系统 — 代码审查与理解文档

> 本文档面向接手项目的工程师，基于对仓库全部代码的静态分析得出，所有结论均可回溯至具体代码位置。

---

## 一、整体架构概览

| 层级 | 技术栈 | 关键文件 |
|------|--------|----------|
| 后端鉴权服务 | Spring Boot 3 + Spring Security + JPA/Hibernate + MySQL + JWT (JJWT) | [backend/src/main/java/com/usermanagement](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement) |
| 管理后台前端 | Vue 3 + Pinia + Vue Router + Element Plus + Vite | [frontend-admin/src](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src) |
| 用户端前端 | Vue 3 + Pinia + Vue Router + Element Plus + Vite | [frontend-user/src](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src) |

### 数据库实体
核心实体为 [User.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/entity/User.java)，表名 `users`，字段：
- `id` (BIGINT, 自增主键)
- `username` (VARCHAR(50), 唯一, 非空)
- `password` (VARCHAR(255), BCrypt 哈希, 非空)
- `nickname` (VARCHAR(50))
- `phone` (VARCHAR(20))
- `email` (VARCHAR(100))
- `role` (VARCHAR, 枚举: USER / ADMIN, 默认 USER)
- `enabled` (BOOLEAN, 默认 true)
- `created_at` / `updated_at` (DATETIME, JPA 自动维护)

### 权限配置（后端）
见 [SecurityConfig.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java#L38-L49)：

| 路径模式 | 访问策略 |
|----------|----------|
| `/api/auth/**` | 完全公开 (`permitAll`) |
| `OPTIONS /**` | 完全公开 |
| `/api/admin/**` | 必须有 `ROLE_ADMIN` 权限 |
| 其他所有 `/api/**` | 必须已认证 (`authenticated`) |

### 默认初始化账号
见 [DataInitializer.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/DataInitializer.java#L18-L37)：
- 管理员：`admin` / `admin123`
- 普通用户：`user` / `user123`

---

## 二、核心链路完整分析

### 链路 1：用户注册

**前端入口**：用户端 [Register.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/views/Register.vue#L100-L108)

| 步骤 | 路径 | 代码位置 | 说明 |
|------|------|----------|------|
| 1 | 表单校验 | Register.vue#L82-L98 | 前端校验用户名长度 3-50、密码≥6位、两次密码一致 |
| 2 | Store 调用 | [user.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/stores/user.js#L32-L39) | `api.post('/auth/register', data)` |
| 3 | Axios 拦截器 | [api/index.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/api/index.js#L9-L15) | 此接口无 token，直接放行 |
| 4 | 后端控制器 | [AuthController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/AuthController.java#L16-L19) | `POST /api/auth/register` |
| 5 | DTO 校验 | [RegisterRequest.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/RegisterRequest.java) | `@NotBlank` + `@Size` 后端二次校验 |
| 6 | 服务层 | [UserService.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L26-L45) | ①校验两次密码一致 ②检查用户名唯一 ③密码用 BCrypt 加密 ④**强制 role=USER, enabled=true** ⑤保存并签发 JWT |
| 7 | 返回 | LoginResponse | 包含 JWT token 和 UserDTO，前端自动登录 |

**关键安全点**：
- 注册接口**不接受 role 和 enabled 字段**——RegisterRequest DTO 中根本没有这两个字段，即使前端在 JSON 中塞入也会被 Jackson 忽略。
- 服务层在 L38-L39 硬编码 `role=USER` 和 `enabled=true`，不存在注册即管理员的风险。
- 注册成功后立即返回 JWT，用户自动登录。

---

### 链路 2：用户登录

**前端入口**：用户端 [Login.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/views/Login.vue#L78-L90) 或管理后台 [Login.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Login.vue#L95-L107)

两个前端共用同一个登录接口 `/api/auth/login`，**后端不区分登录来源**。

| 步骤 | 路径 | 代码位置 | 说明 |
|------|------|----------|------|
| 1 | 控制器 | [AuthController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/AuthController.java#L21-L24) | `POST /api/auth/login` |
| 2 | 服务层 | [UserService.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L47-L61) | ①按用户名查询 ②BCrypt 密码比对 ③**检查 enabled 状态** ④生成 JWT |

**登录检查顺序**（L48-L57）：
1. 先查用户名是否存在 → 不存在抛 "用户名或密码错误"
2. 再比对密码 → 不匹配抛 "用户名或密码错误"
3. 最后检查 `enabled` → 禁用则抛 "账号已被禁用"

**JWT 生成**（[JwtUtil.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtUtil.java#L26-L34)）：
- Claims: `sub=username`, `role=USER/ADMIN`, `iat=签发时间`, `exp=签发时间+24h`
- 签名算法: HS256
- 密钥来源: `application.yml` 中 `jwt.secret`，默认值为硬编码字符串
- 有效期: 86400000ms = **24 小时**（见 [application.yml](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L21)）

---

### 链路 3：JWT 校验（每个受保护请求必经）

| 步骤 | 路径 | 代码位置 | 说明 |
|------|------|----------|------|
| 1 | Filter 入口 | [JwtAuthenticationFilter.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtAuthenticationFilter.java#L24-L44) | `OncePerRequestFilter`，在 `UsernamePasswordAuthenticationFilter` 之前执行 |
| 2 | Token 提取 | L26-L29 | 从 `Authorization: Bearer <token>` 提取 |
| 3 | 签名验证 | [JwtUtil.validateToken()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtUtil.java#L54-L64) | 仅验证签名和过期时间，捕获 JwtException 返回 false |
| 4 | 解析身份 | L31-L32 | 从 token 解出 username 和 role |
| 5 | 设置上下文 | L34-L39 | 构造 `UsernamePasswordAuthenticationToken`，权限为 `ROLE_{role}`，放入 `SecurityContextHolder` |
| 6 | URL 鉴权 | [SecurityConfig.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java#L43-L48) | 根据路径规则决定通过/拒绝 |

**⚠️ 关键设计缺陷（详见风险清单）**：
- JWT 校验**只验证签名和过期时间**，**不回查数据库**。这意味着：
  - 用户被禁用后，已签发的 JWT 在过期前仍然完全有效
  - 用户角色被管理员修改后，旧 JWT 仍然携带旧角色
  - 用户被删除后（当前系统无删除功能），JWT 仍然有效

---

### 链路 4：获取/修改个人资料

**前端入口**：[Profile.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/views/Profile.vue)（用户端）或 [Profile.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Profile.vue)（管理后台）

两个前端都调用同一个 `/api/user/profile` 接口。

#### 4.1 获取个人资料
| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `GET /api/user/profile` | Profile.vue#L171-L177 |
| 控制器 | [UserController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/UserController.java#L17-L20) |
| 身份来源 | `Authentication.getName()` — 即 JWT 中的 subject(username) |
| 服务层 | [UserService.getProfile()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L63-L67) |

#### 4.2 修改个人资料
| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `PUT /api/user/profile`，Body: `{ nickname, phone, email }` | Profile.vue#L179-L187 |
| DTO | [UpdateProfileRequest.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/UpdateProfileRequest.java) | 只有 nickname/phone/email 三个可写字段 |
| 控制器 | [UserController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/UserController.java#L22-L27) |
| 服务层 | [UserService.updateProfile()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L69-L86) | 只更新非 null 字段 |

**关键安全点**：
- 身份完全来自 JWT（`Authentication.getName()`），**接口不接受 userId 参数**，用户无法修改他人资料。
- UpdateProfileRequest 中**没有 role/enabled/username 字段**，普通用户无法通过此接口提权或修改用户名。
- 前端 Axios 拦截器自动附加 token（[api/index.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/api/index.js#L9-L15)）。

---

### 链路 5：修改密码

**前端入口**：Profile.vue "安全设置" Tab

| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `PUT /api/user/password`，Body: `{ oldPassword, newPassword, confirmPassword }` | Profile.vue#L189-L199 |
| DTO | [ChangePasswordRequest.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/ChangePasswordRequest.java) | 三个字段均 `@NotBlank`，新密码 `@Size(min=6)` |
| 控制器 | [UserController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/UserController.java#L29-L35) |
| 服务层 | [UserService.changePassword()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L88-L103) | ①校验两次一致 ②验证原密码 ③BCrypt 加密新密码 ④保存 |

**关键安全点**：
- 需要验证 `oldPassword`，防止 token 被劫持后直接改密码。
- **但密码修改成功后，不失效已有的 JWT token**——旧 token 在未来 24 小时内仍然有效。

---

### 链路 6：管理员新增用户

**前端入口**：管理后台 [Users.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L158-L162) → 弹出添加对话框 → [submitForm()](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L171-L187)

| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `POST /api/admin/users`，Body: `{ username, password?, nickname }` | Users.vue#L179 |
| DTO | [AdminCreateUserRequest.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/AdminCreateUserRequest.java) | username `@NotBlank`, password `@Size(min=6)` 但**无 `@NotBlank`**，nickname 可选 |
| 控制器 | [AdminController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/AdminController.java#L28-L31) |
| URL 鉴权 | SecurityConfig L46: `/api/admin/**` 要求 `ROLE_ADMIN` |
| 服务层 | [UserService.createUser()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L118-L134) | ①检查用户名唯一 ②password 为空则使用 `defaultPassword`(123456) ③**强制 role=USER** ④强制 enabled=true |

**关键安全点**：
- AdminCreateUserRequest **没有 role 字段**，管理员通过此接口创建的用户**只能是普通用户**，无法直接创建管理员。
- password 字段后端不是必填的（无 `@NotBlank`），若不传则使用默认密码 `123456`（配置于 [application.yml](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L24)）。但前端表单 L144 把 password 设为必填，所以前端会拦截空密码提交。
- 若有人绕过前端直接调 API，可以不传 password 创建密码为 123456 的用户。

---

### 链路 7：管理员编辑用户（含修改角色）

**前端入口**：Users.vue 表格行 "编辑" 按钮 → [showEditDialog()](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L164-L169) → 编辑对话框含角色下拉（L102-L107）

| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `PUT /api/admin/users/{id}`，Body: `{ nickname, phone?, email?, role?, enabled? }` | Users.vue#L176 |
| DTO | [AdminUpdateUserRequest.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/AdminUpdateUserRequest.java) | 包含 role(String) 和 enabled(Boolean)，均无校验注解 |
| 服务层 | [UserService.updateUser()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L136-L159) | 对非 null 字段逐一更新，**role 通过 `User.Role.valueOf(request.getRole())` 直接转换** |

**关键安全点**：
- 这是**唯一能修改用户角色和启停状态的接口**。
- role 字段**没有白名单校验**，直接使用 `Enum.valueOf()`。如果传入 `"INVALID"` 等非法值，会抛出 `IllegalArgumentException`，被 GlobalExceptionHandler 兜底为 500 错误而非 400。
- 没有检查"管理员是否在修改自己的角色"——管理员可以把自己降级为 USER，降级后虽然 JWT 内仍为 ADMIN 直到过期，但前端刷新后会从 `/api/user/profile` 获取最新 role 为 USER。

---

### 链路 8：管理员重置用户密码

**前端入口**：Users.vue "重置密码" 按钮 → [resetPassword()](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L189-L197)

| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 弹窗确认 | ElMessageBox.confirm | L190 提示"密码将被重置为 123456" |
| 请求 | `POST /api/admin/users/{id}/reset-password` | L195 |
| 控制器 | [AdminController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/AdminController.java#L40-L44) |
| 服务层 | [UserService.resetPassword()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L161-L167) | 直接设置密码为 BCrypt(defaultPassword) 即 `123456` |

**关键安全点**：
- 重置后的固定默认密码 `123456` 硬编码于配置文件。
- 重置密码**不通知用户**，不强制下次登录修改，不失效现有 JWT。
- 没有检查目标用户是否是管理员自己（理论上可以重置自己的密码，但不需要原密码）。

---

### 链路 9：管理员启停用户账号

**前端入口**：Users.vue "禁用/启用" 按钮 → [toggleStatus()](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L199-L203)

| 步骤 | 路径 | 代码位置 |
|------|------|----------|
| 请求 | `POST /api/admin/users/{id}/toggle-status` | L200 |
| 控制器 | [AdminController.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/AdminController.java#L46-L49) |
| 服务层 | [UserService.toggleUserStatus()](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L169-L176) | `user.setEnabled(!user.getEnabled())`，翻转状态 |

**关键安全点**：
- 启停操作只修改数据库中的 `enabled` 字段。
- **已登录用户的 JWT 不会被失效**——禁用后持有有效 JWT 的用户仍可访问所有接口长达 24 小时。
- 只有在用户重新登录时，[UserService.login() L55-L57](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L55-L57) 才会检查 `enabled`。
- 没有检查是否在禁用自己。

---

## 三、接口权限依赖关系汇总

| 接口 | 方法 | 认证要求 | 角色要求 | 身份来源 |
|------|------|----------|----------|----------|
| `/api/auth/register` | POST | 无 | 无 | - |
| `/api/auth/login` | POST | 无 | 无 | - |
| `/api/user/profile` | GET | 已认证 | USER/ADMIN 均可 | JWT subject |
| `/api/user/profile` | PUT | 已认证 | USER/ADMIN 均可 | JWT subject |
| `/api/user/password` | PUT | 已认证 | USER/ADMIN 均可 | JWT subject（需原密码） |
| `/api/admin/users` | GET | 已认证 | **ADMIN** | - |
| `/api/admin/users/{id}` | GET | 已认证 | **ADMIN** | - |
| `/api/admin/users` | POST | 已认证 | **ADMIN** | - |
| `/api/admin/users/{id}` | PUT | 已认证 | **ADMIN** | - |
| `/api/admin/users/{id}/reset-password` | POST | 已认证 | **ADMIN** | - |
| `/api/admin/users/{id}/toggle-status` | POST | 已认证 | **ADMIN** | - |

### 前端不信任字段清单
以下字段如果由前端传入，后端通过 DTO 设计直接忽略（不在 DTO 中定义）：

| DTO | 未包含的字段 | 说明 |
|-----|-------------|------|
| RegisterRequest | role, enabled, id | 注册无法指定角色或状态 |
| LoginRequest | role, enabled | 登录无法指定身份 |
| UpdateProfileRequest | username, role, enabled, password | 用户无法改自己的用户名/角色/状态 |
| ChangePasswordRequest | username, role | 改密码时身份来自 JWT |
| AdminCreateUserRequest | role, enabled | 创建用户无法直接指定为 ADMIN |
| AdminUpdateUserRequest | username, password | 编辑用户不能改用户名（密码走重置接口） |

### 前端可传入但需特别注意的字段
- **AdminUpdateUserRequest.role**：无后端校验注解，直接 `Enum.valueOf()`，非法值导致 500。
- **AdminUpdateUserRequest.enabled**：无校验，可以任意设置。

---

## 四、风险清单（按优先级排序）

### 🔴 高危

#### 风险 1：账号禁用后现有会话仍完全有效
- **代码依据**：[JwtAuthenticationFilter.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtAuthenticationFilter.java#L30-L40) 在验证 token 时只调 `jwtUtil.validateToken(token)`，该方法（[JwtUtil.java#L54-L64](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtUtil.java#L54-L64)）只验签名和过期，**不查数据库**。而 `enabled` 检查仅在 [UserService.login() L55-L57](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L55-L57) 登录时执行。
- **影响**：管理员禁用某用户后，该用户若已有有效 JWT，在接下来 24 小时内仍可正常访问所有接口（获取资料、修改资料、修改密码等），直到 JWT 自然过期。
- **同理**：管理员修改用户角色后（如 ADMIN → USER），旧 JWT 内的 role claim 不变，用户在 JWT 过期前仍保有原权限。

#### 风险 2：密码修改/重置后旧 JWT 不失效
- **代码依据**：[UserService.changePassword() L88-L103](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L88-L103) 和 [resetPassword() L161-L167](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L161-L167) 在更新密码后只保存实体，没有任何 token 黑名单或版本号机制。
- **影响**：如果用户的 JWT 泄露（如 XSS、日志泄露），即使用户修改了密码，攻击者持有的旧 JWT 在过期前仍可正常使用。管理员重置某用户密码后，该用户的旧 JWT 也仍然有效。

### 🟠 中危

#### 风险 3：默认密码硬编码为 `123456`，且重置后无强制修改机制
- **代码依据**：[application.yml L24](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L24) `app.default-password: 123456`。[UserService.createUser() L126](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L126) 和 [resetPassword() L165](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L165) 均使用此值。系统中无 `must_change_password` 字段，也无任何下次登录强制改密的逻辑。
- **影响**：管理员批量重置密码后，如果忘记通知对应用户，任何人知道默认密码规则即可尝试登录；同一默认密码被反复使用。

#### 风险 4：`AdminUpdateUserRequest.role` 无合法值校验，非法输入触发 500
- **代码依据**：[AdminUpdateUserRequest.java L20](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/AdminUpdateUserRequest.java#L20) 的 `role` 字段是无约束的 String。[UserService.updateUser() L151](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L151) 直接调用 `User.Role.valueOf(request.getRole())`。
- **影响**：传入 role="SUPERADMIN" 或任意非法字符串会抛出 `IllegalArgumentException`，被 [GlobalExceptionHandler L35-L39](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/exception/GlobalExceptionHandler.java#L35-L39) 的通用 Exception handler 捕获，返回 500 而非 400。这会：1) 产生 5xx 告警/日志；2) 向客户端暴露堆栈信息的可能性（取决于 Spring 配置）。

#### 风险 5：管理员可以禁用/降级自己，无防护
- **代码依据**：[UserService.toggleUserStatus() L170-L176](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L170-L176) 和 [updateUser() L137-L159](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L137-L159) 均不检查操作目标是否为当前登录用户。
- **影响**：管理员操作失误可能把自己禁用或降级为普通用户，导致系统中至少在当前会话期间失去管理员（直到 JWT 过期或其他管理员介入）。

#### 风险 6：登录时"账号已被禁用"提示泄露账号存在性
- **代码依据**：[UserService.login() L49-L57](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L49-L57) 用户名存在时先检查密码，如果密码错误返回"用户名或密码错误"（不区分），但如果密码正确且账号禁用返回"账号已被禁用"。
- **影响**：攻击者可以通过枚举用户名 + 任意密码尝试：如果返回"账号已被禁用"说明该用户名存在且曾经设置过正确密码，这构成用户枚举漏洞。

### 🟡 低危

#### 风险 7：JWT 密钥有硬编码默认值
- **代码依据**：[application.yml L20](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L20) `jwt.secret: ${JWT_SECRET:mySecretKeyForJWTTokenGenerationThatIsLongEnoughForHS256Algorithm}`，虽然支持环境变量覆盖，但默认值是一个公开的固定字符串。
- **影响**：如果部署时忘记设置 `JWT_SECRET` 环境变量，任何人都可以用该默认密钥伪造任意身份的 JWT（包括 ADMIN）。

#### 风险 8：CORS 配置允许任意来源
- **代码依据**：[SecurityConfig.java L57](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java#L57) `configuration.setAllowedOriginPatterns(List.of("*"))`，同时 L60 `setAllowCredentials(true)`。
- **影响**：任意网站都可以向该 API 发跨域请求并携带 cookie（虽然本系统主要用 Authorization header），但配合其他漏洞（如 XSS）可能扩大攻击面。

#### 风险 9：前端路由守卫可被篡改
- **代码依据**：[frontend-admin router/index.js L39](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/router/index.js#L39) 检查 `userStore.user?.role !== 'ADMIN'`，但 user 对象来自 localStorage，用户可通过浏览器控制台手动修改 `localStorage.user` 中的 role 字段。
- **影响**：仅前端 UI 层面的防护可以被绕过，普通用户可以看到管理后台的用户列表页面。但实际数据请求会被后端 `ROLE_ADMIN` 鉴权拦截，返回 403，所以数据层面是安全的。这是防御深度问题。

#### 风险 10：异常处理使用通用 RuntimeException，错误码粒度粗
- **代码依据**：[GlobalExceptionHandler.java L15-L19](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/exception/GlobalExceptionHandler.java#L15-L19) 所有 `RuntimeException` 都返回 400，前端无法通过 HTTP 状态码区分"用户名已存在""原密码错误""账号被禁用"等不同类型的业务错误，只能依赖 message 字符串做判断。
- **影响**：前后端错误提示一致性方面，如果将来需要做国际化或根据错误类型做不同 UI 处理，需要重构异常体系。

#### 风险 11：JWT 存储于 localStorage，无 XSS 防护
- **代码依据**：两个前端都将 JWT 存储在 `localStorage`（[user.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/stores/user.js#L12)），一旦发生 XSS 漏洞，攻击者可以轻易窃取 token。
- **影响**：这是 SPA 的常见权衡（相比 cookie 可避免 CSRF），但需要确保前端无 XSS 注入点。

---

## 五、最需要优先验证的测试场景

以下场景按验证优先级排序，建议在接手后第一时间执行确认：

### P0（必须立即验证）

1. **禁用后 token 有效性**：
   - 用 user/user123 登录，获取 token。
   - 用 admin 将 user 禁用。
   - 用之前获取的 token 请求 `GET /api/user/profile`，**预期应被拒绝，但当前代码会返回 200**。验证此问题是否存在。

2. **修改密码后旧 token 有效性**：
   - 登录获取 token A。
   - 修改密码。
   - 用 token A 请求 `GET /api/user/profile`，**预期应失效，但当前代码会返回 200**。

3. **角色变更后旧 token 权限**：
   - 用 admin 账号登录获取 admin token。
   - 用该 admin token 将另一个普通用户提升为 ADMIN。
   - 用该普通用户的旧 token（角色变更前签发）请求 `/api/admin/users`，**预期应该成功（因用户已是 ADMIN）但旧 token 内 role 仍是 USER，会返回 403**；反向测试（降级）同理，旧 ADMIN token 仍可访问管理员接口。

4. **默认 JWT 密钥是否在生产环境被覆盖**：
   - 检查部署配置中是否设置了 `JWT_SECRET` 环境变量；如果没有，意味着所有人都可以伪造 ADMIN token。

### P1（高优先级）

5. **管理员自禁用/自降级**：
   - 用 admin 登录，调用 `POST /api/admin/users/{adminId}/toggle-status` 禁用自己，验证是否被允许。
   - 调用 `PUT /api/admin/users/{adminId}` 将自己 role 改为 USER，验证后果。

6. **重置密码后是否可以用 123456 登录**：
   - 重置某用户密码后，验证该用户的旧 token 是否仍有效（配合场景 2）。
   - 验证 123456 可以成功登录。

7. **非法 role 值的 API 行为**：
   - 直接用 HTTP 工具向 `PUT /api/admin/users/{id}` 发送 `{"role":"INVALID"}`，观察返回是 400 还是 500。

8. **绕过前端创建无密码用户**：
   - 直接向 `POST /api/admin/users` 发送 `{"username":"test1"}`（不传 password），验证是否成功创建、密码是否为 123456。

### P2（建议验证）

9. **CORS 跨域行为**：从任意第三方域名发起请求，验证 CORS 策略是否符合预期。
10. **前端篡改 localStorage role 后访问 /users 页面**：验证后端是否正确返回 403（前端虽然能看到页面但无数据）。
11. **密码重置后是否需要强制修改首次登录密码**：验证当前无此机制。
12. **登录时账号被禁用的错误提示是否区分于密码错误**：验证 "账号已被禁用" 是否在密码正确时返回。

---

## 六、普通用户 vs 管理员路径对比总结

| 操作 | 普通用户（用户端） | 管理员（管理后台） | 普通用户是否可能通过越权访问管理员功能？ |
|------|-------------------|-------------------|------------------------------------------|
| 注册 | ✅ /api/auth/register | ❌ 无入口 | 注册只能产生 USER，无法注册 ADMIN |
| 登录 | ✅ /api/auth/login | ✅ 同一接口 | 后端不区分登录端，admin 可在用户端登录（但用户端无管理页面） |
| 查看个人资料 | ✅ /api/user/profile | ✅ /api/user/profile | 只能看自己，身份来自 JWT |
| 修改个人资料 | ✅ PUT /api/user/profile | ✅ PUT /api/user/profile | 只能改自己，DTO 无 role/enabled |
| 修改密码 | ✅ PUT /api/user/password | ✅ PUT /api/user/password | 需要原密码 |
| 查看所有用户 | ❌ 前端无入口 | ✅ GET /api/admin/users | 后端 `hasRole('ADMIN')` 拦截，403 |
| 新增用户 | ❌ 前端无入口 | ✅ POST /api/admin/users | 后端 ADMIN 拦截；新增用户强制 role=USER |
| 编辑用户/角色 | ❌ | ✅ PUT /api/admin/users/{id} | 后端 ADMIN 拦截；这是唯一改角色的入口 |
| 重置密码 | ❌ | ✅ POST /api/admin/users/{id}/reset-password | 后端 ADMIN 拦截 |
| 启停账号 | ❌ | ✅ POST /api/admin/users/{id}/toggle-status | 后端 ADMIN 拦截；**但 JWT 校验不查 enabled 状态** |

---

> 文档结论均基于当前仓库代码静态分析，未包含运行时验证。建议在实际环境中按第五章的 P0 场景逐项确认，并在修复高风险问题后再投入生产使用。
