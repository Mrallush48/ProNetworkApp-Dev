# ProNetworkAppٍSpot - Development Roadmap
# خارطة طريق التطوير

> **آخر تحديث**: 2026-02-18
> **الإصدار الحالي**: v1.1 - النظام الأساسي مكتمل

---

## المراحل العامة

| المرحلة | الوصف | الحالة |
|--------|------|--------|
| المرحلة 1 | الشاشة الرئيسية Dashboard | ✅ مكتملة |
| المرحلة 2 | الألوان الذكية + الفرز المتقدم + دعم SETTLED | ✅ مكتملة |
| المرحلة 3 | تحسين التصدير + تقارير المقارنة + دورة حياة العميل | ⭕ لم يبدأ |
| المرحلة 4 | الإعدادات + الوضع الليلي + الثيمات | ⭕ لم يبدأ |
| المرحلة 5 | الإشعارات الذكية | ⭕ لم يبدأ |
| المرحلة 6 | الإغلاق السنوي | ⭕ لم يبدأ |
| المرحلة 7 | الحماية (بصمة/رقم سري) | ⭕ لم يبدأ |
| المرحلة 8 | المزامنة السحابية | ⭕ لم يبدأ |

---

## المرحلة 1: الشاشة الرئيسية (Dashboard)
**الأولوية**: عالية جداً | **الجهد**: متوسط | **التأثير**: عالي جداً

### الهدف
شاشة رئيسية تظهر عند فتح التطبيق تعطي لمحة شاملة عن حالة التحصيل والعملاء.

### الملفات المطلوبة

#### 1.1 إنشاء DashboardScreen.kt (جديد)
**المسار**: `app/src/main/java/com/pronetwork/app/ui/screens/DashboardScreen.kt`

**المكونات**:
- **بطاقة ترحيب**: التاريخ الهجري + الميلادي + رسالة ترحيب
- **3 بطاقات KPI رئيسية**:
    - إجمالي التحصيل هذا الشهر (بالريال)
    - نسبة التحصيل (مع مؤشر دائري CircularProgressIndicator)
    - عدد العملاء النشطين
- **بطاقة المقارنة**: هذا الشهر vs الشهر السابق (سهم أخضر للزيادة / أحمر للنقصان)
- **قائمة "يحتاج انتباهك"**: أهم 5 عملاء غير مدفوعين بأعلى مبالغ
- **آخر العمليات**: آخر 5 عمليات تحصيل
- **أزرار سريعة**: "ابدأ التحصيل اليومي" + "إضافة عميل"

