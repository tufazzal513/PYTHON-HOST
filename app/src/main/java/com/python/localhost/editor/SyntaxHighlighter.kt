package com.python.localhost.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

enum class Language { PYTHON, JSON, XML, YAML, MARKDOWN, SHELL, PLAIN }

fun detectLanguage(fileName: String): Language {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "py", "pyw" -> Language.PYTHON
        "json" -> Language.JSON
        "xml", "html", "htm" -> Language.XML
        "yaml", "yml" -> Language.YAML
        "md", "markdown" -> Language.MARKDOWN
        "sh", "bash" -> Language.SHELL
        else -> Language.PLAIN
    }
}

data class HighlightColors(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
)

fun defaultDarkColors() = HighlightColors(
    keyword = Color(0xFF79C0FF),
    string = Color(0xFFA5D6FF),
    comment = Color(0xFF8B949E),
    number = Color(0xFFFF7B72),
)

/**
 * Lightweight, original syntax highlighter. Tokenises on keywords, strings, comments,
 * and numbers and paints them with span styles. Not a full parser — good enough for
 * readable mobile editing without bundling a heavy editor component.
 */
object SyntaxHighlighter {
    private val PY_KEYWORDS = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while", "break", "continue",
        "import", "from", "as", "pass", "with", "try", "except", "finally", "raise", "lambda",
        "yield", "global", "nonlocal", "assert", "del", "in", "is", "not", "and", "or",
        "None", "True", "False", "async", "await", "print",
    )
    private val JSON_KEYWORDS = setOf("true", "false", "null")
    private val SHELL_KEYWORDS = setOf(
        "if", "then", "else", "fi", "for", "while", "do", "done", "echo", "cd", "export",
        "source", "function", "case", "esac",
    )

    fun highlight(code: String, language: Language, colors: HighlightColors): AnnotatedString {
        return when (language) {
            Language.PYTHON -> generic(code, PY_KEYWORDS, colors, "#")
            Language.JSON -> generic(code, JSON_KEYWORDS, colors, null)
            Language.YAML -> generic(code, emptySet(), colors, "#")
            Language.SHELL -> generic(code, SHELL_KEYWORDS, colors, "#")
            Language.XML -> xml(code, colors)
            Language.MARKDOWN -> buildAnnotatedString { append(code) }
            Language.PLAIN -> buildAnnotatedString { append(code) }
        }
    }

    private fun generic(
        code: String,
        keywords: Set<String>,
        colors: HighlightColors,
        comment: String?,
    ): AnnotatedString = buildAnnotatedString {
        var i = 0
        val n = code.length
        while (i < n) {
            val c = code[i]
            if (comment != null && c == comment[0]) {
                val end = code.indexOf('\n', i).let { if (it == -1) n else it }
                withStyle(SpanStyle(color = colors.comment)) { append(code.substring(i, end)) }
                i = end
                continue
            }
            if ((c == '"' || c == '\'' || c == '`')) {
                val quote = c
                var j = i + 1
                while (j < n) {
                    if (code[j] == '\\') { j += 2; continue }
                    if (code[j] == quote) { j++; break }
                    j++
                }
                withStyle(SpanStyle(color = colors.string)) { append(code.substring(i, j)) }
                i = j
                continue
            }
            if (c.isDigit()) {
                var j = i
                while (j < n && (code[j].isDigit() || code[j] == '.')) j++
                withStyle(SpanStyle(color = colors.number)) { append(code.substring(i, j)) }
                i = j
                continue
            }
            if (c.isLetter() || c == '_') {
                var j = i
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                if (keywords.contains(word)) {
                    withStyle(SpanStyle(color = colors.keyword)) { append(word) }
                } else {
                    append(word)
                }
                i = j
                continue
            }
            append(c)
            i++
        }
    }

    private fun xml(code: String, colors: HighlightColors): AnnotatedString = buildAnnotatedString {
        var i = 0
        val n = code.length
        while (i < n) {
            if (code[i] == '<') {
                val end = code.indexOf('>', i).let { if (it == -1) n else it + 1 }
                withStyle(SpanStyle(color = colors.keyword)) { append(code.substring(i, end)) }
                i = end
            } else {
                append(code[i])
                i++
            }
        }
    }
}
