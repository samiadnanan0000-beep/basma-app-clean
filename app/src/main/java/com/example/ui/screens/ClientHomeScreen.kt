package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OfferItem
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ClientHomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToNewOrder: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToPortfolio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val offers by mainViewModel.offerRepository.observeOffers().collectAsState(initial = emptyList())
    val unreadNotifs by mainViewModel.notifications.collectAsState()
    val unreadCount = unreadNotifs.count { !it.isRead }
    val isPreviewingAsClient by mainViewModel.previewAsClient.collectAsState()
    val userProfile by mainViewModel.userProfile.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()
    val isClientRole = userProfile?.role?.uppercase() !in listOf("ADMIN", "SUPER_ADMIN", "DESIGNER")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Preview Mode Banner (if Designer/SuperAdmin is previewing client view)
        item {
            AnimatedVisibility(visible = isPreviewingAsClient) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أنت الآن في وضع تجربة رحلة العميل",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }
                        Button(
                            onClick = { mainViewModel.exitClientPreview() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("العودة للوحة المصمم", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Header Section (One UI Top Bar with Greeting & Notification Bell)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isActivated = userProfile?.isAccountActivated == true || mainViewModel.isSuperAdmin()
                val clientDisplayName = userProfile?.name?.ifBlank { "عميل بصمة" } ?: "عميل بصمة"

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مرحباً $clientDisplayName 👋",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActivated) {
                            Spacer(modifier = Modifier.width(6.dp))
                            com.example.ui.components.VerifiedCheckIcon(
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    com.example.ui.components.ModernVerifiedPillBadge(
                        isActivated = isActivated,
                        onUnverifiedClick = {
                            val adminWhatsApp = appConfig.whatsappNumber.ifBlank { com.example.util.WhatsAppHelper.DEFAULT_ADMIN_WHATSAPP }
                            com.example.util.WhatsAppHelper.openAccountActivation(
                                context = context,
                                userName = userProfile?.name ?: "عميل بصمة",
                                userPhone = userProfile?.phone ?: "",
                                userUid = userProfile?.uid ?: "",
                                adminWhatsApp = adminWhatsApp
                            )
                        }
                    )
                }

                // Interactive Notification Bell with Unread Badge
                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("notification_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text(text = if (unreadCount > 9) "9+" else unreadCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "مركز الإشعارات والتنبيهات",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Animated HorizontalPager Carousel ("عروض بصمة الحصرية")
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عروض بصمة الحصرية 🎁",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (offers.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { offers.size })
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        val offer = offers[page]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clickable { onNavigateToNewOrder() },
                            shape = RoundedCornerShape(22.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = offer.imageUrl,
                                    contentDescription = offer.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Gradient Overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )
                                // Content
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    if (offer.discountBadge.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE11D48))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = offer.discountBadge,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = offer.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = offer.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // New Order Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clickable { onNavigateToNewOrder() }
                        .testTag("quick_new_order_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "طلب عمل جديد",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "تسعير ومقاسات",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // WhatsApp Direct Contact
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clickable {
                            val uri = Uri.parse("https://wa.me/967771051177?text=${Uri.encode("مرحباً استوديو بصمة، أود الاستفسار عن تفاصيل وطلب أعمال دعاية وطباعة.")}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                        .testTag("quick_whatsapp_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF25D366))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "محادثة واتساب",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "فريق الدعم المباشر",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Apply as Designer Partner Card (for clients only)
        if (isClientRole) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_designer_upgrade_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الانضمام كشريك تصميم / التقديم للعمل كمصمم 🎨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "أرسل سيرتك الذاتية (CV) ونماذج أعمالك مباشرة لإدارة بصمة عبر واتساب.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "جارٍ توجيهك لواتساب الإدارة؛ يرجى إرفاق سيرتك الذاتية (CV) ونماذج أعمالك في المحادثة.",
                                    Toast.LENGTH_LONG
                                ).show()

                                val adminWhatsApp = appConfig.whatsappNumber.ifBlank { com.example.util.WhatsAppHelper.DEFAULT_ADMIN_WHATSAPP }
                                com.example.util.WhatsAppHelper.openDesignerApplication(
                                    context = context,
                                    userName = userProfile?.name?.ifBlank { "عميل بصمة" } ?: "عميل بصمة",
                                    userPhone = userProfile?.phone.orEmpty(),
                                    userUid = userProfile?.uid.orEmpty(),
                                    adminWhatsApp = adminWhatsApp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("home_apply_designer_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("التقديم للعمل كمصمم (إرسال CV عبر واتساب)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Portfolio Showcase Grid (سابقة الأعمال)
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "سابقة الأعمال والتنفيذ 🎨",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = { onNavigateToPortfolio?.invoke() ?: onNavigateToNewOrder() },
                        modifier = Modifier.testTag("view_all_portfolio_button")
                    ) {
                        Text(
                            text = "عرض المعرض بالكامل ❯",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val portfolioItems = listOf(
                    Triple("واجهات كلادنج أبراج وبنوك", "تنفيذ كلادنج ومجسمات ضخمة", "https://images.unsplash.com/photo-1541888946425-d0fbb186f5f7?w=600"),
                    Triple("حروف زنكور واكريليك مضيء", "إضاءة LED كورية مخفية", "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=600"),
                    Triple("بنرات واستكرات سيارات", "طباعة عالية الدقة 1440 DPI", "https://images.unsplash.com/photo-1572945753563-8049567831f4?w=600"),
                    Triple("هويات بصرية متكاملة", "تصميم شعار ومطبوعات شركات", "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=600")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (pair in portfolioItems.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in pair) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToPortfolio?.invoke() ?: onNavigateToNewOrder() },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = item.third,
                                            contentDescription = item.first,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = item.first,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.second,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Developer documentation & copyright footer
        item {
            com.example.ui.components.DeveloperFooter(
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