**المدخلات (Parameters)**:
```kotlin
@Composable
fun DashboardScreen(
    currentMonthStats: PaymentViewModel.MonthStats?,
    previousMonthStats: PaymentViewModel.MonthStats?,
    totalClients: Int,
    totalBuildings: Int,
    recentTransactions: List<RecentTransaction>,
    topUnpaidClients: List<UnpaidClientInfo>,
    onNavigateToDaily: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToStats: () -> Unit
)

1.2 إنشاء DashboardViewModel.kt (جديد)
المسار: app/src/main/java/com/pronetwork/app/viewmodel/DashboardViewModel.kt

المهام:

جلب إحصائيات الشهر الحالي والسابق

جلب آخر 5 عمليات

جلب أعلى 5 عملاء غير مدفوعين

حساب نسب المقارنة

1.3 تعديل MainActivity.kt
تغيير الشاشة الافتراضية من clients إلى dashboard

إضافة tab جديد للـ Dashboard في NavigationBar

ربط DashboardViewModel بالبيانات

1.4 إضافة Data Classes (جديد)

data class RecentTransaction(
    val clientName: String,
    val amount: Double,
    val type: String,     // Payment / Refund
    val time: String,
    val buildingName: String
)

data class UnpaidClientInfo(
    val clientId: Int,
    val clientName: String,
    val buildingName: String,
    val monthlyAmount: Double,
    val totalPaid: Double,
    val remaining: Double
)


1.5 إضافة String Resources
values/strings.xml + values-ar/strings.xml

جميع النصوص للـ Dashboard

قائمة المهام
 إنشاء DashboardScreen.kt

 دمج بيانات Dashboard في PaymentViewModel (بدلاً من ViewModel منفصل)

 إنشاء Data Classes (RecentTransaction, UnpaidClientInfo)

 تعديل MainActivity.kt (إضافة tab + ربط البيانات)

 إضافة Strings (عربي + إنجليزي)

 اختبار ومراجعة

المرحلة 2: الألوان الذكية + الفرز المتقدم
الأولوية: عالية | الجهد: منخفض | التأثير: عالي

الهدف
تحسين عرض قائمة العملاء بألوان ذكية + خيارات فرز وعرض متقدمة.

التعديلات
2.1 الألوان الذكية في ClientListScreen.kt
إضافة شريط لوني جانبي لكل عميل:

🟢 أخضر: مدفوع بالكامل (PAID)

🟠 برتقالي: دفع جزئي (PARTIAL)

🔵 أزرق: مُسوَّى (SETTLED)

🔴 أحمر: غير مدفوع (UNPAID)

عرض المبلغ المتبقي بنفس لون الحالة

2.2 نظام الفرز والعرض المتقدم
إضافة قائمة طرق العرض:

حسب حالة الدفع (مدفوع / جزئي / مُسوَّى / غير مدفوع)

حسب الاسم (أبجدي تصاعدي/تنازلي)

حسب المبنى

حسب الباقة

حسب قيمة الاشتراك (تصاعدي/تنازلي)

حسب تاريخ آخر دفعة (الأحدث/الأقدم)

حسب نسبة التحصيل (الأقل أولاً)

فلتر ذكي: عرض المتأخرين فقط (> شهر)

إضافة زر "View Options" في TopAppBar يفتح مربع حوار اختيار طريقة العرض

2.3 إنشاء ViewOptionsDialog.kt
مربع حوار يحتوي RadioButtons لكل خيار

حفظ الاختيار في SharedPreferences

قائمة المهام
 إضافة الألوان الذكية لـ ClientListScreen

 إنشاء ViewOptionsDialog

 تعديل filterClients() في MainActivity لدعم كل طرق الفرز

 إضافة Strings

 اختبار

المرحلة 3: تحسين التصدير + تقارير المقارنة + دورة حياة العميل
الأولوية: عالية | الجهد: متوسط-عالي | التأثير: عالي جداً

الهدف
ترقية نظام التصدير والتقارير لمستوى احترافي عالمي، مع بناء نظام دورة حياة متكامل
للعملاء يدعم تقارير التسرب (Churn Analysis).

المرحلة 3 مقسمة إلى 5 أجزاء

| الجزء | الوصف                                           | الجهد | الأولوية        |
| ----- | ----------------------------------------------- | ----- | --------------- |
| 3.1   | دورة حياة العميل (Subscriber Lifecycle)         | عالي  | 1 - يُنفذ أولاً |
| 3.2   | تحسين تنسيق Excel (Freeze + Auto-Width)         | منخفض | 2               |
| 3.3   | QR Code في التقارير                             | منخفض | 3               |
| 3.4   | تقرير الإيرادات حسب الباقة (Revenue by Package) | متوسط | 4               |
| 3.5   | تقرير المقارنة الفترية + تحليل التسرب (Churn)   | متوسط | 5               |

3.1 دورة حياة العميل (Subscriber Lifecycle) 🔄
لماذا يُنفذ أولاً؟ لأن تقرير التسرب (3.5) يعتمد على هذا النظام. بدونه لا نقدر نتتبع من فُصل ومن عاد.

الفكرة الأساسية
بدلاً من حذف العميل نهائياً عند فصل خدمته، نغيّر حالته فقط. العميل يبقى في قاعدة البيانات
مع كل بياناته وتاريخه المالي، لكن لا يظهر في قوائم التحصيل اليومي ولا في شاشة العملاء النشطين.

نظام الحالات (3 حالات أساسية)

| الحالة       | الأيقونة | الوصف                              | يظهر في التحصيل؟ | يظهر في التقارير؟ |
| ------------ | -------- | ---------------------------------- | ---------------- | ----------------- |
| ACTIVE       | 🟢       | عميل نشط - الخدمة تعمل             | ✅ نعم            | ✅ نعم             |
| SUSPENDED    | 🟡       | معلّق مؤقتاً (عدم سداد / طلب عميل) | ❌ لا             | ✅ نعم             |
| DISCONNECTED | 🔴       | مفصول نهائياً - الخدمة منتهية      | ❌ لا             | ✅ نعم             |

ACTIVE ──────→ SUSPENDED        (تعليق مؤقت)
ACTIVE ──────→ DISCONNECTED     (فصل نهائي مباشر)
SUSPENDED ───→ ACTIVE           (إعادة تفعيل بعد تعليق)
SUSPENDED ───→ DISCONNECTED     (فصل نهائي بعد تعليق)
DISCONNECTED → ACTIVE           (إعادة الخدمة بعد فصل)


3.1.1 تعديل Client.kt (Entity)
إضافة 3 حقول جديدة لجدول clients:

// === حقول دورة حياة العميل (Subscriber Lifecycle) ===
val status: String = "ACTIVE",            // ACTIVE / SUSPENDED / DISCONNECTED
val statusChangedDate: Long? = null,      // تاريخ آخر تغيير حالة (timestamp)
val disconnectReason: String = ""         // سبب الفصل أو التعليق (اختياري)


Migration مطلوب في ClientDatabase.kt:

val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE clients ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE clients ADD COLUMN statusChangedDate INTEGER")
        db.execSQL("ALTER TABLE clients ADD COLUMN disconnectReason TEXT NOT NULL DEFAULT ''")
    }
}


3.1.2 تعديل ClientDao.kt
إضافة الاستعلامات التالية:

// === استعلامات دورة حياة العميل ===

// جلب العملاء النشطين فقط (للتحصيل والعرض اليومي)
@Query("SELECT * FROM clients WHERE status = 'ACTIVE' ORDER BY name ASC")
fun getActiveClients(): LiveData<List<Client>>

// جلب العملاء المعلقين فقط
@Query("SELECT * FROM clients WHERE status = 'SUSPENDED' ORDER BY name ASC")
fun getSuspendedClients(): LiveData<List<Client>>

// جلب العملاء المفصولين فقط
@Query("SELECT * FROM clients WHERE status = 'DISCONNECTED' ORDER BY statusChangedDate DESC")
fun getDisconnectedClients(): LiveData<List<Client>>

// جلب كل العملاء شامل المعلقين والمفصولين (للتقارير فقط)
@Query("SELECT * FROM clients ORDER BY name ASC")
fun getAllClientsIncludingInactive(): LiveData<List<Client>>

// تغيير حالة عميل
@


#### 3.1.2 تعديل ClientDao.kt

إضافة الاستعلامات التالية:

```kotlin
// === استعلامات دورة حياة العميل ===

