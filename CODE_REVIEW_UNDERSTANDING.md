# 用户与权限管理系统 — 代码审查理解文档

> 本文档面向接手该项目的工程师，目的是把现有代码完整串起来，澄清前端、网关、后端与数据库之间的真实数据流，并标注审查中发现的、有具体代码依据的潜在风险。所有结论均可在仓库内对应文件、对应行号找到原文。
>
> 范围：`backend/`（Spring Boot + Spring Security + JPA + JWT），`frontend-admin/` 与 `frontend-user/`（Vue3 + Pinia + Element Plus + Vite + Nginx）。

---

## 一、整体架构概览

### 1.1 部署拓扑

`docker-compose.yml` 一共拉起 4 个容器：

- `mysql`（MySQL 8.0，仅 expose 3306，不对外发布端口）
- `backend`（Spring Boot 3.x，对外 `8080:8080`）
- `frontend-admin`（Nginx 提供静态站点，`8081:80`）
- `frontend-user`（Nginx 提供静态站点，`8082:80`）

两个前端是相互独立的 SPA，分别构建、独立镜像。它们都通过 axios 直接调用 `http://localhost:8080/api`（构建参数 `VITE_API_URL` 为空时回退到默认值，见 [api/index.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/api/index.js#L4-L7) 与 [frontend-user/api/index.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/api/index.js#L4-L7)）。注意：**后端没有任何模块级路由区分“管理员前端”和“用户前端”**——所有访问控制完全靠 JWT 中的 role + Spring Security 的 URL 模式实现。

### 1.2 后端关键组件

- 入口：[BackendApplication.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/BackendApplication.java)
- 安全配置：[SecurityConfig.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java)
- JWT 工具：[JwtUtil.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtUtil.java) + 过滤器 [JwtAuthenticationFilter.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtAuthenticationFilter.java)
- 控制器：`AuthController`（注册/登录）、`UserController`（当前登录人个人资料/改密）、`AdminController`（后台管理）
- 服务层：唯一的 [UserService.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java)，所有业务都在这里
- 实体：单表 [User.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/entity/User.java)，role 为 `USER | ADMIN` 枚举
- 仓库：[UserRepository.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/repository/UserRepository.java)
- 异常：[GlobalExceptionHandler.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/exception/GlobalExceptionHandler.java) 把所有 `RuntimeException` 折成 HTTP 400 + 业务消息
- 初始数据：[DataInitializer.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/DataInitializer.java) 启动时自动建 `admin/admin123` 与 `user/user123`

### 1.3 鉴权的“总闸”

[SecurityConfig.filterChain](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java#L38-L52) 的访问规则极简：

```
/api/auth/**            -> permitAll
OPTIONS /**             -> permitAll
/api/admin/**           -> hasRole("ADMIN")
其它                    -> authenticated
```

CSRF 关闭、Session STATELESS、没有自定义 `AuthenticationEntryPoint`／`AccessDeniedHandler`，未授权时由 Spring Security 默认行为返回 401/403，**响应体不会经过 `GlobalExceptionHandler`，因此和业务接口的 `ApiResponse{code,message,data}` 结构不一致**（前端拦截器仅依赖 `error.response?.data?.message`，缺失时回落到 “请求失败”）。

JWT 过滤器在每次请求前检查 `Authorization: Bearer <token>`，若 `validateToken` 通过，就直接用 token 中的 `subject`（用户名）和 `role` claim 构造一个 `UsernamePasswordAuthenticationToken` 并注入 `SecurityContext`。**整条链路不再回查数据库**。

---

## 二、关键链路逐条拆解

下面每条链路按 “前端页面 → 前端 store/api → HTTP → 后端 Controller → Service → Repository/JPA” 串联，并注明谁依赖当前登录身份、谁是管理员专属、哪些字段“前端可传但后端不该信任”。

### 2.1 注册（仅普通用户端有入口）

- 前端页面：[frontend-user Register.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/views/Register.vue)（管理员端没有注册路由）
- 前端 store：`useUserStore.register` → `POST /auth/register`，并在前端再次校验 `confirmPassword === password`
- HTTP：`POST /api/auth/register`，请求体见 [RegisterRequest](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/RegisterRequest.java)（`username/password/confirmPassword/nickname`）
- 后端：`AuthController.register` → `UserService.register`（[UserService.java:26-45](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L26-L45)）
  - 校验两次密码、用户名唯一
  - **强制** `setRole(USER)`、`setEnabled(true)`，不接受前端传入的 role/enabled 字段（DTO 里也没有这两个字段，安全）
  - BCrypt 加密密码后入库，紧接着颁发 JWT 直接登录

> 后端不信任 / 不接收：`role`、`enabled`、`phone`、`email`、`id`、`createdAt` 等。

### 2.2 登录

- 前端页面：管理端 [Login.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Login.vue) / 用户端同名页面
- 前端 store：`POST /auth/login`，成功后把 `{token, user}` 写入 Pinia + `localStorage`
- 后端：`AuthController.login` → `UserService.login`（[UserService.java:47-61](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L47-L61)）
  - 找不到用户、密码错都返回同一句 “用户名或密码错误”（不区分，OK）
  - **`enabled=false` 仅在登录时被检查**，即“账号已被禁用”
  - 登录成功后 `JwtUtil.generateToken(username, role.name())`，过期 24 小时（`jwt.expiration=86400000`，[application.yml:21](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L19-L21)）
- 前端拦截：401 时清空本地 token 并跳 `/login`（[api/index.js:22-26](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/api/index.js#L17-L29)）

> 注意：管理端 `Login.vue` 直接在页面上显示 “测试账号: admin / admin123”，配合 `DataInitializer` 创建的固定弱口令，对生产风险很大（详见第三章）。

### 2.3 JWT 校验（每个受保护请求）

- 入口：[JwtAuthenticationFilter.doFilterInternal](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtAuthenticationFilter.java#L23-L44)
  - 仅当 header 以 `Bearer ` 开头才处理；解析失败/异常一律静默 fall-through，`SecurityContext` 保持空，由后续 `authorizeHttpRequests` 判定 401/403。
  - 注入的 `Authentication.principal` 是 token 的 `subject`（用户名字符串），权限是 `ROLE_<role>`
- `Authentication authentication` 在 `UserController` 各方法签名中由 Spring 自动注入（[UserController.java:18,24,31](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/UserController.java#L17-L36)），从而决定“当前登录人”是谁，避免前端传 `userId`。
- `UserService.getProfile/updateProfile/changePassword` 接收的是 `username`，再走 `findByUsername` 取实体，**id 永远不来自请求体**——这是该系统最重要的服务端身份信任源。

> 重要：JWT 内既有 `subject=username` 也有 `role`。**校验通过后再没有任何步骤回查数据库的 `enabled/role`**。也就是说：管理员把某用户禁用、或把某 USER 提到 ADMIN，**已签发的旧 token 在过期前依然按旧身份生效**。这是后续风险清单的核心一条。

### 2.4 修改个人资料（普通用户和管理员都可用）

- 前端：`Profile.vue`（管理端/用户端各一个，结构相同）
  - 用户名输入框 `disabled`，仅展示
  - 提交字段：`{nickname, phone, email}` → `PUT /user/profile`
- 后端：[UserController.updateProfile](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/controller/UserController.java#L22-L27) → [UserService.updateProfile](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L69-L86)
  - 仅当字段 `!= null` 才覆盖。**未把空字符串当作“清空”处理**——前端清空 phone 提交空串时，后端会用空串覆盖（这是符合预期的）。
  - DTO [UpdateProfileRequest](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/UpdateProfileRequest.java) 仅含 `nickname/phone/email`，因此即便前端 hack 加上 `role/enabled/username/id/password` 字段，后端也根本不会绑定到字段——这一层是安全的。

> 后端不信任 / 不接收：`username`、`role`、`enabled`、`id`、`password`。
> 当前登录身份来源：`Authentication.getName()`（即 JWT 的 subject）。

### 2.5 修改自己的密码

- 前端：`Profile.vue` 安全设置 Tab → `PUT /user/password` body `{oldPassword,newPassword,confirmPassword}`
- 后端：[UserService.changePassword](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L88-L103)
  - 校验两次新密码一致、校验旧密码 BCrypt 比对、写入新 hash
  - **不会让现有 JWT 失效**，前端也没主动清 token——改完密码原 token 仍可继续访问
- 异常文案：旧密码错误返回 “原密码错误”；前端 `api` 响应拦截器统一 `ElMessage.error(message)` 弹出（[api/index.js:17-29](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/api/index.js#L17-L29)）

### 2.6 管理员新增用户

- 前端入口：[Users.vue](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue) “添加用户” → 弹窗只暴露 `username/password/nickname`，不让选择角色（角色选择仅在“编辑”弹窗里出现，[Users.vue:102-107](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L102-L107)）
- 提交：`POST /admin/users` body 整个 `form` 对象（`{username, password, nickname, phone, email, role}`）。**前端把 `role` 也一起发了**，但是：
- 后端 DTO [AdminCreateUserRequest](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/AdminCreateUserRequest.java) 仅有 `username/password/nickname`，**没有 role/phone/email/enabled**——多余字段会被 Jackson 默认忽略
- 服务端 [UserService.createUser](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L118-L134)
  - 用户名唯一校验
  - `password` 可空，为空则使用 `app.default-password=123456`（[application.yml:24](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L23-L24)）
  - **强制写死** `setRole(USER)`、`setEnabled(true)`——管理员新增时**无法**直接创建另一个管理员（合理但需注意：要把新用户提为管理员必须走“编辑”，详见 2.7）

> 接口归属：`/api/admin/**`，已被 `hasRole("ADMIN")` 拦截。
> 前端可传但后端不信任：`role`、`phone`、`email`、`enabled`、`id`。

### 2.7 管理员编辑用户（含修改角色 / 启停）

- 前端：`Users.vue` 编辑弹窗一次提交 `{nickname, phone, email, role}`（启停用单独按钮，见 2.9）
- 接口：`PUT /admin/users/{id}` → [UserService.updateUser](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L136-L159)
- DTO [AdminUpdateUserRequest](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/dto/AdminUpdateUserRequest.java) 字段：`nickname/phone/email/role/enabled`
- 关键安全点：
  - `role` 字段类型是 `String`，**没有任何校验**（`@Pattern` 也未加）。`User.Role.valueOf(request.getRole())` 仅在传入合法枚举时通过，否则抛 `IllegalArgumentException` → 走 `Exception` 兜底返回 500（不是 400，体验不一致）
  - `enabled` 通过该接口可被修改（与 `toggle-status` 重复路径）
  - **没有任何防自残机制**：管理员可以把自己 `role=USER`、或把自己 `enabled=false`，也可以把另一个 ADMIN 降级——而且前端 `Users.vue` 的列表与编辑入口不会区分“当前登录人”

### 2.8 管理员重置密码

- 前端：`Users.vue.resetPassword` → `POST /admin/users/{id}/reset-password`，并在弹窗里**直接告诉操作者** “密码将被重置为 123456”（[Users.vue:190](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L189-L197)）
- 后端：[UserService.resetPassword](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L161-L167) 直接把 `password` 设为 `passwordEncoder.encode(defaultPassword)`，`defaultPassword` 来自 `app.default-password`
- 默认值是 **`123456`** 且未做任何 “首次登录强制改密” 的标志位（实体里没有 `passwordChanged/forceChangePwd` 字段）
- 重置后**不会让被重置用户的现有 JWT 失效**，被重置用户在 token 过期前仍可正常访问

### 2.9 管理员启停账号

- 前端：`Users.vue.toggleStatus` → `POST /admin/users/{id}/toggle-status`
- 后端：[UserService.toggleUserStatus](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L169-L176)：直接 `setEnabled(!enabled)` 写回
- 注意：
  - 没有 “不能禁用自己”、“不能禁用最后一个 ADMIN” 的保护
  - 禁用是基于 `id`，不会同步剔除已签发的 JWT；被禁用账号在 token 过期之前依然能访问 `/api/user/**` 和 `/api/admin/**`（如果它原本是管理员）
  - 仅 `UserService.login` 在签发新 token 时会读取 `enabled` 来阻止登录

---

## 三、权限边界总结

| 接口 | 谁能调用 | 是否依赖当前登录身份 | 备注 |
| --- | --- | --- | --- |
| `POST /api/auth/register` | 任何人 | 否 | 只能创建 USER |
| `POST /api/auth/login` | 任何人 | 否 | 仅此处校验 `enabled` |
| `GET  /api/user/profile` | 任意已登录 | 是（JWT subject） | |
| `PUT  /api/user/profile` | 任意已登录 | 是 | 不能改 username/role/enabled/password |
| `PUT  /api/user/password` | 任意已登录 | 是 | 需提供旧密码 |
| `GET  /api/admin/users` | ADMIN | 否（仅看角色） | |
| `GET  /api/admin/users/{id}` | ADMIN | 否 | |
| `POST /api/admin/users` | ADMIN | 否 | 强制 role=USER |
| `PUT  /api/admin/users/{id}` | ADMIN | 否 | 可改 role/enabled，可作用在自己身上 |
| `POST /api/admin/users/{id}/reset-password` | ADMIN | 否 | 重置为 `123456` |
| `POST /api/admin/users/{id}/toggle-status` | ADMIN | 否 | 可禁用自己 |

---

## 四、风险清单（按优先级）

> 每条都给出具体代码位置与可观测后果，便于优先验证。

### P0 — 生产环境必须修复

1. **JWT 与账号状态/角色解耦，禁用与降权对已签发 token 不生效**  
   过滤器只校验签名与过期时间（[JwtAuthenticationFilter.java:30-41](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/security/JwtAuthenticationFilter.java#L23-L44)），不会回查 `enabled` 与 `role`。后果：
   - 管理员把 ADMIN 降级为 USER 后，对方仍能继续访问 `/api/admin/**` 直到 token 过期（24 小时）。
   - 管理员禁用账号后，对方仍能继续访问所有受保护接口。  
   优先验证：见 §5 场景 ①。

2. **管理员可对自己降权或禁用，且无最后一个管理员保护**  
   [UserService.updateUser](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L136-L159) 与 [toggleUserStatus](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L169-L176) 没有任何保护：可能把唯一管理员降为 USER 或禁用，从此**永远没有人能管理用户表**（`DataInitializer` 只在 admin 完全不存在时再造，被降级不会被重建）。

3. **默认账号 `admin/admin123`、`user/user123` 强写入**  
   [DataInitializer.java](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/DataInitializer.java) 在每次启动时都会检查并按需写入；默认密码硬编码且登录页（[admin Login.vue:69](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Login.vue#L69)）直接展示。配合 P0-1 的 token 永久信任，攻击面非常大。

4. **JWT 密钥默认值很弱且写在 application.yml 中**  
   [application.yml:20](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L19-L21) 的默认 `JWT_SECRET=mySecretKeyForJWTTokenGenerationThatIsLongEnoughForHS256Algorithm`，docker-compose 也没有覆盖该变量。任何拿到镜像/源码的人都能伪造任意身份的 token（包括 `role=ADMIN`）。

### P1 — 严重逻辑/安全缺陷

5. **重置密码默认值 `123456`，且无强制首登改密**  
   [application.yml:24](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L23-L24) + [UserService.resetPassword](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L161-L167)。User 实体没有 `mustChangePassword` 字段，重置后用户可以一直用 `123456`。

6. **管理员修改用户角色没有白名单/枚举校验，错误信息混乱**  
   [UserService.updateUser:151](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L150-L152) 直接 `User.Role.valueOf(request.getRole())`，传入非法值（如 `"SUPER_ADMIN"`、小写 `"admin"`）抛 `IllegalArgumentException`，被 `GlobalExceptionHandler.handleException` 捕获返回 500 + “服务器内部错误”，前端只显示 “服务器内部错误”，与正常业务的 400 提示不一致。

7. **修改密码、被重置密码后，旧 token 仍然有效**  
   `changePassword`、`resetPassword`、`toggleUserStatus`、`updateUser(role/enabled)` 全部不会让旧 token 失效（无黑名单、无 `passwordChangedAt` 与 token 签发时间比对）。安全敏感操作完成后用户处于 “密码已变 / 角色已变，但 token 仍按旧值生效” 的状态。

8. **CORS 过宽 + `allowCredentials=true` + `allowedOriginPatterns("*")`**  
   [SecurityConfig:56-63](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/config/SecurityConfig.java#L54-L64)。Spring 允许这种组合，但等价于把任意源 + Cookie 信任打开；目前因为 token 走 `Authorization` header 而非 Cookie，影响有限，但若将来加 Cookie 认证会立即变成 CSRF 风险；同时 `*` 也意味着任意第三方页面都可以发起带 token 的浏览器请求（前提是 token 在它能拿到的存储里——目前是 `localStorage`，不存在自动携带，但仍属配置不规范）。

9. **`ddl-auto: update` 直接连生产库**  
   [application.yml:11-12](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/resources/application.yml#L10-L13)。改实体字段时数据库结构会自动 ALTER，存在生产数据风险。

### P2 — 一致性 / 体验 / 健壮性

10. **业务异常一律 `RuntimeException`，被 `GlobalExceptionHandler` 折成 HTTP 400**  
    [GlobalExceptionHandler.java:15-19](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/exception/GlobalExceptionHandler.java#L12-L20)。
    - “用户名或密码错误” 也是 400，前端拦截器看到 400 不会清 token，看到 401 才会清。一致性可接受但语义错。
    - 由于 JWT 失效/缺失走的是 Spring Security 默认 401（**不会被该 Handler 接管**），返回的不是 `ApiResponse` 结构，前端 `error.response?.data?.message` 取不到，会兜底 “请求失败”。

11. **前端 `register` 注册成功后立即把后端返回的 `user` 写本地，但 store 信任的角色信息也来自这里**  
    [frontend-user/stores/user.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-user/src/stores/user.js) / [frontend-admin/stores/user.js](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/stores/user.js)：路由守卫只看 `userStore.user.role`（[admin router:39](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/router/index.js#L35-L44)），而 `localStorage.user` 是用户可改的——只能影响前端 UI 是否显示 “用户管理” 菜单，不能突破后端权限，但仍属“**前端展示不能作为权限依据**”。审查时应明确提醒。

12. **管理员前端的 `Users.vue` 提交编辑表单时整体携带 form**  
    [Users.vue:176](file:///Users/huwenjie/项目/gsb/gsb-01727/frontend-admin/src/views/Users.vue#L171-L187)，意味着即便 `enabled` 不在表单里，未来若加字段会“顺路”被 PUT 到后端；此外编辑表单**没有 `enabled` 控件**，依靠列表的 “启用/禁用” 按钮单独触发 `toggle-status`，但服务端 `updateUser` 也接受 `enabled`，存在两条路径修改同一字段。

13. **`UpdateProfileRequest.phone` 的正则把空串视作合法 (`^…$|^$`)，但服务端是 `if (request.getPhone() != null) user.setPhone(...)`**  
    意味着前端传空串等于“清空电话”，传 `null`/不传等于“保持不变”——这两种语义在 RESTful PUT 里同名容易混淆，建议在审查时和前端确认前端的清空操作走哪种。

14. **未做密码强度策略**  
    仅 `@Size(min=6)`，无大小写/数字/特殊字符要求；对管理员重置后的弱密码尤其敏感。

15. **没有任何分页/搜索**  
    `GET /admin/users` 直接 `findAll()`（[UserService.java:106-110](file:///Users/huwenjie/项目/gsb/gsb-01727/backend/src/main/java/com/usermanagement/service/UserService.java#L106-L110)），用户量大时会出问题。

16. **没有审计日志**  
    重置密码、启停账号、改角色都没有记录“谁在什么时候对谁做了什么”。

17. **`User.Role.valueOf` 对大小写敏感，前端硬编码 `'USER' / 'ADMIN'` 没问题，但任何调用 `/api/admin/users/{id}` 时带不同大小写都会 500**（与 #6 同源）。

---

## 五、最需要优先验证的场景

下面是按风险×可复现性排序的验证清单，建议依此跑一遍冒烟即可暴露绝大多数问题。

1. **被禁用 / 被降权用户的 token 是否仍生效（验证 P0-1，P0-2）**
   - 管理员账号 A 登录 → 拿到 tokenA。
   - 用另一个浏览器以管理员 B 把 A 的 `enabled` 设为 false（或把 role 改为 USER）。
   - 用 tokenA 继续请求 `GET /api/admin/users`、`GET /api/user/profile`，**预期** 失败但实际仍 200。

2. **管理员把自己降级 / 禁用，是否系统不可恢复（验证 P0-2）**
   - 仅留一个 ADMIN 时，调用 `PUT /api/admin/users/{adminId}` `{"role":"USER"}` 或 `toggle-status`。
   - 此时再没有任何账号能进 `/api/admin/**`。

3. **JWT 密钥默认值能否伪造管理员（验证 P0-4）**
   - 直接用 `mySecretKeyForJWTTokenGenerationThatIsLongEnoughForHS256Algorithm` 离线签发 `subject=任意, claim role=ADMIN`，请求 `/api/admin/users`，**预期**应失败但实际通过。

4. **重置密码后是否强制改密（验证 P1-5）**
   - 管理员重置某用户密码 → 该用户用 `123456` 登录 → 直接调用受保护接口，**预期** 应被强制跳转改密，实际无任何拦截。

5. **改密码 / 改角色后旧 token 是否失效（验证 P1-7）**
   - 用户 U 登录拿 tokenU → 自行 `PUT /user/password` 改密 → 用 tokenU 继续访问 `/user/profile`，**预期** 应 401，实际仍 200。

6. **错误提示一致性（验证 P2-10）**
   - 不带 token 访问 `/api/user/profile`，前端控制台看到的错误消息是不是 “请求失败” 而非后端业务消息。
   - 给 `PUT /admin/users/{id}` 传 `role=admin`（小写），前端弹的是不是 “服务器内部错误”。

7. **管理员新增用户时偷偷传 role（验证 DTO 是否真的把多余字段忽略）**
   - `POST /admin/users` body `{"username":"x","password":"x","role":"ADMIN"}` —— 预期落库为 USER（DTO 不绑定该字段，已确认安全，但仍建议跑一次冒烟以防后续有人扩 DTO 时引入回归）。

8. **个人资料里偷偷传敏感字段（验证 UpdateProfileRequest 是否真的只接受三个字段）**
   - `PUT /user/profile` body `{"username":"hacker","role":"ADMIN","enabled":false,"password":"...","nickname":"x"}` —— 预期只 nickname 生效，但建议人工跑一次确认 Jackson 没有意外的 `@JsonAnySetter`。

9. **CORS 行为（验证 P1-8）**
   - 用 `evil.example.com` 的页面 `fetch('http://localhost:8080/api/user/profile', {credentials:'include', headers:{Authorization:...}})`，确认浏览器会发请求并能拿到响应（目前预期会，因为 `allowedOriginPatterns(*)` + `allowCredentials(true)`）。

10. **重启后默认账号是否被复活**
    - 把 `admin` 删除（或在数据库里删掉），重启 backend → `DataInitializer` 重新创建 `admin/admin123`。验证生产场景下是否真的允许这种行为。

---

## 六、给后续整改的简短建议（非本次改动范围，仅作记录）

- JWT 过滤器在校验签名后回查 `User.enabled` 和 `User.role`，或者在 token 里写入 `tokenVersion` / `passwordChangedAt`，用户改密/被禁用/被改角色时数据库 `tokenVersion++`，过滤器比对作废旧 token。
- 业务异常引入专属 `BusinessException`，区分 401/403/400/404 的语义，并和前端拦截器统一约定响应结构；为 Spring Security 添加 `AuthenticationEntryPoint`/`AccessDeniedHandler` 输出 `ApiResponse`。
- `AdminUpdateUserRequest.role` 改为 `User.Role` 枚举或加 `@Pattern(regexp="USER|ADMIN")`。
- `UserService.updateUser/toggleUserStatus` 校验 “不能修改自己角色 / 不能禁用自己 / 不能降级最后一个 ADMIN”。
- `app.default-password` 移到环境变量并且强制首登改密（实体加 `mustChangePassword` 字段）。
- 生产 `JWT_SECRET` 必传环境变量，启动时校验长度与是否等于默认值，若是则拒绝启动。
- `DataInitializer` 仅在 `spring.profiles.active=dev` 时生效。
- `spring.jpa.hibernate.ddl-auto` 生产环境改为 `validate` 或 `none`，迁移用 Flyway/Liquibase。
- CORS 配置改为白名单源；考虑把 token 改为 HttpOnly Cookie + CSRF 双 token。
