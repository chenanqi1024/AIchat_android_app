项目名称：
AI 陪伴聊天 App Android 版

项目目标：
基于同级目录中的 iOS App（AIchat_ios）代码实现 Android App。

本项目没有单独的设计稿，也没有单独的后端 API 文档。Android 版的功能、接口、数据模型、页面流程、交互状态、错误处理和视觉风格，均以 iOS 项目中的现有代码为实现依据。

iOS App 目录：
../AIchat_ios

Android App 目录：
./

核心原则：
- iOS 代码是 Android 实现的事实来源。
- 不假设存在额外设计稿。
- 不假设存在额外后端 API 文档。
- 不根据猜测扩展功能。
- 不随意改变 iOS 已有接口字段、请求路径、错误码和页面流程。
- 如果 Android 平台需要调整实现方式，应保持最终用户体验与 iOS 版一致。

重点参考文件：
../AIchat_ios/AIchat_ios/Services/APIClient.swift
../AIchat_ios/AIchat_ios/Services/AuthStore.swift
../AIchat_ios/AIchat_ios/Models/APIModels.swift
../AIchat_ios/AIchat_ios/Models/RolePresentation.swift
../AIchat_ios/AIchat_ios/ViewModels/LoginViewModel.swift
../AIchat_ios/AIchat_ios/ViewModels/RoleListViewModel.swift
../AIchat_ios/AIchat_ios/ViewModels/ChatViewModel.swift
../AIchat_ios/AIchat_ios/Views/AppRootView.swift
../AIchat_ios/AIchat_ios/Views/OnboardingView.swift
../AIchat_ios/AIchat_ios/Views/HomeView.swift
../AIchat_ios/AIchat_ios/Views/ChatView.swift
../AIchat_ios/AIchat_ios/Views/LoginSheetView.swift
../AIchat_ios/AIchat_ios/Views/SettingsView.swift
../AIchat_ios/AIchat_ios/Views/Components/AppTheme.swift
../AIchat_ios/AIchat_ios/Views/Components/MessageBubbleView.swift
../AIchat_ios/AIchat_ios/Views/Components/RemoteImageView.swift

Android 技术栈：
- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle / ViewModel
- Kotlin Coroutines / Flow
- AndroidX Navigation Compose
- OkHttp，用于普通 HTTP 请求和 SSE 流式聊天
- 当前实现使用 Android 内置 org.json 进行 JSON 编解码；如后续环境可稳定解析依赖，也可改用 kotlinx.serialization 或 Moshi
- Coil Compose，用于网络图片加载
- DataStore Preferences，用于普通偏好存储
- AndroidX Security Crypto，用于 access token 安全存储

当前 Android 工程基础：
- 包名：vibe.ccc.aichat
- minSdk：26
- targetSdk：36
- compileSdk：36.1
- Kotlin：2.2.10
- AGP：9.2.1
- Compose BOM：2026.02.01

接口来源：
接口定义以 iOS 的 APIClient.swift 和 APIModels.swift 为准。

当前 iOS 代码中使用的 Base URL：
登录服务：
https://aichat-login-kemznyglgb.cn-hangzhou.fcapp.run

聊天服务：
https://aichat-chat-nitnspniec.cn-hangzhou.fcapp.run

当前 iOS 代码中使用的接口：
- POST /send-code
- POST /login
- GET /roles
- GET /history
- DELETE /history
- POST /chat

普通接口统一响应结构、字段名、错误处理逻辑，以 APIModels.swift 和 APIClient.swift 为准。

聊天流式响应：
Android 端应按 iOS 的 streamChat 实现处理 text/event-stream。

已知事件：
- start：返回 ChatStartEvent
- delta：返回 ChatDeltaEvent
- done：返回 ChatDoneEvent
- error：返回 ChatErrorEvent

Android 实现要求：
- 必须在 AndroidManifest.xml 中声明 INTERNET 权限。
- UI 使用 Jetpack Compose 实现，不使用 XML 页面布局。
- 页面状态优先使用 ViewModel + StateFlow 管理。
- 网络请求、认证存储、页面状态和 Compose UI 要分层清晰。
- 不要在 Composable 中堆积网络请求、持久化或复杂业务逻辑。
- 新增依赖必须服务于真实实现需求，并写入 gradle/libs.versions.toml。
- 不为了“高级架构”引入不必要的抽象。

推荐目录结构：
app/src/main/java/vibe/ccc/aichat/
- MainActivity.kt
- data/
  - auth/
  - model/
  - network/
  - role/
- ui/
  - app/
  - onboarding/
  - home/
  - chat/
  - login/
  - settings/
  - components/
  - theme/
- util/

功能实现范围：
1. 启动与根流程
   - 对应 iOS AppRootView。
   - 使用 hasSeenOnboarding 判断是否展示引导页。
   - 首次完成引导后进入首页。
   - 如果引导页选择了角色，进入首页后应继续打开该角色聊天。