// جلب العملاء النشطين فقط (للتحصيل والعرض اليومي)
@Query("SELECT * FROM clients WHERE status = 'ACTIVE' ORDER BY name ASC")
fun getActiveClients(): LiveData<List<Client>>

// جلب العملاء المعلقين فقط
@Query("SELECT * FROM clients WHERE status = 'SUSPENDED' ORDER BY name ASC")
fun getSuspendedClients(): LiveData<List<Client>>

// جلب العملاء المفصولين فقط
@Query("SELECT * FROM clients WHERE status = 'DISCONNECTED' ORDER BY statusChangedDate DESC")
fun getDisconnectedClients(): LiveData<List<Client>>

// جلب كل العملاء شامل المعلقين والمفصولين (للتقارير فقط)
@Query("SELECT * FROM clients ORDER BY name ASC")
fun getAllClientsIncludingInactive(): LiveData<List<Client>>

// تغيير حالة عميل
@Query("""
    UPDATE clients 
    SET status = :newStatus, 
        statusChangedDate = :date, 
        disconnectReason = :reason 
    WHERE id = :clientId
""")
suspend fun updateClientStatus(clientId: Int, newStatus: String, date: Long, reason: String)

// عدد المفصولين في فترة معينة (لتقرير Churn)
@Query("""
    SELECT COUNT(*) FROM clients 
    WHERE status = 'DISCONNECTED' 
    AND statusChangedDate BETWEEN :startMillis AND :endMillis
""")
suspend fun getDisconnectedCountInPeriod(startMillis: Long, endMillis: Long): Int

// عدد المُعاد تفعيلهم في فترة معينة
@Query("""
    SELECT COUNT(*) FROM clients 
    WHERE status = 'ACTIVE' 
    AND statusChangedDate BETWEEN :startMillis AND :endMillis
    AND disconnectReason != ''
""")
suspend fun getReactivatedCountInPeriod(startMillis: Long, endMillis: Long): Int

// عدد العملاء الجدد في شهر معين
@Query("""
    SELECT COUNT(*) FROM clients 
    WHERE startMonth = :month 
    AND status != 'DISCONNECTED'
""")
suspend fun getNewClientsInMonth(month: String): Int

// عدد العملاء النشطين (للإحصائيات)
@Query("SELECT COUNT(*) FROM clients WHERE status = 'ACTIVE'")
suspend fun getActiveClientsCount(): Int

// جلب المفصولين في فترة (لتفاصيل تقرير Churn)
@Query("""
    SELECT * FROM clients 
    WHERE status = 'DISCONNECTED' 
    AND statusChangedDate BETWEEN :startMillis AND :endMillis
    ORDER BY statusChangedDate DESC
""")
suspend fun getDisconnectedClientsInPeriod(startMillis: Long, endMillis: Long): List<Client>


3.1.3 تعديل ClientViewModel.kt
إضافة دوال إدارة الحالة:

// === دوال دورة حياة العميل ===

// تعليق عميل (ACTIVE → SUSPENDED)
fun suspendClient(clientId: Int, reason: String = "") = viewModelScope.launch {
    clientDao.updateClientStatus(
        clientId = clientId,
        newStatus = "SUSPENDED",
        date = System.currentTimeMillis(),
        reason = reason
    )
}

// فصل عميل نهائياً (ACTIVE/SUSPENDED → DISCONNECTED)
fun disconnectClient(clientId: Int, reason: String = "") = viewModelScope.launch {
    clientDao.updateClientStatus(
        clientId = clientId,
        newStatus = "DISCONNECTED",
        date = System.currentTimeMillis(),
        reason = reason
    )
}

// إعادة تفعيل عميل (SUSPENDED/DISCONNECTED → ACTIVE)
fun reactivateClient(clientId: Int) = viewModelScope.launch {
    clientDao.updateClientStatus(
        clientId = clientId,
        newStatus = "ACTIVE",
        date = System.currentTimeMillis(),
        reason = ""
    )
}


3.1.4 تعديل واجهة المستخدم
في ClientDetailsScreen.kt — إضافة قسم "إدارة الخدمة":

┌─────────────────────────────────────────┐
│  إدارة الخدمة                           │
│                                         │
│  الحالة الحالية: 🟢 نشط                  │
│                                         │
│  [🟡 تعليق الخدمة]  [🔴 فصل الخدمة]     │
│                                         │
│  * عند التعليق/الفصل يظهر حقل:         │
│    "سبب التعليق/الفصل (اختياري)"        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  إدارة الخدمة                           │
│                                         │
│  الحالة الحالية: 🔴 مفصول               │
│  تاريخ الفصل: 2026-02-15               │
│  السبب: عدم السداد لمدة 3 أشهر         │
│                                         │
│  [🟢 إعادة تفعيل الخدمة]                │
└─────────────────────────────────────────┘


زر "تعليق الخدمة" 🟡 → يظهر AlertDialog تأكيد + حقل سبب اختياري

زر "فصل الخدمة" 🔴 → يظهر AlertDialog تأكيد قوي + حقل سبب اختياري

زر "إعادة تفعيل" 🟢 → يظهر AlertDialog تأكيد بسيط

في ClientListScreen.kt — إضافة تبويبات فلترة الحالة:

