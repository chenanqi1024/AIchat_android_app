# 2026-07-07 本地测试账号

## 新增文件

- `app/src/main/java/vibe/ccc/aichat/data/auth/TestAccount.kt`
  - 定义 Android 本地测试账号：账号 `10086`，验证码 `1234`。
  - 生成本地测试会话 token 和测试用户信息。
- `logs/2026-07-07-local-test-account.md`
  - 记录本次测试账号配置内容和验证结果。

## 修改文件

- `app/src/main/java/vibe/ccc/aichat/ui/login/LoginViewModel.kt`
  - 登录时识别 `10086 / 1234`，不调用短信登录 API，直接写入本地测试会话。
  - 真实手机号验证码登录逻辑保持不变。
- `app/src/main/java/vibe/ccc/aichat/ui/home/RoleListViewModel.kt`
  - 识别本地测试 token，跳过最近聊天历史接口请求，避免被后端 JWT 鉴权拦截。
- `app/src/main/java/vibe/ccc/aichat/ui/chat/ChatViewModel.kt`
  - 识别本地测试 token，跳过聊天历史、清空历史和流式聊天接口。
  - 测试账号发送消息时追加本地测试回复，避免无效 token 触发重新登录。

## 完成功能

- 可使用账号 `10086`、验证码 `1234` 登录 Android App。
- 测试账号可进入首页和聊天页，并可发送本地测试消息。
- 正式手机号验证码登录仍走原后端 API。

## 当前未完成

- 测试账号不具备真实后端 JWT，不能同步真实聊天历史，也不会调用真实 AI 聊天接口。
- 后端短信登录 API 仍需单独修复。

## 验证结果

- 已通过 `./gradlew :app:assembleDebug`
- 已通过 `./gradlew :app:testDebugUnitTest`

