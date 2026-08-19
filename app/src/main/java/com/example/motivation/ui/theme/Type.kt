package com.example.motivation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.example.motivation.R
import androidx.compose.ui.text.googlefonts.GoogleFont

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val EpilogueFont = GoogleFont("Epilogue")

val EpilogueFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = EpilogueFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = EpilogueFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    androidx.compose.ui.text.googlefonts.Font(googleFont = EpilogueFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = EpilogueFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val LiterataFontFamily = FontFamily(
    Font(resId = R.font.literata_regular, weight = FontWeight.Normal),
    Font(resId = R.font.literata_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.literata_bold, weight = FontWeight.Bold),
    Font(resId = R.font.literata_medium, weight = FontWeight.Medium)
)

val InterFontFamily = FontFamily(
    Font(resId = R.font.inter_light, weight = FontWeight.Light)
)

val PlaywriteGBSFontFamily = FontFamily(
    Font(resId = R.font.playwrite_gbs_regular, weight = FontWeight.Normal),
    Font(resId = R.font.playwrite_gbs_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)
