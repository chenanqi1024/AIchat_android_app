# 2026-07-07 测试账号完整流程

## 新增文件

- `app/src/main/java/vibe/ccc/aichat/data/auth/TestChatStore.kt`
  - 为测试账号提供本地聊天历史存储。
  - 支持按角色读取历史、加载更早消息、写入用户与助手消息、保存图片消息、清空历史。
- `logs/2026-07-07-test-account-full-flow.md`
  - 记录本次测试账号完整流程补齐。

## 修改文件

- `app/src/main/java/vibe/ccc/aichat/ui/app/AppContainer.kt`
  - 注入 `TestChatStore`，供首页、引导页和聊天页使用。
- `app/src/main/java/vibe/ccc/aichat/ui/app/AIchatApp.kt`
  - 将测试聊天存储传入首页、引导页和聊天页。
- `app/src/main/java/vibe/ccc/aichat/ui/home/HomeScreen.kt`
  - 首页恢复显示时重新刷新最近聊天，保证从聊天页返回后能看到最新记录。
- `app/src/main/java/vibe/ccc/aichat/ui/home/RoleListViewModel.kt`
  - 测试账号读取本地最近聊天，不再直接返回空列表。
- `app/src/main/java/vibe/ccc/aichat/ui/onboarding/OnboardingScreen.kt`
  - 适配新的 `RoleListViewModel` 构造参数。
- `app/src/main/java/vibe/ccc/aichat/ui/chat/ChatScreen.kt`
  - 将测试聊天存储传入 `ChatViewModel`。
  - 使用 Compose 推荐的 AutoMirrored 返回和发送图标。
- `app/src/main/java/vibe/ccc/aichat/ui/chat/ChatViewModel.kt`
  - 测试账号加载历史、加载更早消息、发送消息和清空历史均走本地测试存储。
  - 正式账号仍走真实后端 API。

## 完成功能

- 测试账号 `10086 / 1234` 登录后可正常进入首页和聊天页。
- 测试账号可按角色保留聊天历史。
- 测试账号可在首页看到最近聊天。
- 测试账号可清空当前角色聊天历史。
- 测试账号发送图片后，图片消息可保存在本地历史中。

## 当前未完成

- 测试账号没有真实后端 JWT，因此不调用真实 AI 聊天接口，也不与后端同步历史。
- 真实手机号验证码登录失败仍需后端短信服务单独修复。

## 验证结果

- 已通过 `./gradlew :app:assembleDebug`
- 已通过 `./gradlew :app:testDebugUnitTest`
