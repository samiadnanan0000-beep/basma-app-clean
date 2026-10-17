# خطة معالجة وتحديث شارة تفعيل الحساب اللحظية (Real-time Account Activation)

تهدف هذه الخطة إلى المعالجة الجذرية لعدم ظهور وتحديث شارة توثيق الحساب (`isAccountActivated`) فور تفعيل الحساب من قِبل الإدارة، لضمان استجابة وانعكاس فوري على واجهات المستخدم دون الحاجة لإعادة التشغيل أو تسجيل الخروج.

---

## 1. تصحيح نموذج بيانات المستخدم (`UserProfile` في `Models.kt`)
- **المشكلة البرمجية:** محول Firebase Firestore في بيئة Java/Kotlin يقوم بحذف بادئة `is` تلقائياً من أسماء الخصائص المنطقية (Booleans) وفقاً لمعايير JavaBeans (تحويل `isAccountActivated` إلى `accountActivated`)، مما يؤدي إلى عدم مطابقة الحقل المخزن في Firestore (`isAccountActivated`) وسقوط قيمته إلى `false`.
- **الحل المقترح:**
  - إضافة التوجيهات الصريحة `@get:PropertyName("isAccountActivated")` و `@set:PropertyName("isAccountActivated")` على المتغير `var isAccountActivated: Boolean = false` داخل كلاس `UserProfile`.
  - استيراد `com.google.firebase.firestore.PropertyName`.

---

## 2. تفعيل الاستماع اللحظي لمستند المستخدم (`Real-time Snapshot Listener` في `MainViewModel` و `UserRepository`)
- التأكد من بقاء مستمع حي دائم (`addSnapshotListener`) موصولاً بمستند المستخدم الحالي `users/{uid}`.
- توفير دالة مخصصة `listenToUserProfile(userId: String)` في `MainViewModel` تقوم بإلغاء أي مهمة سابقة وبدء استماع فوري لأي تغيير يطرأ على وثيقة المستخدم في Firestore وبثه مباشرة إلى `_userProfile: MutableStateFlow<UserProfile?>`.
- التأكد من استدعاء `listenToUserProfile(user.uid)` فور تسجيل الدخول أو استعادة الجلسة في `onUserSignedIn` و `signInWithEmailAndPassword` و `registerWithEmail`.

---

## 3. التحقق وتحديث شاشات العرض التفاعلية (`UI Recomposition`)
- في `ClientHomeScreen.kt` و `SettingsScreen.kt` و `ProfileScreen.kt`:
  - مراقبة `val userProfile by mainViewModel.userProfile.collectAsState()` لحظياً.
  - استخراج المتغير: `val isActivated = userProfile?.isAccountActivated == true || mainViewModel.isSuperAdmin()`.
  - التأكد من إظهار أيقونة التوثيق الخضراء/الزرقاء `VerifiedCheckIcon` والشارة المضيئة `ModernVerifiedPillBadge` مباشرة بجوار اسم المستخدم فور تحول القيمة إلى `true`.

---

## 4. فحص وضمان إجراء التفعيل في لوحة الإدارة (`Admin Activation Action`)
- في `UserRepository.kt` داخل دالة `toggleAccountActivation(targetUid: String, isActivated: Boolean)`:
  - استخدام `usersRef.document(targetUid).set(mapOf("isAccountActivated" to isActivated), SetOptions.merge())` بدلاً من مجرد update لضمان كتابة وتحديث الحقل بدقة متطابقة في Firestore حتى لو لم يكن الحقل موجوداً مسبقاً في الوثيقة.
  - إطلاق إشعار Toast تأكيدي وفوري في `AdminDashboardScreen.kt`.

---

## 5. التحقق والاختبار (Verification)
- تشغيل `compile_applet` للتأكد من خلو المشروع تماماً من أي أخطاء تجميع أو تعارضات.