┌──────────────────────────────────────────┐
│  [نشط ✓]  [معلّق]  [مفصول]  [الكل]     │
│                                          │
│  (الافتراضي = نشط فقط)                   │
└──────────────────────────────────────────┘


إضافة FilterChip أو Tab row فوق قائمة العملاء

الافتراضي يعرض النشطين فقط

اختيار "معلّق" يعرض المعلقين مع أيقونة 🟡

اختيار "مفصول" يعرض المفصولين مع أيقونة 🔴 وتاريخ الفصل

اختيار "الكل" يعرض الجميع مع شارة الحالة

في filterClients() بـ MainActivity.kt:

تعديل الفلتر ليستثني SUSPENDED و DISCONNECTED افتراضياً

إضافة parameter statusFilter: String = "ACTIVE" للتحكم

3.1.5 تعديل الحسابات والتقارير
العملاء SUSPENDED و DISCONNECTED لا يُحسبون في:

إحصائيات الشهر الحالي (KPI cards)

نسبة التحصيل الإجمالية

بطاقات Dashboard

التحصيل اليومي

عداد العملاء النشطين

لكن يظهرون في:

التقارير التفصيلية (مع علامة حالتهم)

تقرير التسرب (Churn Report) — القسم 3.5

شاشة العملاء عند اختيار فلتر "معلّق" أو "مفصول" أو "الكل"

قائمة مهام 3.1
 إضافة 3 حقول جديدة في Client.kt (status, statusChangedDate, disconnectReason)

 إنشاء Migration في ClientDatabase.kt

 إضافة استعلامات الحالة في ClientDao.kt

 إضافة دوال إدارة الحالة في ClientViewModel.kt

 إضافة قسم "إدارة الخدمة" في ClientDetailsScreen.kt

 إضافة تبويبات فلترة الحالة في ClientListScreen.kt

 تعديل filterClients() في MainActivity.kt

 تعديل الحسابات لاستثناء المعلقين والمفصولين

 إضافة Strings (عربي + إنجليزي)

 اختبار كل التحولات بين الحالات

3.2 تحسين تنسيق Excel 📊
3.2.1 تجميد الصف الأول (Freeze Panes)
في buildExcelXml() داخل PaymentsExportManager.kt:

إضافة كتلة WorksheetOptions قبل إغلاق </Worksheet> في كل شيت:

<WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
  <FreezePanes/>
  <FrozenNoSplit/>
  <SplitHorizontal>1</SplitHorizontal>
  <TopRowBottomPane>1</TopRowBottomPane>
  <ActivePane>2</ActivePane>
</WorksheetOptions>


يُطبق على 3 شيتات: Dashboard, Client Details, Transactions

3.2.2 عرض الأعمدة التلقائي (Auto-Width)
إضافة تعريف أعمدة بعد <Table> وقبل أول <Row>:

شيت Client Details:

<Column ss:AutoFitWidth="1" ss:Width="120"/>  <!-- Client Name -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Sub# -->
<Column ss:AutoFitWidth="1" ss:Width="100"/>  <!-- Building -->
<Column ss:AutoFitWidth="1" ss:Width="50"/>   <!-- Room -->
<Column ss:AutoFitWidth="1" ss:Width="70"/>   <!-- Package -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Amount -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Paid -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Remaining -->
<Column ss:AutoFitWidth="1" ss:Width="70"/>   <!-- Status -->
<Column ss:AutoFitWidth="1" ss:Width="100"/>  <!-- Last Payment -->
<Column ss:AutoFitWidth="1" ss:Width="150"/>  <!-- Notes -->


شيت Transactions:

<Column ss:AutoFitWidth="1" ss:Width="70"/>   <!-- Month -->
<Column ss:AutoFitWidth="1" ss:Width="110"/>  <!-- Client -->
<Column ss:AutoFitWidth="1" ss:Width="70"/>   <!-- Sub# -->
<Column ss:AutoFitWidth="1" ss:Width="100"/>  <!-- Building -->
<Column ss:AutoFitWidth="1" ss:Width="50"/>   <!-- Room -->
<Column ss:AutoFitWidth="1" ss:Width="120"/>  <!-- Date -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Amount -->
<Column ss:AutoFitWidth="1" ss:Width="80"/>   <!-- Type -->
<Column ss:AutoFitWidth="1" ss:Width="150"/>  <!-- Notes -->


نفس الشيء يُطبق على DailyCollectionExportManager.kt

قائمة مهام 3.2
 إضافة FrozenRows لشيت Dashboard في PaymentsExportManager

 إضافة FrozenRows لشيت Client Details

 إضافة FrozenRows لشيت Transactions

 إضافة FrozenRows في DailyCollectionExportManager

 إضافة Column widths لكل الشيتات في PaymentsExportManager

 إضافة Column widths في DailyCollectionExportManager

 اختبار الفتح في Microsoft Excel

 اختبار الفتح في Google Sheets

3.3 QR Code في التقارير 📱
الهدف
إضافة QR Code في كل تقرير (PDF + Excel) يحتوي معلومات التقرير للتحقق السريع.

محتوى QR Code

ProNetwork Report
Title: Monthly Report: 2026-02
Generated: 2026-02-18 14:30
Collection Rate: 85.5%
Total: 45,000 SAR
Report ID: A3F8B2C1


3.3.1 إضافة مكتبة ZXing
في build.gradle.kts (Module: app):

