package com.example.motivation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.example.motivation.R

val LiterataFontFamily = FontFamily(
    Font(resId = R.font.literata_regular, weight = FontWeight.Normal),
    Font(resId = R.font.literata_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.literata_bold, weight = FontWeight.Bold),
    Font(resId = R.font.literata_medium, weight = FontWeight.Medium)
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
