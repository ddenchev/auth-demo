package doichin.auth

enum class EnvType(val env: String) {
    DEV("dev"),
    TEST("test"),
    BUILD("build"),
    PROD("prod"),
}