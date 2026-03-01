package com.pronetwork.app.viewmodel

import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// PaymentStatusResolver — مصدر وحيد للحقيقة (Single Source of Truth)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * مسؤول عن حساب حالة الدفع من البيانات الخام.
 *
 * يُستخدم كمصدر وحيد للحقيقة في كل مكان يحتاج تحديد [PaymentStatus]:
 * - PaymentViewModel (Flows التفاعلية + الإحصائيات)
 * - DailyCollectionBuilder (التحصيل اليومي التفصيلي)
 * - أي مكان مستقبلي يحتاج نفس المنطق
 *
 * Stateless — لا يحمل أي حالة داخلية، آمن للاستخدام المتعدد.
 */
@Singleton
class PaymentStatusResolver @Inject constructor() {

    /**
     * حساب حالة الدفع من المبلغ المدفوع والمبلغ المطلوب ووجود استرجاع.
     *
     * @param totalPaid   إجمالي المدفوع (من مجموع payment_transactions)
     * @param monthAmount المبلغ الشهري المطلوب (من جدول payments)
     * @param hasRefund   هل يوجد حركة سالبة (Refund) على هذا الـ Payment
     * @return [PaymentStatus] الحالة المحسوبة
     */
    fun resolve(
        totalPaid: Double,
        monthAmount: Double,
        hasRefund: Boolean
    ): PaymentStatus = when {
        totalPaid <= 0.0                        -> PaymentStatus.UNPAID
        totalPaid < monthAmount && hasRefund     -> PaymentStatus.SETTLED
        totalPaid < monthAmount                  -> PaymentStatus.PARTIAL
        else                                     -> PaymentStatus.FULL
    }

    /**
     * حساب الحالة وإرجاعها كـ String للاستخدام في UI models
     * التي تعتمد على String (مثل [DailyClientCollection.paymentStatus]).
     *
     * التحويل: FULL → "PAID" | الباقي → اسم الـ enum كما هو
     */
    fun resolveAsDisplayString(
        totalPaid: Double,
        monthAmount: Double,
        hasRefund: Boolean
    ): String = resolve(totalPaid, monthAmount, hasRefund).toDisplayString()
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension: تحويل PaymentStatus → String للعرض
// ─────────────────────────────────────────────────────────────────────────────

/**
 * تحويل [PaymentStatus] إلى String متوافق مع [DailyClientCollection.paymentStatus].
 *
 * السبب: الـ enum يستخدم [FULL] لكن الـ UI models تستخدم "PAID".
 * هذه الدالة توحّد التحويل في مكان واحد.
 */
fun PaymentStatus.toDisplayString(): String = when (this) {
    PaymentStatus.FULL -> "PAID"
    else -> this.name   // UNPAID, PARTIAL, SETTLED تبقى كما هي
}
