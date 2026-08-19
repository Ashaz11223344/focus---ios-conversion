package com.example.motivation.util

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.motivation.R
import java.io.File
import java.io.FileOutputStream

enum class WallpaperTheme(val displayName: String) {
    NOIR("Noir"),
    EMBER("Ember"),
    PARCHMENT("Parchment"),
    DUSK("Dusk"),
    ASH("Ash"),
    FOG("Fog")
}

enum class WallpaperAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class WallpaperFont(val displayName: String) {
    LITERATA("Literata"),
    INTER("Inter"),
    LORA("Lora")
}

object WallpaperRenderer {

    // Single pre-cached noise bitmap to prevent memory fragmentation and high overhead
    private var cachedNoiseBitmap: Bitmap? = null

    // Pre-cache typefaces so they are loaded once
    private var typefaceLiterataItalic: Typeface? = null
    private var typefaceLiterataRegular: Typeface? = null
    private var typefaceLiterataMedium: Typeface? = null
    private var typefaceInterLight: Typeface? = null

    /**
     * Initializes resource caches. Safe to call multiple times.
     */
    private fun initResources(context: Context) {
        if (typefaceLiterataItalic == null) {
            typefaceLiterataItalic = ResourcesCompat.getFont(context, R.font.literata_italic)
        }
        if (typefaceLiterataRegular == null) {
            typefaceLiterataRegular = ResourcesCompat.getFont(context, R.font.literata_regular)
        }
        if (typefaceLiterataMedium == null) {
            typefaceLiterataMedium = ResourcesCompat.getFont(context, R.font.literata_medium)
        }
        if (typefaceInterLight == null) {
            typefaceInterLight = ResourcesCompat.getFont(context, R.font.inter_light)
        }
        if (cachedNoiseBitmap == null) {
            cachedNoiseBitmap = generateNoiseBitmap(128, 128)
        }
    }