dependencies {
    implementation("com.google.zxing:core:3.5.3")
}


#### 3.3.2 إنشاء QrCodeGenerator.kt (ملف جديد)

**المسار**: `app/src/main/java/com/pronetwork/app/utils/QrCodeGenerator.kt`

```kotlin
package com.pronetwork.app.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object QrCodeGenerator {

    fun generateBitmap(content: String, size: Int = 200): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun buildReportContent(
        reportTitle: String,
        generatedDate: String,
        collectionRate: Double,
        totalCollected: Double
    ): String {
        val reportId = UUID.randomUUID().toString().take(8).uppercase()
        return buildString {
            appendLine("ProNetwork Report")
            appendLine("Title: $reportTitle")
            appendLine("Date: $generatedDate")
            appendLine("Rate: ${"%.1f".format(collectionRate)}%")
            appendLine("Total: ${"%.2f".format(totalCollected)} SAR")
            appendLine("ID: $reportId")
        }
    }
}



3.3.3 تعديل PDF Builder
في buildPdf() داخل PaymentsExportManager.kt:

بعد رسم العنوان مباشرة، إضافة QR Code في الزاوية العلوية اليمنى:

// === QR Code ===
val qrContent = QrCodeGenerator.buildReportContent(
    reportTitle = data.reportTitle,
    generatedDate = data.generatedDate,
    collectionRate = data.grandCollectionRate,
    totalCollected = data.grandTotalCollected
)
val qrBitmap = QrCodeGenerator.generateBitmap(qrContent, 100)
canvas.drawBitmap(qrBitmap, pageWidth - margin - 100f, 15f, null)


3.3.4 تعديل Excel Builder
في buildExcelXml() داخل PaymentsExportManager.kt:

إضافة سطر يحتوي رابط QR مشفر بعد عنوان التقرير:

<Row>
  <Cell ss:StyleID="SubTitle">
    <Data ss:Type="String">Report ID: [UUID] | Verify at: pronetwork.app/verify/[UUID]</Data>
  </Cell>
</Row>


3.3.5 تطبيق نفس التعديلات على DailyCollectionExportManager.kt
نفس QR Code في PDF اليومي

نفس سطر Report ID في Excel اليومي

قائمة مهام 3.3
 إضافة مكتبة ZXing في build.gradle.kts

 إنشاء QrCodeGenerator.kt

 إضافة QR Code في PDF داخل PaymentsExportManager

 إضافة Report ID في Excel داخل PaymentsExportManager

 إضافة QR Code في PDF داخل DailyCollectionExportManager

 إضافة Report ID في Excel داخل DailyCollectionExportManager

 اختبار قراءة QR Code بالجوال

3.4 تقرير الإيرادات حسب الباقة (Revenue by Package) 📦
الهدف
شيت جديد في تقارير Excel + قسم جديد في PDF يعرض أداء كل باقة إنترنت بشكل منفصل.

البيانات المطلوبة لكل باقة

| الحقل               | الوصف                       |
| ------------------- | --------------------------- |
| اسم الباقة          | مثل: 5Mbps, 10Mbps, 20Mbps  |
| عدد العملاء         | كم عميل مشترك في هذه الباقة |
| الإيراد المتوقع     | عدد العملاء × سعر الباقة    |
| الإيراد المحصّل     | مجموع المدفوع فعلياً        |
| المتبقي             | المتوقع - المحصّل           |
| نسبة التحصيل        | (المحصّل / المتوقع) × 100   |
| عدد المدفوع بالكامل | PAID count                  |
| عدد الجزئي          | PARTIAL count               |
| عدد المُسوَّى       | SETTLED count               |
| عدد غير المدفوع     | UNPAID count                |

3.4.1 إضافة Data Model
في PaymentsExportManager.kt — إضافة data class جديد:

data class PackageRevenueSummary(
    val packageName: String,
    val clientCount: Int,
    val totalExpected: Double,
    val totalCollected: Double,
    val totalRemaining: Double,
    val collectionRate: Double,
    val paidCount: Int,
    val partialCount: Int,
    val settledCount: Int,
    val unpaidCount: Int
)


3.4.2 إضافة دالة جمع البيانات
في PaymentsExportManager.kt — إضافة دالة بعد gatherMonthData():

private fun gatherPackageRevenue(monthsData: List<MonthData>): List<PackageRevenueSummary> {
    val allClients = monthsData.flatMap { it.clients }
    return allClients.groupBy { it.packageType }.map { (pkg, clients) ->
        val expected = clients.sumOf { it.monthlyAmount }
        val collected = clients.sumOf { it.totalPaid }
        val remaining = (expected - collected).coerceAtLeast(0.0)
        val rate = if (expected > 0) (collected / expected) * 100 else 0.0
        PackageRevenueSummary(
            packageName = pkg,
            clientCount = clients.size,
            totalExpected = expected,
            totalCollected = collected,
            totalRemaining = remaining,
            collectionRate = rate,
            paidCount = clients.count { it.status == "PAID" },
            partialCount = clients.count { it.status == "PARTIAL" },
            settledCount = clients.count { it.status == "SETTLED" },
            unpaidCount = clients.count { it.status == "UNPAID" }
        )
    }.sortedByDescending { it.totalCollected }
}


3.4.3 إضافة التوصيات الذكية

private fun generatePackageInsights(packages: List<PackageRevenueSummary>): List<SmartInsight> {
    val insights = mutableListOf<SmartInsight>()
    
    // أضعف باقة تحصيلاً
    val weakest = packages.minByOrNull { it.collectionRate }
    if (weakest != null && weakest.collectionRate < 70) {
        insights.add(SmartInsight(
            "⚠️", "WARNING",
            "Weak Package: ${weakest.packageName}",
            "Collection rate only ${"%.1f".format(weakest.collectionRate)}% — ${weakest.unpaidCount} unpaid clients"
        ))
    }
    
    // أقوى باقة تحصيلاً
    val strongest = packages.maxByOrNull { it.collectionRate }
    if (strongest != null && strongest.collectionRate >= 90) {
        insights.add(SmartInsight(
            "🏆", "SUCCESS",
            "Top Package: ${strongest.packageName}",
            "Excellent collection at ${"%.1f".format(strongest.collectionRate)}%"
        ))
    }
    
    // باقة فيها أكثر عملاء غير مدفوعين
    val mostUnpaid = packages.maxByOrNull { it.unpaidCount }
    if (mostUnpaid != null && mostUnpaid.unpaidCount > 0) {
        insights.add(SmartInsight(
            "🔴", "CRITICAL",
            "${mostUnpaid.unpaidCount} Unpaid in ${mostUnpaid.packageName}",
            "Highest unpaid count — follow up immediately"
        ))
    }
    
    return insights
}


3.4.4 تعديل Excel Builder — إضافة شيت "Revenue by Package"
في buildExcelXml() — إضافة شيت جديد بعد شيت Dashboard:

┌───────────────────────────────────────────────────────────────────────┐
│                    Revenue by Package — 2026-02                       │
├──────────┬────────┬──────────┬──────────┬──────────┬────────┬────────┤
│ Package  │Clients │ Expected │Collected │Remaining │  Rate  │ Status │
├──────────┼────────┼──────────┼──────────┼──────────┼────────┼────────┤
│ 20Mbps   │   45   │ 6,750    │ 6,200    │   550    │ 91.9%  │ 🟢    │
│ 10Mbps   │   30   │ 3,000    │ 2,400    │   600    │ 80.0%  │ 🟢    │
│ 5Mbps    │   25   │ 1,250    │   800    │   450    │ 64.0%  │ 🟡    │
│ 50Mbps   │   10   │ 2,000    │   900    │ 1,100    │ 45.0%  │ 🔴    │
├──────────┼────────┼──────────┼──────────┼──────────┼────────┼────────┤
│ TOTAL    │  110   │ 13,000   │ 10,300   │ 2,700    │ 79.2%  │        │
├──────────┴────────┴──────────┴──────────┴──────────┴────────┴────────┤
│ ⚠️ Weak Package: 50Mbps — Rate only 45.0%, 4 unpaid clients         │
│ 🏆 Top Package: 20Mbps — Excellent collection at 91.9%              │
└──────────────────────────────────────────────────────────────────────┘


3.4.5 تعديل PDF Builder — إضافة قسم "Revenue by Package"
بعد قسم "Collection by Building" في buildPdf():

جدول بنفس الأعمدة أعلاه

ألوان حسب نسبة التحصيل (أخضر ≥80% / أصفر ≥50% / أحمر <50%)

التوصيات الذكية أسفل الجدول

قائمة مهام 3.4
 إضافة PackageRevenueSummary data class

 إضافة gatherPackageRevenue() function

 إضافة generatePackageInsights() function

 إضافة شيت "Revenue by Package" في Excel builder

 إضافة قسم "Revenue by Package" في PDF builder

 إضافة نفس القسم في DailyCollectionExportManager (اختياري)

 اختبار مع باقات مختلفة

3.5 تقرير المقارنة الفترية + تحليل التسرب (Period Comparison + Churn) 📈
الهدف
إضافة مقارنات احترافية متعددة الفترات + تقرير تسرب العملاء.

3.5.1 المقارنة الفترية (Period Comparison)
3 أنواع مقارنة جديدة:

| نوع المقارنة                         | المثال                     | الوصف          |
| ------------------------------------ | -------------------------- | -------------- |
| شهر vs شهر سابق                      | فبراير 2026 vs يناير 2026  | موجود حالياً ✅ |
| شهر vs نفس الشهر السنة الماضية (YoY) | فبراير 2026 vs فبراير 2025 | جديد           |
| ربع vs ربع سابق (QoQ)                | Q1-2026 vs Q4-2025         | جديد           |


إضافة Data Model:

data class PeriodComparison(
    val currentPeriodLabel: String,     // "2026-02" أو "Q1-2026"
    val previousPeriodLabel: String,    // "2025-02" أو "Q4-2025"
    val currentCollected: Double,
    val previousCollected: Double,
    val currentRate: Double,
    val previousRate: Double,
    val currentClients: Int,
    val previousClients: Int,
    val revenueGrowth: Double,          // نسبة النمو في الإيراد (%)
    val rateChange: Double,             // التغير في نسبة التحصيل (%)
    val clientGrowth: Double,           // نسبة نمو العملاء (%)
    val comparisonType: String          // "MoM" / "YoY" / "QoQ"
)


**دالة حساب المقارنة:**

```kotlin
private fun calculateComparison(
    currentMonth: MonthData,
    previousMonth: MonthData,
    type: String
): PeriodComparison {
    val revenueGrowth = if (previousMonth.totalCollected > 0)
        ((currentMonth.totalCollected - previousMonth.totalCollected) / previousMonth.totalCollected) * 100
    else 0.0

    val rateChange = currentMonth.collectionRate - previousMonth.collectionRate

    val clientGrowth = if (previousMonth.totalClients > 0)
        ((currentMonth.totalClients - previousMonth.totalClients).toDouble() / previousMonth.totalClients) * 100
    else 0.0

    return PeriodComparison(
        currentPeriodLabel = currentMonth.month,
        previousPeriodLabel = previousMonth.month,
        currentCollected = currentMonth.totalCollected,
        previousCollected = previousMonth.totalCollected,
        currentRate = currentMonth.collectionRate,
        previousRate = previousMonth.collectionRate,
        currentClients = currentMonth.totalClients,
        previousClients = previousMonth.totalClients,
        revenueGrowth = revenueGrowth,
        rateChange = rateChange,
        clientGrowth = clientGrowth,
        comparisonType = type
    )
}


