package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val VerifiedBlue = Color(0xFF1DA1F2)

@Composable
fun VerifiedCheckIcon(
    modifier: Modifier = Modifier.size(18.dp)
) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "موثق",
        tint = VerifiedBlue,
        modifier = modifier
    )
}

@Composable
fun ModernVerifiedPillBadge(
    isActivated: Boolean,
    onUnverifiedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (isActivated) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VerifiedBlue.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, VerifiedBlue.copy(alpha = 0.3f)),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)) // Green active dot
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "حساب موثق ✓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerifiedBlue
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF97316).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.35f)),
            modifier = modifier.then(
                if (onUnverifiedClick != null) Modifier.clickable { onUnverifiedClick() } else Modifier
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "حساب غير موثق - بانتظار التفعيل ⚠️",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEA580C)
                )
            }
        }
    }
}