    /**
     * Generates a simple, tileable noise bitmap.
     */
    private fun generateNoiseBitmap(width: Int, height: Int): Bitmap {
        val config = Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(width, height, config)
        val random = java.util.Random()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val noise = random.nextInt(256)
                // Grayscale white noise
                val color = Color.argb(noise, 255, 255, 255)
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    /**
     * Core offline rendering engine.
     * Takes dimensions and parameters, and draws a 9:16 layout onto a clean Bitmap on a background thread.
     */
    fun renderWallpaper(
        context: Context,
        width: Int,
        height: Int,
        quoteText: String,
        signatureText: String,
        theme: WallpaperTheme,
        alignment: WallpaperAlignment,
        grainOpacity: Float, // 0.0f to 0.08f
        showBranding: Boolean,
        textSizeSp: Float,
        fontChoice: WallpaperFont = WallpaperFont.LITERATA,
        onTextSizeClamped: ((Float) -> Unit)? = null,
        onVerticalOverflow: (() -> Unit)? = null
    ): Bitmap? {
        try {
            initResources(context)

            val scale = width.toFloat() / 1080f

            // Setup high-quality target bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // --- LAYER 1: Background Theme ---
            drawBackground(canvas, width, height, theme)

            // --- LAYER 2: Depth Vignette Overlay ---
            drawVignette(canvas, width, height)

            // --- LAYOUT MEASUREMENTS ---
            // Max width: 78% of canvas width
            val maxTextWidth = (width * 0.78f).toInt()

            val layoutAlignment = when (alignment) {
                WallpaperAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
                WallpaperAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                WallpaperAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            }

            var currentSp = textSizeSp
            var quoteTextSizePx = currentSp * scale

            // Select typeface based on font choice
            val selectedTypeface = when (fontChoice) {
                WallpaperFont.LITERATA -> typefaceLiterataItalic ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                WallpaperFont.INTER -> typefaceInterLight ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                WallpaperFont.LORA -> Typeface.create("serif", Typeface.ITALIC)
            }

            val quotePaint = TextPaint().apply {
                isAntiAlias = true
                typeface = selectedTypeface
                textSize = quoteTextSizePx
                color = Color.parseColor("#F7DDD4")
            }

            var textLayout = StaticLayout.Builder.obtain(quoteText, 0, quoteText.length, quotePaint, maxTextWidth)
                .setAlignment(layoutAlignment)
                .setLineSpacing(0f, 1.6f) // Line height: 1.6x
                .setIncludePad(false)
                .build()

            // Calculate vertical center block position (slightly above center).
            // Shift up to 34% if the quote is extremely long to avoid overlapping/overflowing the screen.
            var startY = if (quoteText.length > 180) height * 0.34f else height * 0.40f
            var lastLineBottom = startY + textLayout.height
            val gapBetweenTextAndAuthor = if (signatureText.isNotBlank()) 24f * scale else 0f
            var authorY = if (signatureText.isNotBlank()) lastLineBottom + gapBetweenTextAndAuthor else lastLineBottom
            val bottomMargin = (if (showBranding) 80f else 20f) * scale

            var didClamp = false
            while (authorY + bottomMargin > height.toFloat() && currentSp > 14f) {
                currentSp -= 1f
                didClamp = true
                quoteTextSizePx = currentSp * scale
                quotePaint.textSize = quoteTextSizePx
                textLayout = StaticLayout.Builder.obtain(quoteText, 0, quoteText.length, quotePaint, maxTextWidth)
                    .setAlignment(layoutAlignment)
                    .setLineSpacing(0f, 1.6f)
                    .setIncludePad(false)
                    .build()

                startY = if (quoteText.length > 180) height * 0.34f else height * 0.40f
                lastLineBottom = startY + textLayout.height
                authorY = if (signatureText.isNotBlank()) lastLineBottom + gapBetweenTextAndAuthor else lastLineBottom
            }

            if (didClamp && onTextSizeClamped != null) {
                onTextSizeClamped(currentSp)
            }

            if (authorY + bottomMargin > height.toFloat() && onVerticalOverflow != null) {
                onVerticalOverflow()
            }

            // Draw text block
            canvas.save()
            // Center horizontally within the 78% bounds
            val textX = (width - maxTextWidth) / 2f
            canvas.translate(textX, startY)
            textLayout.draw(canvas)
            canvas.restore()

            // --- LAYER 3: Decorative opening quote mark (top-left of text block) ---
            drawDecorativeQuoteMark(canvas, scale, textX, startY)

            // --- LAYER 4: Attribution line (24dp below the text block) ---
            if (signatureText.isNotBlank()) {
                val cleanAuthor = if (signatureText.length > 50) signatureText.take(45) + "..." else signatureText
                val authorText = "— $cleanAuthor"

                val authorPaint = Paint().apply {
                    isAntiAlias = true
                    typeface = typefaceInterLight ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textSize = 13f * scale
                    color = Color.parseColor("#8CF7DDD4") // #F7DDD4 at 55% opacity
                }

                val authorWidth = authorPaint.measureText(authorText)
                val authorX = when (alignment) {
                    WallpaperAlignment.LEFT -> textX
                    WallpaperAlignment.CENTER -> (width - authorWidth) / 2f
                    WallpaperAlignment.RIGHT -> textX + maxTextWidth - authorWidth
                }
                canvas.drawText(authorText, authorX, authorY, authorPaint)
            }

            // --- LAYER 5: Focus Branding Mark (Optional Toggle, centered at bottom) ---
            if (showBranding) {
                drawFocusBranding(canvas, scale, width, authorY)
            }

            // --- LAYER 6: Topmost Film Grain Overlay ---
            if (grainOpacity > 0f) {
                drawFilmGrain(canvas, width, height, grainOpacity)
            }

            return bitmap
        } catch (e: OutOfMemoryError) {
            System.gc()
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, theme: WallpaperTheme) {
        val paint = Paint().apply { isAntiAlias = true }
        when (theme) {
            WallpaperTheme.NOIR -> {
                val center = PointF(width * 0.5f, height * 0.45f)
                val radius = maxOf(width, height) * 0.8f
                paint.shader = RadialGradient(
                    center.x, center.y, radius,
                    Color.parseColor("#261913"),
                    Color.parseColor("#1D100B"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            WallpaperTheme.EMBER -> {
                val center = PointF(width * 0.5f, height * 0.45f)
                val radius = maxOf(width, height) * 0.8f
                paint.shader = RadialGradient(
                    center.x, center.y, radius,
                    Color.parseColor("#2E160A"),
                    Color.parseColor("#170700"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            WallpaperTheme.PARCHMENT -> {
                canvas.drawColor(Color.parseColor("#1C1710"))
            }
            WallpaperTheme.DUSK -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#0F0D14"),
                    Color.parseColor("#1A1208"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            WallpaperTheme.ASH -> {
                canvas.drawColor(Color.parseColor("#111113"))
            }
            WallpaperTheme.FOG -> {
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#0E0E12"),
                    Color.parseColor("#181410"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    private fun drawVignette(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        val center = PointF(width * 0.5f, height * 0.45f)
        val radius = maxOf(width, height) * 0.8f
        paint.shader = RadialGradient(
            center.x, center.y, radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.parseColor("#CC0D0D10") // 80% opacity `#0D0D10`
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawDecorativeQuoteMark(canvas: Canvas, scale: Float, textX: Float, textY: Float) {
        val quoteMarkPaint = Paint().apply {
            isAntiAlias = true
            typeface = typefaceLiterataMedium ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 80f * scale
            color = Color.parseColor("#26F7DDD4") // 15% opacity `#F7DDD4`
        }
        // Offset quote mark slightly up-left relative to text block origin
        val offsetX = -16f * scale
        val offsetY = -8f * scale
        canvas.drawText("\u201C", textX + offsetX, textY + offsetY, quoteMarkPaint)
    }

    private fun drawFocusBranding(canvas: Canvas, scale: Float, width: Int, authorY: Float) {
        val brandGap = 40f * scale
        val ruleY = authorY + brandGap

        // Horizontal rule paint (40dp wide, 1dp height)
        val rulePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#33F7DDD4") // 20% opacity `#F7DDD4`
            strokeWidth = 1f * scale
        }
        val ruleHalfWidth = 20f * scale
        val centerX = width / 2f
        canvas.drawLine(centerX - ruleHalfWidth, ruleY, centerX + ruleHalfWidth, ruleY, rulePaint)

        // Text below rule paint
        val brandTextPaint = Paint().apply {
            isAntiAlias = true
            typeface = typefaceLiterataMedium ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 10f * scale
            color = Color.parseColor("#40F7DDD4") // 25% opacity `#F7DDD4`
            textAlign = Paint.Align.CENTER
        }
        val textGap = 16f * scale
        val textY = ruleY + textGap

        // Native canvas text drawing with letter-spacing (only on Android 21+)
        brandTextPaint.letterSpacing = 0.25f

        canvas.drawText("FOCUS", centerX, textY, brandTextPaint)
    }

    private fun drawFilmGrain(canvas: Canvas, width: Int, height: Int, opacity: Float) {
        val noise = cachedNoiseBitmap ?: return
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (opacity * 255).toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * Saves a compiled Bitmap to local Scoped Storage pictures directory.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): File? {
        val folder = File(context.cacheDir, "shared_wallpapers")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "focus_wallpaper_${System.currentTimeMillis()}.png")
        return try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            if (file.exists()) file else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