دالة حساب الشهر المقابل من السنة الماضية (YoY):

private fun getYearAgoMonth(month: String): String {
    val parts = month.split("-")
    val year = parts.toInt() - 1
    return String.format("%04d-%s", year, parts)[1]
}


دالة حساب الربع:

private fun getQuarterMonths(month: String): List<String> {
    val parts = month.split("-")
    val year = parts.toInt()
    val m = parts.toInt()[1]
    val quarterStart = when (m) {
        in 1..3 -> 1
        in 4..6 -> 4
        in 7..9 -> 7
        else -> 10
    }
    return (0..2).map { String.format("%04d-%02d", year, quarterStart + it) }
}

private fun getPreviousQuarterMonths(month: String): List<String> {
    val parts = month.split("-")
    val year = parts.toInt()
    val m = parts.toInt()[1]
    val quarterStart = when (m) {
        in 1..3 -> 10  // Q4 السنة الماضية
        in 4..6 -> 1
        in 7..9 -> 4
        else -> 7
    }
    val qYear = if (m in 1..3) year - 1 else year
    return (0..2).map { String.format("%04d-%02d", qYear, quarterStart + it) }
}


شكل المقارنة في التقرير (Excel + PDF):

┌─────────────────────────────────────────────────────────────┐
│                   Period Comparison                          │
├─────────────────┬──────────────┬──────────────┬─────────────┤
│ Metric          │ Current      │ Previous     │ Change      │
├─────────────────┼──────────────┼──────────────┼─────────────┤
│ Period          │ 2026-02      │ 2025-02      │ YoY         │
│ Collected       │ 45,000 SAR   │ 38,000 SAR   │ 📈 +18.4%  │
│ Collection Rate │ 85.5%        │ 78.2%        │ 📈 +7.3%   │
│ Active Clients  │ 110          │ 95           │ 📈 +15.8%  │
├─────────────────┼──────────────┼──────────────┼─────────────┤
│ Period          │ Q1-2026      │ Q4-2025      │ QoQ         │
│ Collected       │ 130,000 SAR  │ 125,000 SAR  │ 📈 +4.0%   │
│ Collection Rate │ 83.2%        │ 81.0%        │ 📈 +2.2%   │
│ Active Clients  │ 110          │ 105          │ 📈 +4.8%   │
└─────────────────┴──────────────┴──────────────┴─────────────┘

