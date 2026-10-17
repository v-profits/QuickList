package com.quicklist.app.util

data class ParsedItem(
    val title: String,
    val quantity: String = "",
    val priority: Int = 0,
    val done: Boolean = false
)

/**
 * Turns free text into items. Examples:
 *  "milk, bread, eggs"      -> 3 items
 *  "2 milk" / "milk x2"     -> milk, quantity 2
 *  "1.5 kg apples"          -> apples, quantity "1.5 kg"
 *  "milk 2l"                -> milk, quantity "2 l"
 *  "call mom!"              -> high priority
 *
 * Also understands lists copied from messengers or shared by QuickList itself:
 *  "☐ Milk — 2", "✓ Bread", "- eggs", "• eggs", "[ ] eggs", "[x] eggs", "1. eggs"
 */
object QuickParser {
    private const val UNITS = "kg|g|lb|oz|l|ml|pcs|pc|шт|кг|г|л|мл|уп"
    private const val NUM = "\\d+(?:[.,]\\d+)?"

    // ✓ ✔ ☑ ✅ [x]
    private val doneMarker = Regex("^(?:\u2713|\u2714\uFE0F?|\u2611\uFE0F?|\u2705|\\[[xXхХ]\\])\\s*")

    // ☐ ⬜ □ 🔲 [ ]  and  - • * · ▪ ● ○ ‣ – —  and  1.  1)
    private val openMarker = Regex(
        "^(?:\u2610|\u2B1C|\u25A1|\uD83D\uDD32|\\[\\s?\\])\\s*" +
                "|^(?:[-\u2022*\u00B7\u25AA\u25CF\u25CB\u2023\u2013\u2014]|\\d+[.)])\\s+"
    )

    private val dashQty = Regex("^(.+?)\\s+[\u2014\u2013]\\s+(.+)$")
    private val leading = Regex("^($NUM)\\s*($UNITS|[xх×])?\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val trailingX = Regex("^(.+?)\\s+[xх×]\\s?($NUM)$", RegexOption.IGNORE_CASE)
    private val trailingUnit = Regex("^(.+?)\\s+($NUM)\\s*($UNITS)$", RegexOption.IGNORE_CASE)

    /** Quick-add input: items separated by comma, semicolon or new line. */
    fun parseAll(text: String): List<ParsedItem> =
        text.split('\n', ',', ';').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { parseOne(it) }

    /**
     * Pasted or shared text. If it has several lines, one item per line is assumed
     * (commas stay part of the name); a header line like "Shopping:" is skipped.
     */
    fun parseShared(text: String): List<ParsedItem> {
        val lines = text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        if (lines.size == 1) return parseAll(lines[0])
        val first = lines[0]
        val dropHeader = !hasMarker(first) && (first.endsWith(":") || lines.drop(1).any { hasMarker(it) })
        val body = if (dropHeader) lines.drop(1) else lines
        return body.mapNotNull { parseOne(it) }
    }

    private fun hasMarker(s: String) = doneMarker.containsMatchIn(s) || openMarker.containsMatchIn(s)

    fun parseOne(raw: String): ParsedItem? {
        var s = raw.trim()
        var done = false

        val dm = doneMarker.find(s)
        if (dm != null) {
            done = true
            s = s.substring(dm.range.last + 1).trim()
        } else {
            val om = openMarker.find(s)
            if (om != null) s = s.substring(om.range.last + 1).trim()
        }

        var priority = 0
        if (s.endsWith("!")) {
            priority = 2
            s = s.trimEnd('!').trim()
        }
        if (s.isEmpty()) return null

        var qty = ""
        val dq = dashQty.matchEntire(s)
        if (dq != null) {
            s = dq.groupValues[1]
            qty = dq.groupValues[2].trim()
        } else {
            val lm = leading.matchEntire(s)
            if (lm != null) {
                qty = qtyOf(lm.groupValues[1], lm.groupValues[2])
                s = lm.groupValues[3]
            } else {
                val tx = trailingX.matchEntire(s)
                if (tx != null) {
                    qty = qtyOf(tx.groupValues[2], "")
                    s = tx.groupValues[1]
                } else {
                    val tu = trailingUnit.matchEntire(s)
                    if (tu != null) {
                        qty = qtyOf(tu.groupValues[2], tu.groupValues[3])
                        s = tu.groupValues[1]
                    }
                }
            }
        }
        s = s.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (s.isEmpty()) return null
        return ParsedItem(s, qty, priority, done)
    }

    private fun qtyOf(num: String, unit: String): String {
        val u = unit.lowercase()
        return if (u.isEmpty() || u == "x" || u == "х" || u == "×") num else "$num $u"
    }
}
