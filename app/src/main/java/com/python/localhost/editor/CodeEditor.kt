package com.python.localhost.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CodeHighlightTransformation(private val annotated: AnnotatedString) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(annotated, OffsetMapping.Identity)
}

/**
 * A real (if compact) code editor: monospace text, line-number gutter, syntax
 * highlighting via a VisualTransformation, optional word wrap, and horizontal scroll.
 * Cursor/selection use the identity offset mapping so highlighting never corrupts them.
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: Language,
    fontSize: Int,
    wordWrap: Boolean,
    showLineNumbers: Boolean,
    colors: HighlightColors,
    modifier: Modifier = Modifier,
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    Row(modifier) {
        if (showLineNumbers) {
            val lineCount = value.text.count { it == '\n' } + 1
            Column(
                Modifier
                    .verticalScroll(vScroll)
                    .width(44.dp)
                    .padding(end = 6.dp, top = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top,
            ) {
                for (i in 1..lineCount) {
                    Text(
                        i.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val annotated = remember(value.text, language, colors) {
            SyntaxHighlighter.highlight(value.text, language, colors)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(vScroll)
                .then(if (wordWrap) Modifier else Modifier.horizontalScroll(hScroll))
                .padding(6.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            visualTransformation = CodeHighlightTransformation(annotated),
        )
    }
}
