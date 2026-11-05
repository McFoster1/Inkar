package com.mcfoster.inkar

import android.graphics.Color as AColor
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo.get(applicationContext)
        val splashOnStart = savedInstanceState == null
        if (splashOnStart && repo.updateCheckOn()) Updater.check(applicationContext)
        Theme.mode = repo.theme()
        Theme.amoled = repo.amoled()
        Theme.sysDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        applyBars(Theme.isLight)
        setContent {
            val saved = repo.theme()
            val amoledOn = repo.amoled()
            val sysDark = isSystemInDarkTheme()
            SideEffect {
                Theme.mode = saved
                Theme.amoled = amoledOn
                Theme.sysDark = sysDark
            }
            val light = Theme.isLight
            var splashDone by rememberSaveable { mutableStateOf(!splashOnStart) }
            LaunchedEffect(Unit) {
                if (!splashDone) {
                    delay(1300)
                    splashDone = true
                }
            }
            SideEffect { applyBars(light && splashDone) }
            MaterialTheme(colorScheme = inkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = InkBg) {
                    Box(Modifier.fillMaxSize()) {
                        InkarApp(repo)
                        AnimatedVisibility(visible = !splashDone, enter = EnterTransition.None, exit = fadeOut(tween(350))) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        EditorHooks.flush?.invoke()
        super.onPause()
    }

    private fun applyBars(light: Boolean) {
        val style = if (light) SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT) else SystemBarStyle.dark(AColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

@Composable
private fun InkarApp(repo: Repo) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var openStory by rememberSaveable { mutableStateOf<String?>(null) }
    var storyTab by rememberSaveable { mutableStateOf("overview") }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var newRequested by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val levelUp = repo.levelUp
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    fun openEditor(sid: String, cid: String) {
        searching = false
        viewing = false
        editing = "$sid|$cid"
        tab = 2
    }

    fun leaveEditor(sid: String) {
        viewing = false
        editing = null
        storyTab = "chapters"
        openStory = sid
        tab = 1
    }

    fun continueStory(s: StoryV) {
        val c = s.chapters.maxByOrNull { it.updatedAt }
        if (c != null) {
            openEditor(s.id, c.id)
        } else {
            searching = false
            storyTab = "overview"
            openStory = s.id
            tab = 1
        }
    }

    fun selectTab(i: Int) {
        searching = false
        aboutOpen = false
        if (i == 2) {
            if (editing != null) {
                tab = 2
                return
            }
            val l = repo.lastChapter()
            if (l != null) openEditor(l.first, l.second) else toast(ctx, "Create a story and chapter to start writing")
            return
        }
        if (i == 1) openStory = null
        editing = null
        tab = i
    }

    BackHandler(enabled = aboutOpen || searching || editing != null || openStory != null || tab != 0) {
        val e = editing
        when {
            searching -> searching = false
            aboutOpen -> aboutOpen = false
            e != null && viewing -> viewing = false
            e != null -> leaveEditor(e.substringBefore('|'))
            openStory != null -> openStory = null
            else -> tab = 0
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(InkBg).statusBarsPadding().imePadding()) {
            AnimatedVisibility(
                visible = !viewing,
                enter = fadeIn(tween(200)) + expandVertically(tween(200), expandFrom = Alignment.Top),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top),
            ) {
                AppTopBar(
                    repo,
                    showNav = wide && editing == null,
                    selected = tab,
                    onSelect = { selectTab(it) },
                    onSearch = { if (editing == null || !viewing) searching = true },
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (searching) {
                    SearchScreen(
                        repo,
                        query = searchQuery,
                        onQuery = { searchQuery = it },
                        onStory = { sid ->
                            searching = false
                            editing = null
                            storyTab = "overview"
                            openStory = sid
                            tab = 1
                        },
                        onChapter = { sid, cid -> openEditor(sid, cid) },
                        onNote = { sid ->
                            searching = false
                            editing = null
                            storyTab = "notes"
                            openStory = sid
                            tab = 1
                        },
                    )
                } else when (tab) {
                    0 -> HomeScreen(
                        repo,
                        onContinue = { s -> continueStory(s) },
                        onNewStory = {
                            openStory = null
                            newRequested = true
                            tab = 1
                        },
                    )
                    1 -> {
                        val id = openStory
                        if (id == null) {
                            StoriesPage(
                                repo,
                                query = query,
                                onQuery = { query = it },
                                filter = filter,
                                onFilter = { filter = it },
                                newRequested = newRequested,
                                onNewHandled = { newRequested = false },
                                onOpen = { sid ->
                                    storyTab = "overview"
                                    openStory = sid
                                },
                            )
                        } else {
                            StoryPage(
                                repo, id, storyTab,
                                onTab = { storyTab = it },
                                onWrite = { cid -> openEditor(id, cid) },
                                onBack = { openStory = null },
                            )
                        }
                    }
                    2 -> {
                        val e = editing
                        if (e != null) {
                            val sid = e.substringBefore('|')
                            val cid = e.substringAfter('|')
                            EditorScreen(repo, sid, cid, viewing, { viewing = it }, onBack = { leaveEditor(sid) })
                        } else {
                            LaunchedEffect(Unit) {
                                val l = repo.lastChapter()
                                if (l != null) {
                                    openEditor(l.first, l.second)
                                } else {
                                    toast(ctx, "Create a story and chapter to start writing")
                                    tab = 1
                                }
                            }
                        }
                    }
                    3 -> StatsScreen(repo)
                    else -> if (aboutOpen) AboutScreen(repo) else MenuScreen(repo, onAbout = { aboutOpen = true }, onCleared = {
                        openStory = null
                        editing = null
                        searchQuery = ""
                        tab = 0
                    })
                }
            }
            val showBottom = !wide && !imeOpen && !viewing && editing == null
            AnimatedVisibility(
                visible = showBottom,
                enter = fadeIn(tween(200)) + expandVertically(tween(200), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Bottom),
            ) {
                BottomBar(tab) { selectTab(it) }
            }
            val inset by animateColorAsState(if (showBottom) InkCard else InkBg, tween(200), label = "inset")
            Box(Modifier.fillMaxWidth().background(inset).navigationBarsPadding())
        }

        if (levelUp != null) {
            LaunchedEffect(levelUp) {
                delay(6000)
                repo.dismissLevelUp()
            }
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 12.dp, end = 12.dp)
                    .widthIn(max = 360.dp).fillMaxWidth()
                    .shadow(12.dp, shape, ambientColor = InkBlue, spotColor = InkBlue)
                    .clip(shape).background(InkCard).border(1.dp, InkBlue, shape)
                    .clickable { repo.dismissLevelUp() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Txt("LEVEL UP!", 12f, InkBlue, FontWeight.Bold, spacing = 0.96f)
                Txt("You've reached Level ${levelUp.index + 1}", 15f, InkText)
                Txt(levelUp.title, 18f, InkText, FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
                Txt("Your dedication to writing continues to grow.", 13f, InkDim)
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(InkCard)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(InkBorder))
        Row(Modifier.fillMaxWidth()) {
            NAV_DEFS.forEachIndexed { i, d ->
                val on = i == selected
                val color = if (on) InkBlue else InkDim
                val src = remember { MutableInteractionSource() }
                val pressed by src.collectIsPressedAsState()
                Column(
                    Modifier.weight(1f)
                        .heightIn(min = 54.dp)
                        .graphicsLayer {
                            alpha = if (pressed) 0.55f else 1f
                            scaleX = if (pressed) 0.94f else 1f
                            scaleY = if (pressed) 0.94f else 1f
                        }
                        .clickable(interactionSource = src, indication = null) { onSelect(i) }
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    VecIcon(if (on) d.solid else d.outline, color, 24.dp)
                    Txt(d.short, 11f, color, if (on) FontWeight.SemiBold else FontWeight.Normal, lh = 1.3f)
                }
            }
        }
    }
}

/** The opening screen: just the nib on black, signed "BY MCFOSTER". */
@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { } }) {
        Image(
            painterResource(R.drawable.nib),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(width = 67.dp, height = 120.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            "BY MCFOSTER",
            color = Color(0xB3FFFFFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 44.dp),
        )
    }
}
