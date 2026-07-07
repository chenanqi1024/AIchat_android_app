package vibe.ccc.aichat.data.role

import vibe.ccc.aichat.data.model.ChatRole

data class RoleVisual(
    val id: Int,
    val key: String,
    val nickname: String,
    val onboardingDescription: String,
    val homeTag: String,
    val homeDescription: String,
    val chatTag: String,
    val greeting: String,
    val welcomeMessage: String,
    val avatarUrl: String,
    val backgroundUrl: String
)

object RolePresentation {
    val visuals = listOf(
        RoleVisual(
            id = 1,
            key = "naitang",
            nickname = "奶糖",
            onboardingDescription = "猫咪系陪伴角色",
            homeTag = "猫咪系",
            homeDescription = "喵~ 想要被温柔陪伴的一天",
            chatTag = "猫咪系陪伴角色",
            greeting = "今天也要开心喵~",
            welcomeMessage = "喵呜，我是奶糖，今天也想黏在你身边陪你聊天呀。你想先跟我说说现在的心情，还是让我蹭蹭你再开始？",
            avatarUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_avatar_cat.jpg",
            backgroundUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_bg_cat.jpg"
        ),
        RoleVisual(
            id = 2,
            key = "wanqing",
            nickname = "晚晴",
            onboardingDescription = "温柔成熟的姐姐型陪伴角色",
            homeTag = "温柔姐姐",
            homeDescription = "愿意倾听你所有的心事",
            chatTag = "温柔姐姐型陪伴角色",
            greeting = "有什么想聊的吗？",
            welcomeMessage = "你好呀，我是晚晴。看起来你今天也经历了很多事情呢，想和我聊聊吗？我会一直陪着你的。",
            avatarUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_avatar_girl.jpg",
            backgroundUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_bg_girl.jpg"
        ),
        RoleVisual(
            id = 3,
            key = "yaochuan",
            nickname = "曜川",
            onboardingDescription = "阳光帅气的少年型陪伴角色",
            homeTag = "阳光少年",
            homeDescription = "用笑容驱散你的阴霾",
            chatTag = "阳光少年型陪伴角色",
            greeting = "嘿！今天过得怎么样？",
            welcomeMessage = "嘿！我是曜川，很高兴能陪你聊天。不管遇到什么事，我都会在你身边的！今天过得怎么样？",
            avatarUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_avatar_boy.jpg",
            backgroundUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_bg_boy.jpg"
        ),
        RoleVisual(
            id = 4,
            key = "xiaofu",
            nickname = "小芙",
            onboardingDescription = "梦境系精灵陪伴角色",
            homeTag = "梦境精灵",
            homeDescription = "在梦境中寻找温暖的陪伴",
            chatTag = "梦境精灵陪伴角色",
            greeting = "要一起做个美梦吗？",
            welcomeMessage = "嗨~ 我是小芙，来自梦境的精灵。在这里，你可以和我分享任何想说的话，就像在温柔的梦里一样安心。",
            avatarUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_avatar_elf.jpg",
            backgroundUrl = "https://zzz-pet.oss-cn-hangzhou.aliyuncs.com/image/chat_bg_elf.jpg"
        )
    )

    val defaults: List<ChatRole> = visuals.map { visual ->
        ChatRole(
            id = visual.id,
            key = visual.key,
            nickname = visual.nickname,
            description = visual.onboardingDescription,
            avatarUrl = visual.avatarUrl,
            backgroundUrl = visual.backgroundUrl
        )
    }

    fun visualFor(role: ChatRole): RoleVisual? =
        visuals.firstOrNull { it.key == role.key } ?: visuals.firstOrNull { it.id == role.id }

    fun figmaOrdered(roles: List<ChatRole>): List<ChatRole> {
        val remaining = roles.toMutableList()
        val ordered = mutableListOf<ChatRole>()

        visuals.forEach { visual ->
            val index = remaining.indexOfFirst { it.key == visual.key || it.id == visual.id }
            if (index >= 0) {
                ordered += remaining.removeAt(index)
            } else {
                ordered += ChatRole(
                    id = visual.id,
                    key = visual.key,
                    nickname = visual.nickname,
                    description = visual.onboardingDescription,
                    avatarUrl = visual.avatarUrl,
                    backgroundUrl = visual.backgroundUrl
                )
            }
        }

        return ordered + remaining
    }
}

val ChatRole.displayName: String
    get() = RolePresentation.visualFor(this)?.nickname ?: nickname

val ChatRole.onboardingDescription: String
    get() = RolePresentation.visualFor(this)?.onboardingDescription ?: description

val ChatRole.homeDescription: String
    get() = RolePresentation.visualFor(this)?.homeDescription ?: description

val ChatRole.displayTag: String
    get() = RolePresentation.visualFor(this)?.homeTag
        ?: description.replace("陪伴角色", "").trim()

val ChatRole.chatTag: String
    get() = RolePresentation.visualFor(this)?.chatTag ?: description

val ChatRole.greeting: String
    get() = RolePresentation.visualFor(this)?.greeting ?: "我在这里，随时听你说。"

val ChatRole.welcomeMessage: String
    get() = RolePresentation.visualFor(this)?.welcomeMessage
        ?: "你好，我是 $displayName。把想说的话慢慢告诉我吧，我会认真听。"

val ChatRole.avatarImageUrl: String?
    get() = RolePresentation.visualFor(this)?.avatarUrl ?: avatarUrl

val ChatRole.backgroundImageUrl: String?
    get() = RolePresentation.visualFor(this)?.backgroundUrl ?: backgroundUrl
