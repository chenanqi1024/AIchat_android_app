# 2026-07-07 Android iOS Port

## 新增文件
- `data/model/`：迁移 iOS API 数据模型、聊天事件和错误类型。
- `data/network/`：实现登录、角色、历史、清空历史和 SSE 流式聊天请求。
- `data/auth/`：实现 token 安全存储、用户偏好存储和认证状态。
- `data/role/`：实现 iOS 同款默认角色、展示文案、排序和兜底逻辑。
- `ui/app/`：实现应用容器、根流程和 Navigation Compose。
- `ui/login/`：实现验证码登录弹窗和登录 ViewModel。
- `ui/home/`：实现首页和角色列表 ViewModel。
- `ui/onboarding/`：实现首次引导角色选择页。
- `ui/chat/`：实现聊天页、流式消息状态和图片附件发送。
- `ui/settings/`：实现设置页、登录入口和退出登录。
- `ui/components/`：实现远程图片、渐变视觉组件和消息气泡。
- `util/`：实现 ViewModel 工厂、时间格式化和图片压缩工具。

## 修改文件
- `app/build.gradle.kts`、`build.gradle.kts`、`gradle/libs.versions.toml`：新增 Compose 导航、ViewModel、OkHttp、Coil、DataStore、Security Crypto 等依赖。
- `app/src/main/AndroidManifest.xml`：新增 `INTERNET` 权限。
- `MainActivity.kt`：替换模板 Greeting，接入 `AIchatApp`。
- `ui/theme/Color.kt`、`ui/theme/Theme.kt`：迁移 iOS 主色、浅色背景和基础 Material 主题。

## 完成功能
- 首次引导、角色选择、首页、设置页、聊天页完整串联。
- 登录验证码流程、token 保存、认证失效重新登录。
- 角色接口失败时使用本地默认角色兜底。
- 普通接口 envelope 解析和中文错误提示。
- `/chat` SSE 事件解析：`start`、`delta`、`done`、`error`。
- 聊天历史加载、加载更早、发送文本、取消回复、清空历史。
- 系统图片选择、JPEG 压缩、base64 data URL 发送、本地图片预览。

## 验证结果
- 已运行 `./gradlew :app:assembleDebug`，构建通过。
- 构建时需要使用 Android Studio bundled JDK，并清理失效代理 JVM 参数。

## 当前注意事项
- 本机无法解析 Kotlin serialization Gradle 插件，因此 JSON 解析改为 Android 自带 `org.json` 手写解析；接口字段仍以 iOS 代码为准。
- 未进行真机/模拟器手动登录和聊天验证。
