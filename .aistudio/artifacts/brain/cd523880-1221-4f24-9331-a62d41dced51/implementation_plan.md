# خطة العمل البرمجية: إصلاح مسار رفع وعرض ومعاينة صور إشعارات الدفع والمرفقات

تهدف هذه الخطة إلى حل مشكلة عدم فتح أو تحميل صور إشعارات الدفع (سواء الدفعة المقدمة أو النهائية) وصور مرفقات الطلبات على شاشات العميل والإدارة، عبر ضمان رفع الصور الحقيقية مباشرة إلى **Firebase Storage** وحفظ الروابط السحابية الدائمة (`https://...`) في Firestore بدلاً من المسارات المحلية (`content://`)، مع توفير عارض صور متطور يدعم التكبير والتنزيل.

---

## 1. إضافة مكتبة وتكوين Firebase Storage
- إضافة `firebase-storage` في `gradle/libs.versions.toml`:
  ```toml
  firebase-storage = { group = "com.google.firebase", name = "firebase-storage" }
  ```
- إضافة الاعتماد في `app/build.gradle.kts`:
  ```kotlin
  implementation(libs.firebase.storage)
  ```
- إنشاء وحدة مساعدة مركزية `FirebaseStorageHelper.kt` لإدارة عمليات الرفع السحابي:
  - دالة `uploadReceiptImage(context, uri, userId)` لحفظ الإشعارات في: `receipts/${System.currentTimeMillis()}_${userId}.jpg`.
  - دالة `uploadOrderAttachment(context, uri, userId)` لحفظ المرفقات في: `order_attachments/${System.currentTimeMillis()}_${userId}.jpg`.
  - استخدام `contentResolver.openInputStream(uri)?.use { it.readBytes() }` مع `putBytes` و `.await()` لاستخراج رابط التنزيل المباشر `downloadUrl.toString()`.

---

## 2. تحديث شاشات ونوافذ الرفع (`AdvancePaymentDialog` و `NewOrderScreen`)
- **في نافذة الدفع (`AdvancePaymentDialog.kt`):**
  - عند اختيار المستخدم لصورة الإشعار عبر `PickVisualMedia`:
    - إطلاق مهمة الرفع في Coroutine وتفعيل حالة `isUploading = true`.
    - عرض شريط تقدم `LinearProgressIndicator` مع نص توضيحي: *"جارٍ رفع صورة الإشعار للسحابة..."*.
    - تعطيل زر التأكيد أثناء الرفع لمنع إرسال مسار فارغ أو محلي.
    - عند اكتمال الرفع: تخزين الرابط السحابي الدائم `https://...` في `receiptImageUrl`.
    - التأكد التام من أن دالتي `submitAdvancePayment` و `submitFinalPayment` تستقبلان الرابط السحابي فقط.
- **في شاشة إنشاء طلب جديد (`NewOrderScreen.kt`):**
  - عند اختيار صورة مرفقة/مرجعية، إطلاق الرفع فورياً وعرض مؤشر `LinearProgressIndicator` مع *"جارٍ رفع الصورة للسحابة..."*.
  - تعطيل زر إرسال الطلب حتى اكتمال الرفع وحفظ الرابط السحابي في `referenceAttachmentUrl`.

---

## 3. تصحيح عرض الصور في الواجهات ومعالجة حالات Coil
- بناء مكون مخصص `SafeAsyncImage.kt` يعتمد على `SubcomposeAsyncImage`:
  - **التحقق من الرابط:** إذا كان الرابط فارغاً أو لا يبدأ بـ `http`، عرض بطاقة أنيقة وبديلة: *"لا توجد صورة مرفقة أو الرابط غير صالح"*.
  - **حالة التحميل (`loading`):** عرض `CircularProgressIndicator` متناسق وأنيق في مركز الصورة.
  - **حالة الخطأ (`error`):** عرض أيقونة خطأ مع رسالة تنبيهية وزر لإعادة المحاولة.
  - ضبط أبعاد مناسبة وثابتة مع `ContentScale.Crop` أو `ContentScale.Fit`.

---

## 4. نافذة المعاينة والتكبير بالحجم الكامل (`FullScreenImagePreviewDialog.kt`)
- إنشاء نافذة حوارية ملء الشاشة قابلة لإعادة الاستخدام في كامل التطبيق:
  - **ميزة التكبير والتصغير باللمس (Pinch-to-zoom):** باستخدام `detectTransformGestures` وتعديل `graphicsLayer(scaleX, scaleY, translationX, translationY)` مع دعم النقر المزدوج (Double Tap) لإعادة الضبط.
  - **زر تنزيل وحفظ الصورة:** استخدام نظام أندرويد القياسي `DownloadManager` لتنزيل وحفظ الصورة في مجلد الصور (`Pictures/Basma`) مع إشعار للمستخدم.
  - **زر إغلاق دائري واضح وسهل الوصول.**
- دمج هذه النافذة في:
  - لوحة تحكم الإدارة `AdminDashboardScreen` لمعاينة إشعارات الدفع ومرفقات الطلبات.
  - شاشة تتبع الطلبات `OrderTrackingScreen` للمعاينة من قِبل العميل.
  - لوحة المصمم `DesignerHubScreen` لمعاينة المرفقات والتصاميم المعتمدة.

---

## 5. الاختبار والتحقق البرمجي (Verification)
- تشغيل أداة `compile_applet` للتحقق من نجاح التجميع وخلو التعديلات من أي أخطاء أو تعارضات.
