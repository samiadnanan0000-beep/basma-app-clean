package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Developer documentation & copyright footer (DeveloperFooter).
 * Features direct dial click action via Intent.ACTION_DIAL (safe, no CALL_PHONE permission required).
 */
@Composable
fun DeveloperFooter(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phoneNumber = "771051177"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .testTag("developer_footer"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "إعداد وتطوير : سامي عدنان البكري - ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Text(
            text = phoneNumber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB), // Distinct interactive blue
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                    }
                }
                .testTag("developer_phone_dial_action")
        )
    }
}