📈 = أخضر (نمو)    📉 = أحمر (انخفاض)    ➡️ = رمادي (ثبات)


3.5.2 تقرير تحليل التسرب (Churn Analysis)
يعتمد على: القسم 3.1 (دورة حياة العميل) — يجب تنفيذ 3.1 أولاً



Data Model:


data class ChurnReport(
    val periodLabel: String,           // "2026-02"
    val activeClientsStart: Int,       // عدد النشطين بداية الفترة
    val activeClientsEnd: Int,         // عدد النشطين نهاية الفترة
    val newClients: Int,               // عملاء جدد خلال الفترة
    val disconnectedClients: Int,      // عملاء تم فصلهم خلال الفترة
    val suspendedClients: Int,         // عملاء تم تعليقهم خلال الفترة
    val reactivatedClients: Int,       // عملاء أُعيد تفعيلهم خلال الفترة
    val netGrowth: Int,                // صافي النمو = جدد - مفصولين
    val churnRate: Double,             // معدل التسرب (%) = مفصولين / نشطين بداية الفترة
    val growthRate: Double,            // معدل النمو (%) = جدد / نشطين بداية الفترة
    val disconnectedDetails: List<ChurnClientDetail>  // تفاصيل المفصولين
)

data class ChurnClientDetail(
    val clientName: String,
    val subscriptionNumber: String,
    val buildingName: String,
    val packageType: String,
    val disconnectDate: String,
    val reason: String,
    val totalRevenue: Double           // إجمالي ما دفعه العميل خلال حياته
)


شكل تقرير Churn في التقرير (Excel + PDF):

