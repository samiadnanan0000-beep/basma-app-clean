package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppHelper {

    const val DEFAULT_ADMIN_WHATSAPP = "967771051177"

    /**
     * Opens official WhatsApp conversation or falls back to WhatsApp Business / browser.
     */
    fun openChat(
        context: Context,
        phoneNumber: String = DEFAULT_ADMIN_WHATSAPP,
        message: String = ""
    ) {
        val cleanPhone = phoneNumber
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .trim()

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            // Attempt standard WhatsApp first
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Try WhatsApp Business or fallback to any web browser
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp.w4b")
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                } catch (e3: Exception) {
                    Toast.makeText(context, "تعذر فتح تطبيق واتساب أو المتصفح", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Formats and opens account activation message for management verification.
     */
    fun openAccountActivation(
        context: Context,
        userName: String,
        userPhone: String,
        userUid: String,
        adminWhatsApp: String = DEFAULT_ADMIN_WHATSAPP
    ) {
        val message = """
            السلام عليكم إدارة تطبيق بصمة، أود تفعيل حسابي لتقديم الطلبات:
            - الاسم: $userName
            - رقم الهاتف المسجل: $userPhone
            - معرف الحساب: $userUid
            (يرجى التكرم باعتماد التفعيل للبدء في طلب الخدمات)
        """.trimIndent()

        openChat(context, adminWhatsApp, message)
    }

    /**
     * Formats and opens designer application message with CV request via WhatsApp.
     */
    fun openDesignerApplication(
        context: Context,
        userName: String,
        userPhone: String,
        userUid: String,
        adminWhatsApp: String = DEFAULT_ADMIN_WHATSAPP
    ) {
        val message = """
            السلام عليكم إدارة تطبيق بصمة، أود التقديم للانضمام كشريك تصميم (مصمم معتمد):
            - الاسم: $userName
            - رقم الهاتف المسجل: $userPhone
            - معرف الحساب: $userUid
            (مرفق لكم في هذه المحادثة ملف السيرة الذاتية CV ونماذج من أعمالي السابقة للاطلاع والاعتماد)
        """.trimIndent()

        openChat(context, adminWhatsApp, message)
    }
}
