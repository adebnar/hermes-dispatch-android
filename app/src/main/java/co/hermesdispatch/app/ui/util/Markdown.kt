package co.hermesdispatch.app.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal Markdown renderer for cron deliverables — headings, bold/italic,
 * inline code, fenced code blocks, bullet/numbered lists, blockquotes, rules,
 * and links. Deliberately dependency-free (no WebView, no Google libs) and
 * scoped to what hermes-agent reports actually contain.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    Column(modifier = modifier) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("```") -> {
                    val buf = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        buf.appendLine(lines[i]); i++
                    }
                    CodeBlock(buf.toString().trimEnd('\n'))
                }
                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    Heading(trimmed.drop(level).trim(), level)
                }
                trimmed == "---" || trimmed == "***" || trimmed == "___" ->
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                trimmed.startsWith("> ") || trimmed == ">" ->
                    BlockQuote(trimmed.removePrefix(">").trim())
                trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                    Bullet("•", trimmed.drop(2))
                NUMBERED.matches(trimmed) -> {
                    val m = NUMBERED.find(trimmed)!!
                    Bullet(m.groupValues[1] + ".", m.groupValues[2])
                }
                trimmed.isBlank() -> Unit
                else -> Text(
                    inline(line),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            i++
        }
    }
}

private val NUMBERED = Regex("""^(\d+)\.\s+(.*)$""")

@Composable
private fun Heading(text: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        inline(text),
        style = style,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Bullet(marker: String, text: String) {
    Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
        Text("$marker ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(inline(text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BlockQuote(text: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
    ) {
        Text(inline(text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CodeBlock(code: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
    ) {
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val URL = Regex("""https?://[^\s<>"')\]]+""")
// **bold**, *italic*/_italic_, `code`, and [text](url) — non-greedy.
private val TOKEN = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|_(.+?)_|`([^`]+?)`|\[([^\]]+)]\(([^)]+)\)""")

/** Render inline markdown spans into an AnnotatedString. */
fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in TOKEN.findAll(text)) {
        if (m.range.first > last) appendLinkified(text.substring(last, m.range.first))
        when {
            m.groupValues[1].isNotEmpty() ->
                withStyleSpan(SpanStyle(fontWeight = FontWeight.Bold), m.groupValues[1])
            m.groupValues[2].isNotEmpty() ->
                withStyleSpan(SpanStyle(fontStyle = FontStyle.Italic), m.groupValues[2])
            m.groupValues[3].isNotEmpty() ->
                withStyleSpan(SpanStyle(fontStyle = FontStyle.Italic), m.groupValues[3])
            m.groupValues[4].isNotEmpty() ->
                withStyleSpan(
                    SpanStyle(fontFamily = FontFamily.Monospace),
                    m.groupValues[4],
                )
            m.groupValues[5].isNotEmpty() -> {
                val label = m.groupValues[5]
                val url = m.groupValues[6]
                withLink(LinkAnnotation.Url(url, LINK_STYLES)) { append(label) }
            }
        }
        last = m.range.last + 1
    }
    if (last < text.length) appendLinkified(text.substring(last))
}

private val LINK_STYLES = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))

private fun AnnotatedString.Builder.withStyleSpan(style: SpanStyle, content: String) {
    pushStyle(style)
    append(content)
    pop()
}

/** Append plain text, turning bare URLs into tappable links. */
private fun AnnotatedString.Builder.appendLinkified(text: String) {
    var last = 0
    for (m in URL.findAll(text)) {
        append(text.substring(last, m.range.first))
        val url = m.value.trimEnd('.', ',', ';', ':', ')', ']')
        withLink(LinkAnnotation.Url(url, LINK_STYLES)) { append(url) }
        if (url.length < m.value.length) append(m.value.substring(url.length))
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}
