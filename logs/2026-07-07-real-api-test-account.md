# 2026-07-07 测试账号真实接口链路

## 新增文件

- `logs/2026-07-07-real-api-test-account.md`
  - 记录本次测试账号从本地模拟改为真实 API 链路的实现和验证结果。

## 修改文件

- `app/src/main/java/vibe/ccc/aichat/data/auth/TestAccount.kt`
  - 保留 `10086 / 1234` 测试账号常量。
  - 删除本地测试 session 生成逻辑。
  - 保留旧本地 token 识别，用于让旧版本登录态自动失效。
- `app/src/main/java/vibe/ccc/aichat/data/auth/AuthStore.kt`
  - 读取 token 时过滤旧的 `local-test-token-10086`，避免继续使用无效本地 token。
- `app/src/main/java/vibe/ccc/aichat/ui/login/LoginViewModel.kt`
  - 测试账号登录改为调用真实 `/login`，由后端签发真实 JWT。
  - 测试账号允许触发 `/send-code`，用于对齐普通账号流程。
- `app/src/main/java/vibe/ccc/aichat/ui/chat/ChatViewModel.kt`
  - 移除测试账号本地聊天回复和本地历史分支。
  - 聊天、历史、清空历史均恢复走真实 ChatService API。
- `app/src/main/java/vibe/ccc/aichat/ui/app/AppContainer.kt`
  - 移除本地测试聊天存储。
- `app/src/main/java/vibe/ccc/aichat/ui/app/AIchatApp.kt`
  - 移除本地测试聊天存储传参。
- `app/src/main/java/vibe/ccc/aichat/ui/home/RoleListViewModel.kt`
  - 最近聊天恢复统一通过真实 `/history` 获取。
- `app/src/main/java/vibe/ccc/aichat/ui/home/HomeScreen.kt`
  - 保留首页恢复时刷新最近聊天，确保真实历史同步及时更新。
- `app/src/main/java/vibe/ccc/aichat/ui/onboarding/OnboardingScreen.kt`
  - 移除本地测试聊天存储传参。
- `app/src/main/java/vibe/ccc/aichat/ui/chat/ChatScreen.kt`
  - 移除本地测试聊天存储传参。

## 后端配套

- 已在桌面 `AIChat-API/LoginService/app.py` 本地源码中补充 `10086 / 1234` 真实 JWT 登录分支。
- 已更新 `AIChat-API/tests/test_login_service.py`、`README.md`、`Doc/API.md`、`Doc/API.html`。
- 已生成新版 LoginService 部署包：`/tmp/LoginService-deploy.zip`。

## 完成功能

- Android 测试账号不再使用本地假 token。
- 后端部署后，测试账号可获得真实 JWT。
- 后端部署后，测试账号的聊天、天气查询、聊天历史和清空历史都会走真实 ChatService。

## 当前未完成

- 当前线上 LoginService 还未部署新版代码；线上 `10086 / 1234` 暂时仍会返回手机号格式错误。
- 需要将 `/tmp/LoginService-deploy.zip` 部署到线上 LoginService 后，Android 新包才能完成真实 AI 和天气调用。

## 验证结果

- Android 已通过 `./gradlew :app:assembleDebug`
- Android 已通过 `./gradlew :app:testDebugUnitTest`
- 后端已通过 `PYTHONPATH=/tmp/aichat-api-test-deps python3 -m pytest tests/test_login_service.py`