2. 认证
   - 对应 iOS AuthStore 和 LoginViewModel。
   - 手机号限制为 11 位数字。
   - 验证码限制为数字，至少 4 位。
   - 获取验证码后根据 retryAfter 倒计时。
   - 登录成功后保存 access token 和用户信息。
   - access token 不应明文存入普通 SharedPreferences。
   - 遇到 AUTH_REQUIRED、INVALID_TOKEN、TOKEN_EXPIRED 或 token 缺失时，应清理认证状态并引导重新登录。

3. 引导页
   - 对应 iOS OnboardingView。
   - 加载角色列表。
   - 展示角色横向轮播。
   - 点击“开始聊天”时，如果未登录，先弹出登录页；登录成功后继续进入所选角色。

4. 首页
   - 对应 iOS HomeView 和 RoleListViewModel。
   - 加载角色列表。
   - 记录 selectedRoleId。
   - 展示欢迎文案、精选角色、所有角色、快捷入口、最近聊天。
   - 未登录时，打开角色聊天前应先弹出登录页。
   - 登录状态变化后应刷新最近聊天。
   - 支持刷新首页数据。

5. 角色展示
   - 对应 iOS RolePresentation.swift。
   - Android 端应实现相同的本地角色视觉映射和展示文案。
   - /roles 请求失败时，应使用本地默认角色兜底，不应显示空白页面。
   - 角色排序和补齐逻辑应与 iOS 的 figmaOrdered 保持一致。

6. 聊天页
   - 对应 iOS ChatView 和 ChatViewModel。
   - 进入页面后加载当前角色聊天历史。
   - 支持加载更早消息。
   - 支持发送文本消息。
   - 支持发送图片附件。
   - 发送时先插入本地临时 user message 和空 assistant message。
   - 收到 start 事件后替换本地 user message。
   - 收到 delta 事件后追加 assistant 内容。
   - 收到 done 事件后用服务端 assistantMessage 替换本地 assistant message。
   - 收到 error 事件后展示错误；如认证失效，应清理登录态并弹出登录页。
   - 支持取消正在进行的流式回复。
   - 页面离开时应取消未完成的流式请求。
   - 支持清空当前角色聊天历史。

7. 图片附件
   - 对应 iOS ChatImageCompressor。
   - 使用系统图片选择器选择图片。
   - 将图片压缩为 JPEG。
   - 最长边不超过 1440。
   - 按多个质量档位尝试压缩。
   - 压缩后大小不超过 6MB。
   - 发送给 /chat 的图片格式为 data:image/jpeg;base64,...
   - 本地消息气泡中应显示已选择的图片预览。

8. 登录弹窗
   - 对应 iOS LoginSheetView。
   - 应包含手机号输入、验证码输入、获取验证码按钮、登录按钮、错误提示和协议文案。
   - 获取验证码按钮应显示发送中、倒计时和可重新发送状态。

9. 设置页
   - 对应 iOS SettingsView。
   - 展示当前登录状态。
   - 已登录时展示用户手机号。
   - 未登录时可打开登录弹窗。
   - 已登录时可退出登录。
   - 退出登录只清理认证状态，不应清理无关偏好。

视觉实现依据：
没有单独设计稿。视觉实现参考 iOS 的 SwiftUI 页面代码和 AppTheme.swift。

Android 端应尽量保持：
- 浅色背景
- 柔和渐变
- 白色卡片
- 圆角
- 轻阴影
- 紫粉主色
- 角色远程头像和背景图
- 用户和助手聊天气泡的方向、颜色和圆角差异
- 加载、空状态、错误状态的中文文案

如果 SwiftUI 的某些视觉效果不适合 Compose 原样实现，可以使用 Android 原生、简洁、可维护的方式近似表达。

编码原则：
- 先实现真实可运行流程，再完善视觉细节。
- 保证每一步改动都可以编译。
- 不提交无法编译的半成品。
- 不修改与当前任务无关的大量代码。
- 不破坏已有 Gradle 配置。
- 不硬编码用户 token、验证码或其他敏感信息。
- 不忽略网络错误、解析错误、认证失效和流式请求取消。
- 不把 iOS 代码机械翻译成 Kotlin，应使用 Android 常规写法复刻行为。

验证要求：
每次完成实现任务后，优先运行：
./gradlew :app:assembleDebug

如修改了核心逻辑，视情况补充或运行：
./gradlew :app:testDebugUnitTest

如果因为依赖下载、网络限制或本地环境导致无法验证，应在交付说明中明确说明。

交付要求：
每次完成开发任务时，应说明：
- 新增了哪些文件
- 修改了哪些文件
- 完成了哪些功能
- 是否已编译验证
- 当前仍未完成或需要注意的内容

如任务涉及较大实现，应在 Android 项目根目录 logs/ 下保存一份本次改动日志，优先使用 .md 格式。

最终目标：
产出一个以 iOS 现有代码为唯一参考实现的 Android App：
- 可登录
- 可选择陪伴角色
- 可查看最近聊天
- 可进行文本和图片聊天
- 可流式展示 AI 回复
- 可查看、分页加载和清空聊天历史
- 可管理登录状态
- 可在 Android 设备或模拟器上稳定运行
- 代码结构清晰、可维护，用户体验接近 iOS 版
