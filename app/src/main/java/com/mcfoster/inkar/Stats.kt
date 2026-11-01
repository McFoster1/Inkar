package com.mcfoster.inkar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun StatsScreen(repo: Repo) {
    val goal = repo.goal()
    val stories = repo.storyViews()
    val totalWords = stories.sumOf { it.words }
    val week = repo.weekWords()
    val streak = repo.streak()
    val longest = repo.longestStreak()
    val r30 = repo.daySeries(30)
    val activeDays = r30.count { it.second > 0 }
    val goalDays = r30.count { it.second > 0 && it.second >= goal }
    val avgDay = if (activeDays > 0) Math.round(r30.sumOf { it.second }.toDouble() / activeDays).toInt() else 0
    val wd = repo.weekdayStats()
    val lv = repo.level()
    val weeklyTarget = minOf(100, Math.round(week.toDouble() / (goal * 7) * 100).toInt())
    var sel by remember { mutableStateOf<Int?>(null) }
    val cols = ((LocalConfiguration.current.screenWidthDp - 32 + 16) / 216).coerceIn(1, 4)

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) { detectTapGestures { sel = null } }
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Txt("Understand your writing habits.", 26f, InkText, FontWeight.SemiBold, spacing = -0.26f)
        Spacer(Modifier.height(4.dp))

        LevelCard(lv)

        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { m -> StatTile("TOTAL WORDS", fmt(totalWords), "${stories.size} ${if (stories.size == 1) "story" else "stories"}", R.drawable.ic_doc, m) },
            { m -> StatTile("THIS WEEK", fmt(week), "Last 7 days", R.drawable.ic_cal, m) },
            { m -> StatTile("CURRENT STREAK", "$streak ${if (streak == 1) "day" else "days"}", "Longest: $longest", R.drawable.ic_fire, m) },
            { m -> StatTile("AVG / WRITING DAY", fmt(avgDay), "Last 30 days", R.drawable.ic_bars, m) },
        )
        CardGrid(cols, tiles)

        WritingChartCard(repo, goal, sel) { sel = it }

        WeekdayCard(wd)

        val second: List<@Composable (Modifier) -> Unit> = listOf(
            { m -> WeeklyTargetCard(weeklyTarget, week, goal * 7, m) },
            { m -> CountCard("WRITING DAYS", activeDays, 30, "Days with any words, last 30", m) },
            { m -> CountCard("GOAL DAYS", goalDays, 30, "Days you hit ${fmt(goal)} words", m) },
            { m ->
                HCard(m) {
                    Txt("LONGEST STREAK", 11f, InkDim, FontWeight.SemiBold, spacing = 0.88f)
                    Txt("$longest ${if (longest == 1) "day" else "days"}", 26f, InkText, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Txt("Your best run so far", 12f, InkDim, modifier = Modifier.padding(top = 4.dp))
                }
            },
        )
        CardGrid(cols, second)

        StoryStatsCard(repo, stories, totalWords)
    }
}

@Composable
private fun CardGrid(cols: Int, items: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in items.chunked(cols)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (item in row) item(Modifier.weight(1f))
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------- level card ----------

@Composable
private fun LevelCard(lv: LevelInfo) {
    HCard(glow = Glow.TopLeft) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(84.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    val r = 38.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(InkEl, radius = r, center = c, style = Stroke(stroke))
                    if (lv.progress > 0f) {
                        drawArc(
                            color = InkBlue,
                            startAngle = -90f,
                            sweepAngle = 360f * lv.progress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = Offset(c.x - r, c.y - r),
                            size = Size(2f * r, 2f * r),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Box(Modifier.align(Alignment.Center)) { VecIcon(R.drawable.ic_pen, InkBlue, 30.dp) }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(30.dp)
                        .clip(CircleShape).background(InkBlue).border(3.dp, InkCard, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Txt("${lv.index + 1}", 14f, Color.White, FontWeight.Bold, lh = 1f) }
            }
            Column(Modifier.weight(1f)) {
                Txt(lv.title, 24f, InkBlue, FontWeight.Bold, lh = 1.15f)
                Txt("Level ${lv.index + 1} Author", 14f, InkMuted)
                Spacer(Modifier.height(12.dp))
                StatBar(lv.progress)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt("${fmt(lv.words)} words written", 13f, InkDim)
                    Txt(if (lv.hasNext) "${fmt(lv.left)} to next" else "Max level reached", 13f, InkDim)
                }
            }
        }
    }
}

@Composable
private fun StatBar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(InkEl)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(InkBlue))
    }
}

// ---------- tiles ----------

