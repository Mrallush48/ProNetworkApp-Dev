package com.pronetwork.app.viewmodel

// ─────────────────────────────────────────────────────────────────────────────
// PaymentStatus — حالات الدفع الممكنة
// ─────────────────────────────────────────────────────────────────────────────

/**
 * حالات الدفع الأربع المستخدمة في كل التطبيق.
 *
 * - [UNPAID]   لم يُدفع شيء
 * - [PARTIAL]  دفع جزئي بدون استرجاع
 * - [SETTLED]  دفع جزئي مع وجود استرجاع (تسوية)
 * - [FULL]     مدفوع بالكامل
 */
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    SETTLED,
    FULL
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension: تحويل PaymentStatus → String للعرض
// ─────────────────────────────────────────────────────────────────────────────

/**
 * تحويل [PaymentStatus] إلى String متوافق مع UI models التي تعتمد على String.
 *
 * السبب: الـ enum يستخدم [FULL] لكن الـ UI models تستخدم "PAID".
 * هذه الدالة توحّد التحويل في مكان واحد.
 */
fun PaymentStatus.toDisplayString(): String = when (this) {
    PaymentStatus.FULL -> "PAID"
    else -> this.name  // UNPAID, PARTIAL, SETTLED تبقى كما هي
}
