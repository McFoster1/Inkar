package com.mcfoster.inkar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    repo: Repo,
    query: String,
    onQuery: (String) -> Unit,
    onStory: (String) -> Unit,
    onChapter: (String, String) -> Unit,
    onNote: (String) -> Unit,
) {
    var applied by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        delay(150)
        applied = query
    }
    val results = remember(applied) { repo.searchAll(applied) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250)
        runCatching { focus.requestFocus() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Txt("Search", 26f, InkText, FontWeight.SemiBold, spacing = -0.26f)
        HField(query, onQuery, modifier = Modifier.focusRequester(focus), placeholder = "Search chapters, notes and stories", maxLen = 200)

        val q = applied.trim()
        if (q.length < 2) {
            Txt("Type at least 2 letters to search your chapters, notes and stories.", 15f, InkMuted)
        } else if (results.isEmpty) {
            Txt("Nothing found for “$q”.", 15f, InkMuted)
        } else {
            if (results.stories.isNotEmpty()) {
                ResultCard("Stories", results.stories.size) {
                    for ((i, s) in results.stories.take(40).withIndex()) {
                        if (i > 0) Divider()
                        ResultRow(
                            title = s.title,
                            meta = metaLine(s.genre, "${s.chapters} chapter${if (s.chapters == 1) "" else "s"}"),
                            snip = null,
                        ) { onStory(s.id) }
                    }
                }
            }
            if (results.chapters.isNotEmpty()) {
                ResultCard("Chapters", results.chapters.size) {
                    for ((i, c) in results.chapters.take(40).withIndex()) {
                        if (i > 0) Divider()
                        ResultRow(
                            title = c.title,
                            meta = metaLine(c.story, "${c.matches} match${if (c.matches == 1) "" else "es"}"),
                            snip = c.snip,
                        ) { onChapter(c.sid, c.cid) }
                    }
                }
            }
            if (results.notes.isNotEmpty()) {
                ResultCard("Notes", results.notes.size) {
                    for ((i, n) in results.notes.take(40).withIndex()) {
                        if (i > 0) Divider()
                        ResultRow(title = n.title, meta = metaLine(n.story, n.cat), snip = n.snip) { onNote(n.sid) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(InkBorder))
}

@Composable
private fun ResultCard(title: String, count: Int, content: @Composable () -> Unit) {
    HCard {
        Row {
            Txt(title, 15f, InkText, FontWeight.SemiBold)
            Txt(" ($count)", 15f, InkDim)
        }
        content()
    }
}

@Composable
private fun ResultRow(title: String, meta: String, snip: Snip?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Txt(title.ifBlank { "Untitled" }, 16f, InkText, FontWeight.SemiBold)
        Txt(meta, 13f, InkMuted)
        if (snip != null) {
            val text = buildAnnotatedString {
                if (snip.head) append("…")
                append(snip.before)
                withStyle(SpanStyle(background = InkGlow2)) { append(snip.match) }
                append(snip.after)
                if (snip.tail) append("…")
            }
            Text(text, color = InkMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
