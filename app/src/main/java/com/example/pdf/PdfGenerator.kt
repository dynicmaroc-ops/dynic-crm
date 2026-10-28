package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.Company
import com.example.model.DocumentEntity
import com.example.model.DocumentLineEntity
import com.example.model.DocumentType
import com.example.model.DocumentWithLines
import com.example.model.FormatUtils
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private val COLOR_DYNIC_GREEN = Color.parseColor("#1DB094")
    private val COLOR_CELL_GRAY = Color.parseColor("#F2F2F2")
    private val COLOR_BORDER = Color.parseColor("#333333")

    fun generatePdfFile(
        context: Context,
        documentWithLines: DocumentWithLines,
        company: Company
    ): File {
        val doc = documentWithLines.document
        val lines = documentWithLines.lines

        val pdfDocument = PdfDocument()

        // Load background and stamp assets
        val bgBitmap = try {
            context.assets.open("dynic_fond_page.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }

        val stampBitmap = try {
            context.assets.open("dynic_cachet.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }

        val bgFillPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawPageBackground() {
            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT), null)
            } else {
                canvas.drawColor(Color.WHITE)
            }
        }

        fun drawHeaderAndClientInfo() {
            // 4.2 Date: "Salé le : JJ/MM/AAAA" at x = 373, y ≈ 99
            val dateText = "Salé le : ${doc.date}"
            canvas.drawText(dateText, 373f, 99f, textPaint)

            // 4.3 Bloc client at x = 243, line-height 14.3 pt, first baseline at y ≈ 160
            val startY = 160f
            val lineHeight = 14.5f

            // Client line
            canvas.drawText("Client : ", 243f, startY, boldPaint)
            val clientPrefixWidth = boldPaint.measureText("Client : ")
            canvas.drawText(doc.tiersNom.ifBlank { "Client Divers" }, 243f + clientPrefixWidth, startY, textPaint)

            // Adresse line
            canvas.drawText("Adresse : ", 243f, startY + lineHeight, boldPaint)
            val addrPrefixWidth = boldPaint.measureText("Adresse : ")
            canvas.drawText(doc.tiersAdresse.ifBlank { "Salé" }, 243f + addrPrefixWidth, startY + lineHeight, textPaint)

            // ICE line
            canvas.drawText("ICE : ", 243f, startY + (lineHeight * 2), boldPaint)
            val icePrefixWidth = boldPaint.measureText("ICE : ")
            canvas.drawText(doc.tiersIce.ifBlank { "-" }, 243f + icePrefixWidth, startY + (lineHeight * 2), textPaint)

            // 4.4 Titre at x = 34, baseline y ≈ 231 pt, bold 14 pt
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            }
            val titlePrefix = when (doc.type) {
                DocumentType.DEVIS -> "Devis N° :  "
                DocumentType.BON_LIVRAISON -> "Bon de livraison N° :  "
                DocumentType.FACTURE -> "Facture N° :  "
                DocumentType.BON_COMMANDE -> "Bon de commande N° :  "
            }
            canvas.drawText(titlePrefix + doc.numero, 34f, 231f, titlePaint)
        }

        // Draw Table Header
        fun drawTableHeader(startY: Float): Float {
            val tableStartX = 32f
            val headerHeight = 22f

            bgFillPaint.color = COLOR_DYNIC_GREEN
            canvas.drawRect(tableStartX, startY, tableStartX + 490f, startY + headerHeight, bgFillPaint)

            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            if (doc.type == DocumentType.BON_LIVRAISON) {
                // BL Columns: Réf (70), Désignation (280), UM (60), Quantités livrées (80)
                canvas.drawText("Réf", tableStartX + 35f, startY + 15f, headerTextPaint)
                canvas.drawText("Désignation", tableStartX + 70f + 140f, startY + 15f, headerTextPaint)
                canvas.drawText("UM", tableStartX + 350f + 30f, startY + 15f, headerTextPaint)
                canvas.drawText("Quantités livrées", tableStartX + 410f + 40f, startY + 15f, headerTextPaint)

                // Draw vertical lines
                canvas.drawLine(tableStartX + 70f, startY, tableStartX + 70f, startY + headerHeight, borderPaint)
                canvas.drawLine(tableStartX + 350f, startY, tableStartX + 350f, startY + headerHeight, borderPaint)
                canvas.drawLine(tableStartX + 410f, startY, tableStartX + 410f, startY + headerHeight, borderPaint)
            } else {
                // Devis / Facture: Désignation (221), UM (58), Qté (60), PU (HT) (70), Total (HT) (81)
                canvas.drawText("Désignation", tableStartX + (221f / 2f), startY + 15f, headerTextPaint)
                canvas.drawText("UM", tableStartX + 221f + 29f, startY + 15f, headerTextPaint)
                canvas.drawText("Qté", tableStartX + 279f + 30f, startY + 15f, headerTextPaint)
                canvas.drawText("PU (HT)", tableStartX + 339f + 35f, startY + 15f, headerTextPaint)
                canvas.drawText("Total (HT)", tableStartX + 409f + 40.5f, startY + 15f, headerTextPaint)

                // Vertical column lines
                canvas.drawLine(tableStartX + 221f, startY, tableStartX + 221f, startY + headerHeight, borderPaint)
                canvas.drawLine(tableStartX + 279f, startY, tableStartX + 279f, startY + headerHeight, borderPaint)
                canvas.drawLine(tableStartX + 339f, startY, tableStartX + 339f, startY + headerHeight, borderPaint)
                canvas.drawLine(tableStartX + 409f, startY, tableStartX + 409f, startY + headerHeight, borderPaint)
            }

            canvas.drawRect(tableStartX, startY, tableStartX + 490f, startY + headerHeight, borderPaint)
            return startY + headerHeight
        }

        // Initialize page 1
        drawPageBackground()
        drawHeaderAndClientInfo()
        var currentY = drawTableHeader(274f)

        val tableStartX = 32f

        val staticTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        // Draw Table Rows
        for (line in lines) {
            val desigWidth = if (doc.type == DocumentType.BON_LIVRAISON) 270 else 213
            val staticLayout = StaticLayout.Builder.obtain(
                line.designation,
                0,
                line.designation.length,
                staticTextPaint,
                desigWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val textHeight = staticLayout.height.toFloat()
            val rowHeight = (textHeight + 10f).coerceAtLeast(22f)

            // Check page overflow (never exceed y ≈ 755 pt to protect footer)
            if (currentY + rowHeight > 755f) {
                // Page break!
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawPageBackground()
                currentY = drawTableHeader(100f) // Subsequents start table at y=100
            }

            val rowBottom = currentY + rowHeight

            if (doc.type == DocumentType.BON_LIVRAISON) {
                // Background fills
                bgFillPaint.color = COLOR_CELL_GRAY
                canvas.drawRect(tableStartX, currentY, tableStartX + 70f, rowBottom, bgFillPaint)
                bgFillPaint.color = Color.WHITE
                canvas.drawRect(tableStartX + 70f, currentY, tableStartX + 350f, rowBottom, bgFillPaint)
                bgFillPaint.color = COLOR_CELL_GRAY
                canvas.drawRect(tableStartX + 350f, currentY, tableStartX + 490f, rowBottom, bgFillPaint)

                // Borders
                canvas.drawRect(tableStartX, currentY, tableStartX + 490f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 70f, currentY, tableStartX + 70f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 350f, currentY, tableStartX + 350f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 410f, currentY, tableStartX + 410f, rowBottom, borderPaint)

                // Content
                // Ref
                val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 9.5f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(line.ref.ifBlank { "-" }, tableStartX + 35f, currentY + 14f, centerPaint)

                // Designation
                canvas.save()
                canvas.translate(tableStartX + 75f, currentY + 5f)
                staticLayout.draw(canvas)
                canvas.restore()

                // UM
                canvas.drawText(line.um, tableStartX + 380f, currentY + 14f, centerPaint)

                // Quantite
                val qteStr = FormatUtils.formatMoney(line.quantite)
                canvas.drawText(qteStr, tableStartX + 450f, currentY + 14f, centerPaint)
            } else {
                // Devis / Facture / Commande
                // Col 1: White, Col 2..5: #F2F2F2
                bgFillPaint.color = Color.WHITE
                canvas.drawRect(tableStartX, currentY, tableStartX + 221f, rowBottom, bgFillPaint)
                bgFillPaint.color = COLOR_CELL_GRAY
                canvas.drawRect(tableStartX + 221f, currentY, tableStartX + 490f, rowBottom, bgFillPaint)

                // Borders
                canvas.drawRect(tableStartX, currentY, tableStartX + 490f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 221f, currentY, tableStartX + 221f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 279f, currentY, tableStartX + 279f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 339f, currentY, tableStartX + 339f, rowBottom, borderPaint)
                canvas.drawLine(tableStartX + 409f, currentY, tableStartX + 409f, rowBottom, borderPaint)

                // Texts
                // Designation
                canvas.save()
                canvas.translate(tableStartX + 5f, currentY + 5f)
                staticLayout.draw(canvas)
                canvas.restore()

                val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 9.5f
                    textAlign = Paint.Align.CENTER
                }
                val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 9.5f
                    textAlign = Paint.Align.RIGHT
                }

                // UM
                canvas.drawText(line.um, tableStartX + 221f + 29f, currentY + 14f, centerPaint)

                // Qté
                canvas.drawText(FormatUtils.formatMoney(line.quantite), tableStartX + 279f + 30f, currentY + 14f, centerPaint)

                // PU HT
                canvas.drawText(FormatUtils.formatMoney(line.prixUnitaireHT), tableStartX + 405f, currentY + 14f, rightPaint)

                // Total HT
                canvas.drawText(FormatUtils.formatMoney(line.totalHT), tableStartX + 486f, currentY + 14f, rightPaint)
            }

            currentY = rowBottom
        }

        // Draw Totals and Stamp
        if (doc.type != DocumentType.BON_LIVRAISON) {
            val totalsHeight = 14f + (17.5f * 3) + 140f // table space + 3 lines + stamp
            if (currentY + totalsHeight > 760f) {
                // Page break so totals and stamp stay together as requested in spec
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPageBackground()
                currentY = 100f
            }

            currentY += 14f // gap
            val totalsStartX = 371f
            val labelWidth = 70f
            val valueWidth = 81f
            val rowH = 17.5f

            val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
            }

            // Line 1: Total Ht
            bgFillPaint.color = COLOR_DYNIC_GREEN
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth, currentY + rowH, bgFillPaint)
            bgFillPaint.color = COLOR_CELL_GRAY
            canvas.drawRect(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, bgFillPaint)
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, borderPaint)
            canvas.drawLine(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth, currentY + rowH, borderPaint)
            canvas.drawText("Total Ht", totalsStartX + (labelWidth / 2f), currentY + 12f, totalLabelPaint)
            canvas.drawText(FormatUtils.formatMoney(doc.totalHT), totalsStartX + labelWidth + valueWidth - 5f, currentY + 12f, totalValuePaint)

            // Line 2: TVA 20%
            currentY += rowH
            bgFillPaint.color = COLOR_DYNIC_GREEN
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth, currentY + rowH, bgFillPaint)
            bgFillPaint.color = COLOR_CELL_GRAY
            canvas.drawRect(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, bgFillPaint)
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, borderPaint)
            canvas.drawLine(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth, currentY + rowH, borderPaint)
            val tvaLabel = "TVA  ${doc.tvaTaux.toInt()}%"
            canvas.drawText(tvaLabel, totalsStartX + (labelWidth / 2f), currentY + 12f, totalLabelPaint)
            canvas.drawText(FormatUtils.formatMoney(doc.totalTVA), totalsStartX + labelWidth + valueWidth - 5f, currentY + 12f, totalValuePaint)

            // Line 3: TTC
            currentY += rowH
            bgFillPaint.color = COLOR_DYNIC_GREEN
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth, currentY + rowH, bgFillPaint)
            bgFillPaint.color = COLOR_CELL_GRAY
            canvas.drawRect(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, bgFillPaint)
            canvas.drawRect(totalsStartX, currentY, totalsStartX + labelWidth + valueWidth, currentY + rowH, borderPaint)
            canvas.drawLine(totalsStartX + labelWidth, currentY, totalsStartX + labelWidth, currentY + rowH, borderPaint)
            canvas.drawText("TTC", totalsStartX + (labelWidth / 2f), currentY + 12f, totalLabelPaint)
            canvas.drawText(FormatUtils.formatMoney(doc.totalTTC), totalsStartX + labelWidth + valueWidth - 5f, currentY + 12f, totalValuePaint)

            currentY += rowH

            // Notes if any
            if (doc.notes.isNotBlank()) {
                val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY
                    textSize = 8.5f
                    typeface = Typeface.create("sans-serif", Typeface.ITALIC)
                }
                canvas.drawText(doc.notes, 34f, currentY + 14f, notesPaint)
            }

            // Cachet DYNIC
            if (doc.avecCachet && stampBitmap != null) {
                val stampRect = RectF(365f, currentY + 6f, 365f + 112f, currentY + 6f + 130f)
                canvas.drawBitmap(stampBitmap, null, stampRect, null)
            }
        } else {
            // Bon de livraison: Reçu par / Signature client + Cachet
            val footerBoxHeight = 120f
            if (currentY + footerBoxHeight > 760f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPageBackground()
                currentY = 100f
            }

            currentY += 16f
            val sigBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            }
            canvas.drawText("Reçu par / Signature du client :", 40f, currentY + 12f, sigBoxPaint)

            val boxBorder = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(40f, currentY + 18f, 240f, currentY + 95f, 6f, 6f, boxBorder)

            // Cachet on right
            if (doc.avecCachet && stampBitmap != null) {
                val stampRect = RectF(365f, currentY, 365f + 112f, currentY + 130f)
                canvas.drawBitmap(stampBitmap, null, stampRect, null)
            }
        }

        pdfDocument.finishPage(page)

        // Save to cache dir
        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val filename = "${doc.type.label}_${doc.numero.replace("/", "-")}_${doc.date.replace("/", "-")}.pdf"
        val outputFile = File(outputDir, filename)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    fun renderPdfToBitmaps(pdfFile: File): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // Render at high resolution (1.5x scale for clarity)
                val width = (page.width * 1.5f).toInt()
                val height = (page.height * 1.5f).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le document PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
