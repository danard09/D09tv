package com.example.i18n

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val shortBadge: String,
    val isRtl: Boolean,
) {
    SORANI(
        code = "ckb",
        nativeLabel = "کوردی (سۆرانی)",
        shortBadge = "سۆرانی",
        isRtl = true,
    ),
    BADINI(
        code = "kmr",
        nativeLabel = "کوردی (بادینی)",
        shortBadge = "بادینی",
        isRtl = true,
    ),
    ARABIC(
        code = "ar",
        nativeLabel = "العربية",
        shortBadge = "عربي",
        isRtl = true,
    ),
    ENGLISH(
        code = "en",
        nativeLabel = "English",
        shortBadge = "EN",
        isRtl = false,
    );

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