@Composable
private fun StatTile(label: String, value: String, sub: String, @DrawableRes icon: Int, modifier: Modifier) {
    HCard(modifier, glow = Glow.TopRight) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Txt(label, 11f, InkDim, FontWeight.SemiBold, spacing = 0.88f, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                Txt(value, 26f, InkText, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                Txt(sub, 12f, InkDim)
            }
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier.size(38.dp).shadow(8.dp, shape, ambientColor = InkGlow2, spotColor = InkGlow2).clip(shape).background(InkBlue),
                contentAlignment = Alignment.Center,
            ) { VecIcon(icon, Color.White, 20.dp) }
        }
    }
}

@Composable
private fun CountCard(label: String, value: Int, of: Int, sub: String, modifier: Modifier) {
    HCard(modifier) {
        Txt(label, 11f, InkDim, FontWeight.SemiBold, spacing = 0.88f)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Txt("$value", 26f, InkText, FontWeight.Bold)
            Txt(" / $of", 15f, InkDim, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
        }
        Spacer(Modifier.height(10.dp))
        StatBar(value.toFloat() / of)
        Txt(sub, 12f, InkDim, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun WeeklyTargetCard(percent: Int, week: Int, target: Int, modifier: Modifier) {
    HCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val r = 36.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(InkEl, radius = r, center = c, style = Stroke(stroke))
                    if (percent > 0) {
                        drawArc(
                            color = InkBlue,
                            startAngle = -90f,
                            sweepAngle = 360f * percent / 100f,
                            useCenter = false,
                            topLeft = Offset(c.x - r, c.y - r),
                            size = Size(2f * r, 2f * r),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Txt("$percent%", 16f, InkText, FontWeight.Bold, lh = 1.2f, align = TextAlign.Center)
            }
            Column(Modifier.weight(1f)) {
                Txt("WEEKLY TARGET", 11f, InkDim, FontWeight.SemiBold, spacing = 0.88f)
                Txt("${fmt(week)} of ${fmt(target)} words", 13f, InkMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ---------- weekday insights ----------

@Composable
private fun WeekdayCard(wd: WeekdayStats) {
    var bestIdx = 0
    for (i in wd.avg.indices) if (wd.avg[i] > wd.avg[bestIdx]) bestIdx = i
    val best = wd.avg[bestIdx]
    val maxAvg = maxOf(1, wd.avg.maxOrNull() ?: 1)
    HCard {
        Txt("Weekday insights", 15f, InkText, FontWeight.SemiBold)
        Txt(
            if (best > 0) "You write most on ${DAY_NAMES[bestIdx]}s, averaging ${fmt(best)} words. Based on ${wd.days} days of history."
            else "Write for a few days and your weekday pattern will appear here.",
            15f, InkMuted, modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 7) {
                val isBest = i == bestIdx && best > 0
                val shape = RoundedCornerShape(12.dp)
                Column(
                    Modifier.weight(1f)
                        .drawBehind {
                            if (isBest) {
                                val g = 3.dp.toPx()
                                drawRoundRect(InkGlow, Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(12.dp.toPx() + g))
                            }
                        }
                        .clip(shape).background(InkEl)
                        .border(1.dp, if (isBest) InkBlue else Color.Transparent, shape)
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Txt(fmt(wd.avg[i]), 13f, InkText, FontWeight.Bold, lh = 1.2f, maxLines = 1, align = TextAlign.Center)
                    Box(Modifier.width(14.dp).height(84.dp).clip(CircleShape).background(InkCard), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight(wd.avg[i].toFloat() / maxAvg).clip(CircleShape)
                                .background(if (isBest) InkBlue else InkDim),
                        )
                    }
                    Txt(DAY_SHORT[i], 12f, InkDim, lh = 1.2f)
                }
            }
        }
    }
}

// ---------- story statistics ----------

@Composable
private fun StoryStatsCard(repo: Repo, stories: List<StoryV>, totalWords: Int) {
    HCard {
        Txt("Story statistics", 15f, InkText, FontWeight.SemiBold)
        if (stories.isEmpty()) {
            Txt("No stories yet.", 15f, InkMuted, modifier = Modifier.padding(top = 8.dp))
        }
        for ((i, s) in stories.withIndex()) {
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(InkBorder))
            val avg = if (s.chapters.isEmpty()) 0 else Math.round(s.words.toDouble() / s.chapters.size).toInt()
            val share = Math.round(s.words.toDouble() / maxOf(totalWords, 1) * 100).toInt()
            val dot = try {
                Color(android.graphics.Color.parseColor(s.color))
            } catch (e: Exception) {
                InkBlue
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverImage(s.cover, s.title, s.color, 44.dp, (44f * 4f / 3f).dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                        Spacer(Modifier.width(8.dp))
                        Txt(s.title, 16f, InkText, FontWeight.SemiBold)
                    }
                    Txt(
                        metaLine("${fmt(s.words)} words", "${s.chapters.size} ch", "avg ${fmt(avg)}/ch", "${fmt(repo.last7(s.id))} this week"),
                        13f, InkMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Txt("$share%", 15f, InkMuted)
            }
        }
    }
}
