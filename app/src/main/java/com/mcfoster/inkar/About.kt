package com.mcfoster.inkar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** The tappable row in Settings that opens the About page. */
@Composable
fun AboutRow(onClick: () -> Unit) {
    Panel(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(InkBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                VecIcon(R.drawable.ic_info, InkBlue, 26.dp)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Txt("About Inkar", 16f, InkText, FontWeight.SemiBold)
                Txt("Version info and updates", 13f, InkMuted)
            }
            VecIcon(R.drawable.ic_chevron, InkMuted, 22.dp)
        }
    }
}

@Composable
fun AboutScreen(repo: Repo) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val version = remember { currentVersion(ctx) }
    val checkOn = repo.updateCheckOn()
    val st = Updater.state

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // header
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(InkCard).background(InkBlue.copy(alpha = 0.14f))
                .border(1.dp, InkBlue.copy(alpha = 0.25f), shape).padding(vertical = 28.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.app_icon), null, Modifier.size(78.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Txt("Inkar", 28f, InkText, FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            Txt("Version $version", 15f, InkMuted)
            Txt("A private writing workspace for authors", 15f, InkMuted, modifier = Modifier.padding(top = 6.dp), align = TextAlign.Center)
        }

        // updates
        HCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VecIcon(R.drawable.ic_refresh, InkBlue, 24.dp)
                Spacer(Modifier.width(10.dp))
                Txt("Updates", 18f, InkText, FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Txt("Check on startup", 16f, InkText)
                    Txt("Automatically check for updates when app opens", 13f, InkMuted)
                }
                Switch(
                    checked = checkOn,
                    onCheckedChange = {
                        repo.setUpdateCheck(it)
                        if (it) Updater.check(ctx)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = InkBlue,
                        checkedBorderColor = InkBlue,
                        uncheckedThumbColor = InkMuted,
                        uncheckedTrackColor = InkEl,
                        uncheckedBorderColor = InkBorder,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(InkBorder))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (st) {
                    is UpdateState.UpToDate -> {
                        VecIcon(R.drawable.ic_check_circle, InkOk, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Txt("You're on the latest version", 15f, InkMuted)
                    }
                    is UpdateState.Available -> {
                        VecIcon(R.drawable.ic_refresh, InkBlue, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Txt("Version ${st.version} is available", 15f, InkText)
                    }
                    is UpdateState.Failed -> {
                        VecIcon(R.drawable.ic_alert, InkBad, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Txt("Couldn't check for updates", 15f, InkMuted)
                    }
                    is UpdateState.Checking -> Txt("Checking…", 15f, InkMuted)
                    is UpdateState.Idle -> Txt("Tap the button to check for updates", 15f, InkMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            val btnShape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(btnShape).background(InkEl)
                    .clickable {
                        if (st is UpdateState.Available) uri.openUri(st.url) else Updater.check(ctx)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                VecIcon(R.drawable.ic_refresh, InkText, 20.dp)
                Spacer(Modifier.width(10.dp))
                Txt(if (st is UpdateState.Available) "Download update" else "Check for Updates", 16f, InkText, FontWeight.Medium)
            }
        }

    }

    // links, pinned to the bottom
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        LinkButton("Website", { uri.openUri(REPO_URL) }) {
            Image(painterResource(R.drawable.nib), null, Modifier.size(11.dp, 20.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(InkText))
        }
        LinkButton("Source", { uri.openUri(REPO_URL) }) { VecIcon(R.drawable.ic_github, InkText, 26.dp) }
        LinkButton("Releases", { uri.openUri(RELEASES_URL) }) { VecIcon(R.drawable.ic_alert, InkText, 22.dp) }
    }
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(InkEl), contentAlignment = Alignment.Center) { icon() }
        Txt(label, 12f, InkMuted, modifier = Modifier.padding(top = 4.dp))
    }
}
