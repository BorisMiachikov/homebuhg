package ru.homebuhg.core.export

import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWriter {

    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val zone = ZoneId.systemDefault()

    private val HEADERS = listOf(
        "Дата", "Тип", "Сумма", "Счёт (откуда)", "Счёт (куда)", "Категория", "Примечание", "Источник"
    )

    fun write(
        transactions: List<TransactionEntity>,
        categories: Map<String, CategoryEntity>,
        accounts: Map<String, AccountEntity>,
        output: OutputStream
    ) {
        val rows = transactions.map { tx ->
            listOf(
                tx.occurredAt.toDate(),
                tx.type.label(),
                "%.2f".format(tx.amountMinor / 100.0),
                accounts[tx.accountId]?.name ?: "",
                tx.toAccountId?.let { accounts[it]?.name } ?: "",
                categories[tx.categoryId]?.name ?: "",
                tx.note ?: "",
                tx.sourceType.name
            )
        }

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.entry("[Content_Types].xml", contentTypesXml())
            zip.entry("_rels/.rels", relsXml())
            zip.entry("xl/workbook.xml", workbookXml())
            zip.entry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            zip.entry("xl/styles.xml", stylesXml())
            zip.entry("xl/worksheets/sheet1.xml", sheetXml(HEADERS, rows))
        }
    }

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun Long.toDate(): String =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate().format(dateFmt)

    private fun TransactionType.label() = when (this) {
        TransactionType.INCOME -> "Доход"
        TransactionType.EXPENSE -> "Расход"
        TransactionType.TRANSFER -> "Перевод"
    }

    private fun xmlEsc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun cellRef(row: Int, col: Int) = "${('A' + col)}$row"

    private fun sheetXml(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        sb.append("""<row r="1">""")
        headers.forEachIndexed { col, h ->
            sb.append("""<c r="${cellRef(1, col)}" t="inlineStr" s="1"><is><t>${xmlEsc(h)}</t></is></c>""")
        }
        sb.append("</row>")

        rows.forEachIndexed { idx, row ->
            val r = idx + 2
            sb.append("""<row r="$r">""")
            row.forEachIndexed { col, value ->
                sb.append("""<c r="${cellRef(r, col)}" t="inlineStr"><is><t>${xmlEsc(value)}</t></is></c>""")
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun relsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Операции" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
}
