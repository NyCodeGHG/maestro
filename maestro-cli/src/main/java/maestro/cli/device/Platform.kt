package maestro.cli.device

enum class Platform(val description: String) {
    ANDROID("Android"),
    IOS("iOS"),
    WEB("Web");

    companion object {
        fun fromString(p: String): Platform? {
            return entries.firstOrNull { it.description.lowercase() == p.lowercase() }
        }
    }
}