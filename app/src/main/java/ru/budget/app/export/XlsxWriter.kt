package ru.budget.app.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Минимальный генератор .xlsx (OOXML) без внешних зависимостей.
 * (Перенесён из TheSleepTracker — проверено в работе.)
 *
 * Пишет один лист с inline-строками и числами — этого достаточно, чтобы файл
 * открывался в Excel, Google Sheets, LibreOffice и «Google Таблицах» на телефоне.
 */
object XlsxWriter {

    /** Ячейка таблицы. */
    sealed interface Cell {
        data class Text(val value: String) : Cell
        data class Number(val value: Double) : Cell
    }

    /**
     * @param sheetName имя листа (Excel ограничивает 31 символом)
     * @param header заголовки колонок — выделяются жирным
     * @param rows строки данных
     * @param columnWidths ширина колонок в символах
     */
    fun write(
        out: OutputStream,
        sheetName: String,
        header: List<String>,
        rows: List<List<Cell>>,
        columnWidths: List<Int> = emptyList(),
    ) {
        ZipOutputStream(out).use { zip ->
            zip.put("[Content_Types].xml", CONTENT_TYPES)
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbookXml(sheetName))
            zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.put("xl/styles.xml", STYLES)
            zip.put("xl/worksheets/sheet1.xml", sheetXml(header, rows, columnWidths))
        }
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheetXml(
        header: List<String>,
        rows: List<List<Cell>>,
        columnWidths: List<Int>,
    ): String = buildString {
        append(XML_DECL)
        append("<worksheet xmlns=\"$NS_MAIN\">")

        if (columnWidths.isNotEmpty()) {
            append("<cols>")
            columnWidths.forEachIndexed { i, w ->
                append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }

        append("<sheetData>")

        // строка заголовков (стиль 1 — жирный)
        append("<row r=\"1\">")
        header.forEachIndexed { col, title ->
            append("<c r=\"${ref(col, 0)}\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
            append(escape(title))
            append("</t></is></c>")
        }
        append("</row>")

        // данные
        rows.forEachIndexed { rowIndex, cells ->
            val r = rowIndex + 2
            append("<row r=\"$r\">")
            cells.forEachIndexed { col, cell ->
                val ref = ref(col, rowIndex + 1)
                when (cell) {
                    is Cell.Number ->
                        append("<c r=\"$ref\"><v>${trimNumber(cell.value)}</v></c>")

                    is Cell.Text -> {
                        append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        append(escape(cell.value))
                        append("</t></is></c>")
                    }
                }
            }
            append("</row>")
        }

        append("</sheetData></worksheet>")
    }

    /** 0,0 -> A1 ; 27,0 -> AB1 */
    private fun ref(col: Int, row: Int): String {
        val sb = StringBuilder()
        var c = col
        while (true) {
            sb.insert(0, ('A' + c % 26))
            c = c / 26 - 1
            if (c < 0) break
        }
        return "$sb${row + 1}"
    }

    private fun trimNumber(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.2f", v)

    private fun escape(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else ->
                    // управляющие символы недопустимы в XML
                    if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun workbookXml(sheetName: String): String {
        val safe = escape(sheetName.take(31).ifBlank { "Sheet1" })
        return """$XML_DECL<workbook xmlns="$NS_MAIN" xmlns:r="$NS_REL"><sheets><sheet name="$safe" sheetId="1" r:id="rId1"/></sheets></workbook>"""
    }

    private const val XML_DECL = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private const val CONTENT_TYPES = """$XML_DECL<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

    private const val ROOT_RELS = """$XML_DECL<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private const val WORKBOOK_RELS = """$XML_DECL<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

    private const val STYLES = """$XML_DECL<styleSheet xmlns="$NS_MAIN"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs></styleSheet>"""
}
