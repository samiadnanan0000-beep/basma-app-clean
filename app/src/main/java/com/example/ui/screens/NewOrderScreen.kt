package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OrderItem
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OrderViewModel
import com.example.util.WhatsAppHelper

@Composable
fun NewOrderScreen(
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    onOrderSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by mainViewModel.userProfile.collectAsState()
    val isSubmitting by orderViewModel.isSubmitting.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()

    // 1. Flexible text fields
    var designTypeInput by remember { mutableStateOf("") }
    var detailedSpecsInput by remember { mutableStateOf("") }
    var widthInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var contactPhoneInput by remember { mutableStateOf(userProfile?.phone.orEmpty()) }
    var referenceAttachmentUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingAttachment by remember { mutableStateOf(false) }
    var previewAttachmentUrl by remember { mutableStateOf<String?>(null) }

    var showActivationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedOrderNum by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            validationError = null
            isUploadingAttachment = true
            coroutineScope.launch {
                val result = com.example.util.FirebaseStorageHelper.uploadOrderAttachment(
                    context = context,
                    uri = uri,
                    userId = userProfile?.uid.orEmpty()
                )
                result.fold(
                    onSuccess = { downloadUrl ->
                        referenceAttachmentUrl = downloadUrl
                        isUploadingAttachment = false
                        Toast.makeText(context, "تم رفع المرفق للسحابة بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        isUploadingAttachment = false
                        Toast.makeText(context, "فشل رفع المرفق للسحابة: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val finalAttachment = referenceAttachmentUrl.trim().let { if (it.startsWith("http", ignoreCase = true)) it else "" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "طلب تصميم وعمل جديد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اكتب تفاصيل طلبك بحرية لتصلك التسعيرة المعتمدة من الإدارة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Section 1: Design Type & Title (Flexible Text)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Title,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عنوان / نوع التصميم المطلوب *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = designTypeInput,
                        onValueChange = {
                            designTypeInput = it
                            validationError = null
                        },
                        placeholder = { Text("مثال: تصميم هوية بصرية كاملة، لوحة واجهة محل كلادنج، بنر سوشيال ميديا...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("design_type_input")
                    )
                }
            }
        }

        // Section 2: Detailed Specifications (Multi-line)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مواصفات وتفاصيل التصميم المطلوب *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اكتب هنا كافة المتطلبات والشروط الفنية، النصوص المكتوبة، الألوان المفضلة، ونوع النشاط بدقة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = detailedSpecsInput,
                        onValueChange = {
                            detailedSpecsInput = it
                            validationError = null
                        },
                        placeholder = { Text("اكتب تفاصيل طلبك هنا بالكامل...") },
                        minLines = 4,
                        maxLines = 10,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detailed_specs_input")
                    )
                }
            }
        }

        // Section 3: Dimensions & Contact (Optional Complementary Fields)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الأبعاد والمقاسات التقديرية (اختياري)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { widthInput = it },
                            label = { Text("العرض (متر)") },
                            placeholder = { Text("مثال: 3.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("الارتفاع (متر)") },
                            placeholder = { Text("مثال: 1.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "رقم هاتف التواصل للطلب:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = contactPhoneInput,
                        onValueChange = { contactPhoneInput = it },
                        placeholder = { Text("771051177") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 4: Attachments & Reference Photos
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "المرفقات أو نماذج توضيحية (اختياري)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (finalAttachment.isNotBlank()) "تم اختيار مرفق ✓" else "اختيار صورة أو شعار من المعرض")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = referenceAttachmentUrl,
                        onValueChange = { referenceAttachmentUrl = it },
                        placeholder = { Text("أو أدخل رابط المرفق المرجعي (Drive / رابط خارجي)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Uploading progress indicator
                    if (isUploadingAttachment) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "جارٍ رفع الصورة للسحابة...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (finalAttachment.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { previewAttachmentUrl = finalAttachment }
                        ) {
                            com.example.ui.components.SafeAsyncImage(
                                imageUrl = finalAttachment,
                                contentDescription = "معاينة المرفق",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                            ) {
                                Text(
                                    text = "🔍 اضغط للتكبير الكامل",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                    referenceAttachmentUrl = ""
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .size(26.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Info notice card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "بعد إرسال الطلب، تقوم الإدارة بمراجعة المواصفات بدقة وتحديد التسعيرة الرسمية وموعد التسليم لاعتمادها من طرفك.",
                        fontSize = 12.sp,
                        color = Color(0xFF15803D),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (validationError != null) {
            item {
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (designTypeInput.isBlank()) {
                        validationError = "يرجى كتابة عنوان أو نوع التصميم المطلوب"
                        return@Button
                    }
                    if (detailedSpecsInput.isBlank()) {
                        validationError = "يرجى كتابة مواصفات وتفاصيل التصميم المطلوب"
                        return@Button
                    }

                    // Check Account Activation:
                    val isActivated = userProfile?.isAccountActivated == true || mainViewModel.isSuperAdmin()
                    if (!isActivated) {
                        showActivationDialog = true
                        return@Button
                    }

                    validationError = null
                    val order = OrderItem(
                        clientId = userProfile?.uid.orEmpty(),
                        clientName = userProfile?.name.orEmpty().ifBlank { "عميل بصمة" },
                        clientPhone = contactPhoneInput.ifBlank { userProfile?.phone.orEmpty() },
                        clientEmail = userProfile?.email.orEmpty(),
                        designType = designTypeInput.trim(),
                        detailedSpecs = detailedSpecsInput.trim(),
                        width = widthInput.toDoubleOrNull() ?: 0.0,
                        height = heightInput.toDoubleOrNull() ?: 0.0,
                        referenceAttachmentUrl = finalAttachment,
                        contactDetails = contactPhoneInput
                    )

                    orderViewModel.submitNewOrder(order) {
                        generatedOrderNum = order.displayOrderNumber
                        showSuccessDialog = true
                    }
                },
                enabled = !isSubmitting && !isUploadingAttachment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_order_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSubmitting || isUploadingAttachment) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال الطلب لتلقي التسعيرة المعتمدة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // 1. WhatsApp Activation Dialog
    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تفعيل الحساب مطلوب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "عذراً، يجب تفعيل حسابك أولاً لتتمكن من تقديم الطلبات. اضغط على الزر للتواصل وتفعيل حسابك مباشرة عبر واتساب الإدارة.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "تنبيه: يرجى إرسال الرسالة من نفس رقم هاتفك المسجل (${userProfile?.phone}) لمطابقة البيانات واعتماد التفعيل من قِبل المدير.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActivationDialog = false
                        WhatsAppHelper.openAccountActivation(
                            context = context,
                            userName = userProfile?.name.orEmpty().ifBlank { "مستخدم جديد" },
                            userPhone = userProfile?.phone.orEmpty(),
                            userUid = userProfile?.uid.orEmpty(),
                            adminWhatsApp = appConfig.whatsappNumber
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تفعيل الحساب عبر واتساب", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showActivationDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onOrderSubmitted()
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(46.dp)
                )
            },
            title = {
                Text(
                    text = "تم استلام طلبك بنجاح!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "تم استلام مواصفات طلبك بنجاح، سيتم مراجعة المتطلبات وتحديد السعر المناسب من قِبل الإدارة في أقرب وقت لتتمكن من اعتماده وسداد الدفعة المقدمة.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onOrderSubmitted()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("متابعة طلباتي", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. Fullscreen Image Preview Dialog
    if (previewAttachmentUrl != null) {
        com.example.ui.components.FullScreenImagePreviewDialog(
            imageUrl = previewAttachmentUrl!!,
            title = "معاينة المرفق المرجعي للطلب",
            onDismiss = { previewAttachmentUrl = null }
        )
    }
}
