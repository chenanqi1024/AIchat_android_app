# 2026-07-07 登录验证码修复

## 新增文件

- `logs/2026-07-07-login-verification-fix.md`
  - 记录本次 Android 登录验证码修复内容和验证结果。

## 复查与对齐文件

- `app/src/main/java/vibe/ccc/aichat/data/network/APIClient.kt`
  - 已与 iOS `APIClient.swift` 对齐：发送验证码和登录均严格依赖后端 `success=true` 与 `data` 字段。
  - 发送验证码成功解析 `bizId`、`expiresIn`、`retryAfter`；失败时直接透传后端错误。
  - 登录成功严格解析 `accessToken`、`tokenType`、`expiresIn`、`user`；失败时直接透传后端错误。
- `app/src/main/java/vibe/ccc/aichat/data/model/APIError.kt`
  - 已与 iOS `APIError.server(code:message:)` 对齐，仅保留 `code` 与 `message`。
- `app/src/main/java/vibe/ccc/aichat/ui/login/LoginViewModel.kt`
  - 已与 iOS `LoginViewModel.swift` 对齐：手机号限制 11 位数字，验证码限制 4 位数字。
  - `canLogin` 使用手机号合法且验证码至少 4 位。
  - 发送验证码和登录失败时直接显示 `error.localizedMessage`，不做 Android 侧额外错误码映射或兜底。

## 完成功能

- Android 登录链路已重新对齐 iOS 代码。
- 复查确认 Android/iOS 使用的登录字段 `phoneNumber + verifyCode` 与后端校验入口一致。
- 复查确认 Android 当前报错来自后端响应，而不是 Android 字段名、验证码长度或本地解析逻辑。

## API 复查结论

- `POST /send-code` 对测试手机号返回 `HTTP 502`，body 为 `{"code":"UNKNOWN","message":"UNKNOWN","success":false}`，但用户反馈短信实际可送达，说明后端存在“短信已发出但响应仍报 5xx/UNKNOWN”的问题。
- `POST /login` 使用 `phoneNumber + verifyCode` 会进入短信服务校验；测试验证码返回 `HTTP 502`，body 中 `code` 为 `SMS_PROVIDER_ERROR`，`details.providerCode` 为 `isv.ValidateFail`，`message` 为“验证失败”。
- 改用 `code` 字段会返回 `INVALID_INPUT`，说明 Android 当前使用 `verifyCode` 字段是正确的。
- 如果用户输入真实短信中的四位验证码仍收到 `SMS_PROVIDER_ERROR/isv.ValidateFail`，前端无法安全绕过，根因应在登录 API 或短信验证码校验服务。
- 2026-07-07 再次请求测试号时，`/send-code` 仍返回 `HTTP 502 + UNKNOWN`，`/login` 仍返回 `HTTP 502 + SMS_PROVIDER_ERROR + isv.ValidateFail`。
- `GET /health` 返回 `success=true`、版本 `2026.06.11-dbdiag.1`。
- `GET /health/config` 返回 `loginReady=true`、`issues=[]`，但接口说明明确“不会验证阿里云 AccessKey 权限”；真实短信上游调用仍失败。
- 后端 `LoginService/app.py` 中 `/send-code` 调用 `SendSmsVerifyCode`，`/login` 调用 `CheckSmsVerifyCode`；当前失败位置在短信上游调用或验证码校验结果，不在客户端。

## 当前未完成

- 未使用真实手机号完成端到端人工登录验证；本次仅通过测试手机号确认了后端会返回 `UNKNOWN` / `SMS_PROVIDER_ERROR` 等错误形态。
- 工作区中已有 `AuthStore.kt`、`SecureTokenStore.kt` 和 `.kotlin/` 未提交改动，本次未回滚或覆盖。

## 验证结果

- 已通过 `./gradlew :app:assembleDebug`
- 已通过 `./gradlew :app:testDebugUnitTest`
