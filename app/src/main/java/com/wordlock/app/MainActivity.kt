package com.wordlock.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WordLockTheme { key(resumeTick) { SettingsScreen() } } }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++ // re-read permissions and today's stats when coming back from Settings or the lock screen
        val prefs = Prefs(this)
        if (prefs.enabled && LockService.canRun(this)) LockService.start(this)
    }
}

private data class Palette(val bg: Color, val fg: Color, val muted: Color, val line: Color, val panel: Color, val ok: Color)

private val LocalPalette = staticCompositionLocalOf {
    Palette(Color(0xFFF3F2EF), Color(0xFF17191B), Color(0xFF6F7275), Color(0xFFE1DFDA), Color(0xFFFBFAF8), Color(0xFF2F8A5E))
}

@Composable
private fun WordLockTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Palette(Color(0xFF0E0F10), Color(0xFFECEBE8), Color(0xFF8D8F92), Color(0xFF25272A), Color(0xFF151618), Color(0xFF5BC894))
    else LocalPalette.current
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.fg, onPrimary = p.bg, background = p.bg, surface = p.bg, onBackground = p.fg, onSurface = p.fg,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val p = LocalPalette.current
    val prefs = remember { Prefs(context) }

    var enabled by remember { mutableStateOf(prefs.enabled) }
    var level by remember { mutableStateOf(prefs.level) }
    var pin by remember { mutableStateOf(prefs.pin) }
    var pinSaved by remember { mutableStateOf(false) }

    val overlayOk = Settings.canDrawOverlays(context)
    var notifOk by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val batteryOk = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }

    // If the user turned it on before granting the overlay permission, it can't run yet.
    val running = enabled && overlayOk

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Text("ENGLISH LOCK SCREEN", color = p.muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("Word Lock", color = p.fg, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "화면을 켤 때마다 그림이 나오고, 영어 단어를 맞혀야 열려요.",
            color = p.muted, fontSize = 14.5.sp, lineHeight = 22.sp,
        )

        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth().background(p.panel, RoundedCornerShape(18.dp)).border(1.dp, p.line, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("잠금 퀴즈", color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    when {
                        running -> "켜짐 · 다음에 화면을 켜면 나와요"
                        enabled -> "아래 ‘다른 앱 위에 표시’를 먼저 허용하세요"
                        else -> "꺼짐"
                    },
                    color = if (running) p.ok else p.muted, fontSize = 13.sp,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it; prefs.enabled = it
                    if (it) {
                        if (!notifOk && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        if (LockService.canRun(context)) LockService.start(context)
                    } else LockService.stop(context)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = p.fg, checkedThumbColor = p.bg),
            )
        }

        Section("권한")
        PermissionRow("다른 앱 위에 표시", "필수 · 잠금화면 위에 퀴즈를 띄워요", overlayOk) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }
        if (Build.VERSION.SDK_INT >= 33) {
            PermissionRow("알림", "권장 · 앱이 꺼지지 않게 작은 알림을 띄워요", notifOk) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        PermissionRow("배터리 최적화 제외", "권장 · 휴대폰이 앱을 멋대로 끄지 않게 해요", batteryOk) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }

        Section("난이도")
        Row(
            Modifier.background(p.panel, RoundedCornerShape(12.dp)).border(1.dp, p.line, RoundedCornerShape(12.dp)).padding(3.dp)
        ) {
            Level.entries.forEach { l ->
                val on = l == level
                Text(
                    l.label,
                    color = if (on) p.bg else p.muted, fontSize = 14.sp,
                    modifier = Modifier
                        .background(if (on) p.fg else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { level = l; prefs.level = l }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        Text(level.help, color = p.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 8.dp))

        Section("비상 PIN")
        Text("급할 때 퀴즈 없이 바로 여는 숫자 4자리예요.", color = p.muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter(Char::isDigit).take(4); pinSaved = false },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(color = p.fg, fontSize = 22.sp, letterSpacing = 8.sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(p.fg),
                modifier = Modifier
                    .width(140.dp)
                    .background(p.panel, RoundedCornerShape(12.dp))
                    .border(1.dp, p.line, RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp),
            )
            Spacer(Modifier.width(12.dp))
            val valid = pin.length == 4
            Text(
                if (pinSaved) "저장됨" else "저장",
                color = if (valid && !pinSaved) p.bg else p.muted,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(if (valid && !pinSaved) p.fg else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable(enabled = valid && !pinSaved) { prefs.pin = pin; pinSaved = true }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
        if (pin == "0000") Text("기본 PIN(0000)이에요. 바꿔 두는 걸 추천해요.", color = p.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(26.dp))
        Text(
            "잠금화면 미리보기",
            color = p.bg, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(p.fg, RoundedCornerShape(14.dp))
                .clickable { LockActivity.show(context, preview = true) }.padding(vertical = 15.dp),
        )

        Section("오늘")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("맞힌 단어", prefs.todayCount.toString(), Modifier.weight(1f))
            Stat("힌트 없이 연속", prefs.streak.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        val log = prefs.log
        if (log.isEmpty()) {
            Text("아직 맞힌 단어가 없어요. 화면을 껐다 켜 보세요.", color = p.muted, fontSize = 13.sp)
        } else {
            log.take(20).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e.picture, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(e.english, color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text(e.korean, color = p.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(e.time, color = p.muted, fontSize = 12.sp)
                }
                HorizontalDivider(color = p.line)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    val p = LocalPalette.current
    Spacer(Modifier.height(32.dp))
    Text(title, color = p.muted, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean, onRequest: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.fg, fontSize = 15.sp)
            Text(desc, color = p.muted, fontSize = 12.5.sp)
        }
        if (granted) Text("허용됨", color = p.ok, fontSize = 13.sp)
        else Text(
            "허용하기", color = p.fg, fontSize = 13.sp,
            modifier = Modifier.border(1.dp, p.fg, RoundedCornerShape(999.dp)).clickable(onClick = onRequest)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
    HorizontalDivider(color = p.line)
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.background(p.panel, RoundedCornerShape(16.dp)).border(1.dp, p.line, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(value, color = p.fg, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Text(label, color = p.muted, fontSize = 12.sp)
    }
}
