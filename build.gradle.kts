// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // ❌ شلنا plugin compose لأنه غير مطلوب أصلاً
    alias(libs.plugins.hilt) apply false
}
