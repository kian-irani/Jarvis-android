package com.kianirani.jarvis.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kianirani.jarvis.core.text.MarkdownLite
import com.kianirani.jarvis.core.text.MdBlock
import com.kianirani.jarvis.core.text.MdBlockType
import com.kianirani.jarvis.core.text.MdSpan

/**
 * BUG-1b — renders the small Markdown subset from [MarkdownLite] (bold, inline code,
 * bullet / numbered lists) as themed Compose text. The parsing is pure and unit-tested;
 * this is the thin view layer mapping blocks/spans onto AnnotatedString + Column, so
 * the model's `**bold**`, `` `code` `` and lists read properly instead of as raw markup.
 */
@Composable
fun MarkdownText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { MarkdownLite.parse(text) }
    Column(modifier) {
        blocks.forEach { block ->
            when (block.type) {
                MdBlockType.PARAGRAPH ->
                    Text(annotate(block.spans, color), style = style, color = color)
                MdBlockType.BULLET ->
                    MarkdownListRow(prefix = "•", block = block, style = style, color = color)
                MdBlockType.NUMBERED ->
                    MarkdownListRow(prefix = "${block.ordinal}.", block = block, style = style, color = color)
            }
        }
    }
}

@Composable
private fun MarkdownListRow(prefix: String, block: MdBlock, style: TextStyle, color: Color) {
    Row(Modifier.padding(start = 4.dp, top = 2.dp)) {
        Text("$prefix ", style = style, color = color.copy(alpha = 0.7f))
        Text(annotate(block.spans, color), style = style, color = color)
    }
}

private fun annotate(spans: List<MdSpan>, base: Color): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        when {
            span.code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = base.copy(alpha = 0.12f))) {
                append(span.text)
            }
            span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
            else -> append(span.text)
        }
    }
}