┌─────────────────────────────────────────────────────────────┐
│                 Churn Analysis — 2026-02                     │
├─────────────────────────┬───────────────────────────────────┤
│ Active (Start of Month) │ 105                               │
│ + New Clients           │ +8                                │
│ + Reactivated           │ +2                                │
│ - Suspended             │ -3                                │
│ - Disconnected          │ -2                                │
│ = Active (End of Month) │ 110                               │
├─────────────────────────┼───────────────────────────────────┤
│ Net Growth              │ +5 (📈 +4.8%)                     │
│ Churn Rate              │ 1.9% (🟢 Healthy < 5%)           │
│ Growth Rate             │ 7.6%                              │
├─────────────────────────┴───────────────────────────────────┤
│                                                             │
│ Disconnected Clients Details:                               │
│ ┌──────────┬────────┬──────────┬───────────────────────┐   │
│ │ Client   │ Pkg    │ Date     │ Reason                │   │
│ ├──────────┼────────┼──────────┼───────────────────────┤   │
│ │ أحمد     │ 10Mbps │ 02-10    │ عدم السداد 3 أشهر    │   │
│ │ محمد     │ 5Mbps  │ 02-15    │ طلب العميل            │   │
│ └──────────┴────────┴──────────┴───────────────────────┘   │
│                                                             │
│ 🟢 Churn Rate < 3%  = Excellent                            │
│ 🟡 Churn Rate 3-5%  = Acceptable                           │
│ 🔴 Churn Rate > 5%  = Critical — needs immediate action    │
└─────────────────────────────────────────────────────────────┘


3.5.3 تعديل Excel Builder
في buildExcelXml() — إضافة شيتين جديدين:

شيت "Period Comparison": جدول المقارنة الفترية (MoM + YoY + QoQ)

شيت "Churn Analysis": تقرير التسرب مع التفاصيل

3.5.4 تعديل PDF Builder
في buildPdf() — إضافة قسمين:

قسم "Period Comparison": بعد Monthly Comparison الموجود حالياً

قسم "Churn Analysis": بعد Smart Insights

3.5.5 إضافة Churn Insights في Smart Insights

// في generateSmartInsights():

// Churn Rate Alert
if (churnReport != null) {
    val churnInsight = when {
        churnReport.churnRate > 5 -> SmartInsight(
            "🔴", "CRITICAL", "High Churn: ${"%.1f".format(churnReport.churnRate)}%",
            "${churnReport.disconnectedClients} clients lost this month — urgent retention needed"
        )
        churnReport.churnRate > 3 -> SmartInsight(
            "🟡", "WARNING", "Moderate Churn: ${"%.1f".format(churnReport.churnRate)}%",
            "${churnReport.disconnectedClients} clients disconnected — monitor closely"
        )
        else -> SmartInsight(
            "🟢", "SUCCESS", "Low Churn: ${"%.1f".format(churnReport.churnRate)}%",
            "Customer retention is healthy"
        )
    }
    insights.add(churnInsight)

    // Net Growth
    if (churnReport.netGrowth > 0) {
        insights.add(SmartInsight(
            "📈", "SUCCESS", "Net Growth: +${churnReport.netGrowth} clients",
            "Business is growing — ${churnReport.newClients} new vs ${churnReport.disconnectedClients} lost"
        ))
    } else if (churnReport.netGrowth < 0) {
        insights.add(SmartInsight(
            "📉", "CRITICAL", "Net Loss: ${churnReport.netGrowth} clients",
            "Losing more clients than gaining — review pricing and service quality"
        ))
    }
}


قائمة مهام 3.5
 إضافة PeriodComparison data class

 إضافة ChurnReport و ChurnClientDetail data classes

 إضافة دوال حساب المقارنة (MoM, YoY, QoQ)

 إضافة دوال حساب Churn من ClientDao

 إضافة شيت "Period Comparison" في Excel builder

 إضافة شيت "Churn Analysis" في Excel builder

 إضافة قسم Period Comparison في PDF builder

 إضافة قسم Churn Analysis في PDF builder

 إضافة Churn insights في generateSmartInsights()

 إضافة Strings (عربي + إنجليزي)

 اختبار مع بيانات فعلية

ملخص المرحلة 3 — ترتيب التنفيذ

الخطوة 1: [3.1] دورة حياة العميل ← الأساس لكل شي بعده
    ↓
الخطوة 2: [3.2] تحسين Excel (Freeze + Width) ← سريع ومستقل
    ↓
الخطوة 3: [3.3] QR Code ← سريع ومستقل
    ↓
الخطوة 4: [3.4] تقرير الباقات ← يعتمد على بيانات موجودة
    ↓
الخطوة 5: [3.5] المقارنة + Churn ← يعتمد على 3.1


| الملف                           | نوع التغيير                                                |
| ------------------------------- | ---------------------------------------------------------- |
| Client.kt                       | تعديل — إضافة 3 حقول                                       |
| ClientDao.kt                    | تعديل — إضافة استعلامات                                    |
| ClientDatabase.kt               | تعديل — إضافة Migration                                    |
| ClientViewModel.kt              | تعديل — إضافة دوال الحالة                                  |
| ClientDetailsScreen.kt          | تعديل — إضافة قسم إدارة الخدمة                             |
| ClientListScreen.kt             | تعديل — إضافة تبويبات الفلترة                              |
| MainActivity.kt                 | تعديل — تعديل filterClients + فلتر الحالة                  |
| PaymentsExportManager.kt        | تعديل — Freeze + Width + QR + Package + Comparison + Churn |
| DailyCollectionExportManager.kt | تعديل — Freeze + Width + QR                                |
| QrCodeGenerator.kt              | جديد — مولد QR Code                                        |
| build.gradle.kts                | تعديل — إضافة مكتبة ZXing                                  |
| strings.xml                     | تعديل — نصوص جديدة (عربي + إنجليزي)                        |




