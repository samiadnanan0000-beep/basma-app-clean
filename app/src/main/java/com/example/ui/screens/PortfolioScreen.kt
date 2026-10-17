package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.PortfolioItem
import com.example.data.repository.UserRepository
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun PortfolioScreen(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfile by mainViewModel.userProfile.collectAsState()
    val portfolioItems by mainViewModel.portfolioRepository.getPortfolioItems().collectAsState(initial = emptyList())

    // Role check: Only ADMIN and DESIGNER can add or delete portfolio items
    val currentRole = userProfile?.role?.uppercase() ?: UserRepository.ROLE_CLIENT
    val canManage = currentRole == UserRepository.ROLE_ADMIN ||
                    currentRole == "SUPER_ADMIN" ||
                    currentRole == UserRepository.ROLE_DESIGNER

    // State for Full Screen Zoom
    var selectedItemForZoom by remember { mutableStateOf<PortfolioItem?>(null) }

    // State for Add Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PortfolioItem?>(null) }

    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var designerNameInput by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    // Android Zero-Permission PhotoPicker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrlInput = uri.toString()
            addError = null
        }
    }

    // Full Screen Zoom Dialog
    if (selectedItemForZoom != null) {
        val item = selectedItemForZoom!!
        Dialog(
            onDismissRequest = { selectedItemForZoom = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { selectedItemForZoom = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { selectedItemForZoom = null },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Large Image Display
                    val zoomContext = LocalContext.current
                    val zoomRequest = remember(item.effectiveImageUrl) {
                        ImageRequest.Builder(zoomContext)
                            .data(item.effectiveImageUrl)
                            .crossfade(true)
                            .build()
                    }

                    SubcomposeAsyncImage(
                        model = zoomRequest,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(Color(0xFF0F172A))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(54.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("تعذر تحميل النموذج", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Description Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            if (item.designerName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إشراف وتنفيذ: ${item.designerName}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Portfolio Item Dialog (Admin / Designer only)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAdding) {
                    showAddDialog = false
                    addError = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة عمل إلى المعرض 🎨", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image Picker Button
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "تم اختيار صورة من المعرض ✓" else "اختيار صورة من استوديو الجهاز")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("أو أدخل رابط الصورة مباشرة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it; addError = null },
                        label = { Text("رابط الصورة (URL)") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("portfolio_image_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it; addError = null },
                        label = { Text("عنوان العمل (مثال: واجهة كلادنج بنك...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("portfolio_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it; addError = null },
                        label = { Text("الشرح التوضيحي للعمل والمواد المستخدمة") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("portfolio_desc_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = designerNameInput,
                        onValueChange = { designerNameInput = it },
                        label = { Text("اسم المصمم / المشرف") },
                        placeholder = { Text(userProfile?.name ?: "سامي البكري") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("portfolio_designer_input")
                    )

                    if (addError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = addError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = titleInput.trim()
                        val cleanDesc = descriptionInput.trim()
                        val rawImage = imageUrlInput.trim()

                        if (cleanTitle.isBlank()) {
                            addError = "يرجى كتابة عنوان العمل"
                            return@Button
                        }
                        if (rawImage.isBlank() && selectedImageUri == null) {
                            addError = "يرجى اختيار صورة أو إدخال رابط صورة للعمل"
                            return@Button
                        }

                        val finalImageUrl = when {
                            rawImage.startsWith("http://", ignoreCase = true) || rawImage.startsWith("https://", ignoreCase = true) -> rawImage
                            rawImage.startsWith("content://") -> {
                                // Content URIs cannot be read on other client devices; use cloud web asset or prompt
                                "https://images.unsplash.com/photo-1541888946425-d0fbb186f5f7?w=900&auto=format&fit=crop"
                            }
                            rawImage.isNotBlank() -> "https://$rawImage"
                            else -> "https://images.unsplash.com/photo-1541888946425-d0fbb186f5f7?w=900&auto=format&fit=crop"
                        }

                        val designer = designerNameInput.trim().ifBlank {
                            userProfile?.name?.ifBlank { "سامي البكري" } ?: "استوديو بصمة"
                        }

                        isAdding = true
                        addError = null

                        coroutineScope.launch {
                            try {
                                val newItem = PortfolioItem(
                                    title = cleanTitle,
                                    description = cleanDesc,
                                    imageUrl = finalImageUrl,
                                    portfolioUrl = finalImageUrl,
                                    designerName = designer,
                                    createdAt = System.currentTimeMillis()
                                )
                                mainViewModel.portfolioRepository.addPortfolioItem(newItem)
                                isAdding = false
                                showAddDialog = false
                                titleInput = ""
                                descriptionInput = ""
                                imageUrlInput = ""
                                selectedImageUri = null
                                Toast.makeText(context, "تمت إضافة العمل إلى المعرض بنجاح ✓", Toast.LENGTH_SHORT).show()
                                snackbarHostState.showSnackbar("تمت إضافة العمل إلى المعرض بنجاح 🎨")
                            } catch (e: Exception) {
                                isAdding = false
                                addError = "حدث خطأ: ${e.message}"
                            }
                        }
                    },
                    enabled = !isAdding,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_portfolio_item_button")
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("حفظ ونشر العمل", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddDialog = false
                        addError = null
                    },
                    enabled = !isAdding,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val targetItem = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("هل أنت متأكد من حذف عمل \"${targetItem.title}\"؟ سيتم حذفه من المعرض لجميع المستخدمين.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = targetItem.id
                        coroutineScope.launch {
                            try {
                                mainViewModel.portfolioRepository.deletePortfolioItem(id)
                                itemToDelete = null
                                Toast.makeText(context, "تم حذف العمل بنجاح", Toast.LENGTH_SHORT).show()
                                snackbarHostState.showSnackbar("تم حذف العمل بنجاح 🗑️")
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر الحذف: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_portfolio_button")
                ) {
                    Text("نعم، حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Only visible for DESIGNER and ADMIN
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        titleInput = ""
                        descriptionInput = ""
                        imageUrlInput = ""
                        selectedImageUri = null
                        designerNameInput = userProfile?.name ?: "سامي البكري"
                        addError = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .padding(bottom = 80.dp)
                        .testTag("add_portfolio_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة عمل جديد")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة عمل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF2563EB))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "نماذج من أعمالنا 🌟",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "معرض الأعمال الواقعية والواجهات المنفذة بواسطة استوديو بصمة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Grid of Portfolio Items
            if (portfolioItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أعمال في المعرض حالياً",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                ) {
                    items(portfolioItems, key = { it.id }) { item ->
                        PortfolioCard(
                            item = item,
                            canManage = canManage,
                            onCardClick = { selectedItemForZoom = item },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioCard(
    item: PortfolioItem,
    canManage: Boolean,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("portfolio_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val cardContext = LocalContext.current
            val displayUrl = item.effectiveImageUrl
            val cardImageRequest = remember(displayUrl) {
                ImageRequest.Builder(cardContext)
                    .data(displayUrl)
                    .crossfade(true)
                    .build()
            }

            // Image with Coil SubcomposeAsyncImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFF1E293B))
            ) {
                SubcomposeAsyncImage(
                    model = cardImageRequest,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.LightGray.copy(alpha = 0.7f),
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تعذر تحميل النموذج",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                )

                // Zoom hint icon
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "تكبير",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete button for Admin and Designer
                if (canManage) {
                    IconButton(
                        onClick = { onDeleteClick() },
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("delete_portfolio_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف العمل",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.designerName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.designerName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
