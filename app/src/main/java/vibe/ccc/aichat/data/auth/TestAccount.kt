package vibe.ccc.aichat.data.auth

object TestAccount {
    const val phoneNumber = "10086"
    const val verifyCode = "1234"
    private const val legacyLocalAccessToken = "local-test-token-10086"

    fun matches(phoneNumber: String, verifyCode: String): Boolean =
        phoneNumber == TestAccount.phoneNumber && verifyCode == TestAccount.verifyCode

    fun isPhoneNumber(phoneNumber: String): Boolean = phoneNumber == TestAccount.phoneNumber

    fun isLegacyToken(token: String?): Boolean = token == legacyLocalAccessToken
}
