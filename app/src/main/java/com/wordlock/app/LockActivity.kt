package com.wordlock.app

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

private val Sand = Color(0xFFE6D3B3)
private val Ink = Color(0xFF0F1719)
private val Mint = Color(0xFFBEF0D7)

class LockActivity : ComponentActivity() {

    private var preview = false
    private var session by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (preview) finish() }
        })
        setContent {
            key(session) {
                LockScreen(
                    prefs = remember { Prefs(this) },
                    preview = preview,
                    onUnlocked = ::unlock,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        session++ // fresh word every time the screen goes off again
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (!preview && km.isKeyguardLocked) {
            // Hand over to the phone's own lock (PIN / fingerprint / swipe).
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = finish()
                override fun onDismissCancelled() = finish()
                override fun onDismissError() = finish()
            })
        } else {
            finish()
        }
    }

    companion object {
        const val EXTRA_PREVIEW = "preview"
        fun show(context: Context, preview: Boolean = false) {
            context.startActivity(
                Intent(context, LockActivity::class.java)
                    .putExtra(EXTRA_PREVIEW, preview)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
        }
    }
}

// ---------------------------------------------------------------- UI

private enum class Mode { QUIZ, PIN, SOLVED }

@Composable
internal fun LockScreen(prefs: Prefs, preview: Boolean, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val level = remember { prefs.level }

    var word by remember { mutableStateOf(Words.random(level)) }
    var typed by remember { mutableStateOf("") }
    var revealed by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var usedHint by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(Mode.QUIZ) }
    var correct by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(false) }
    val tileShake = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }

    val maxLen = word.accepted.maxOf { it.length }

    fun newWord() {
        word = Words.random(level, except = word)
        typed = ""; revealed = 0; misses = 0; usedHint = false; message = ""
    }

    fun hint(auto: Boolean = false) {
        val l = word.letters
        if (revealed < l.length - 1) {
            revealed++; usedHint = true
            if (typed.length < revealed && typed == l.take(typed.length)) typed = l.take(revealed - 1)
            if (!auto) message = "글자 하나를 열었어요"
        } else message = "더 열 수 있는 글자가 없어요"
    }

    fun shakeToChange() {
        if (mode != Mode.QUIZ || correct) return
        vibrate(context, 40)
        prefs.resetStreak()
        scope.launch {
            screenShake.snapTo(0f)
            for (x in listOf(-14f, 14f, -10f, 10f, -5f, 0f)) screenShake.animateTo(x, tween(55))
        }
        newWord()
        toast = true
    }

    fun submit() {
        if (typed.isEmpty() || correct) return
        if (typed in word.accepted) {
            correct = true
            message = "정답이에요"
            if (!preview) prefs.recordSolved(word, usedHint)
            scope.launch {
                delay(350); mode = Mode.SOLVED
                delay(1100); onUnlocked()
            }
            return
        }
        misses++
        vibrate(context, 25)
        scope.launch {
            for (x in listOf(-18f, 18f, -12f, 12f, 0f)) tileShake.animateTo(x, tween(50))
        }
        if (misses % 3 == 0) { hint(auto = true); message = "3번 틀려서 글자 하나를 열었어요" }
        else message = "다시 해 보세요 · ${3 - misses % 3}번 더 틀리면 힌트"
    }

    ShakeDetector(enabled = mode == Mode.QUIZ && !correct, onShake = ::shakeToChange)

    LaunchedEffect(toast) { if (toast) { delay(1300); toast = false } }

    Box(
        Modifier
            .fillMaxSize()
            .wallpaper()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = screenShake.value * density }
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Clock(Modifier.padding(top = 28.dp))

            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = word,
                    transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = .7f)) togetherWith fadeOut(tween(150)) },
                    label = "picture",
                ) { w -> Text(w.picture, fontSize = 84.sp, lineHeight = 96.sp) }
                Spacer(Modifier.height(14.dp))
                Text("이 그림을 영어로", color = Color.White.copy(alpha = .65f), fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(14.dp))
                Tiles(
                    word = word, typed = typed, revealed = revealed, correct = correct,
                    modifier = Modifier.graphicsLayer { translationX = tileShake.value * density },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    message.ifEmpty { " " }, color = Color.White.copy(alpha = .78f),
                    fontSize = 12.5.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    TextLink("힌트") { hint() }
                    TextLink("흔들어서 바꾸기", ::shakeToChange)
                    TextLink("비상 PIN") { mode = Mode.PIN }
                }
            }

            Keyboard(
                onKey = { if (!correct && typed.length < maxLen) typed += it },
                onDelete = { if (!correct) typed = typed.dropLast(1) },
                onHint = { hint() },
                onSubmit = ::submit,
            )
        }

        AnimatedVisibility(toast, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)) {
            Text(
                "새 문제로 바뀌었어요",
                color = Color.White, fontSize = 12.5.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = .16f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .2f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        AnimatedVisibility(mode == Mode.PIN, enter = fadeIn(), exit = fadeOut()) {
            PinPad(
                correctPin = prefs.pin,
                hint = if (preview) "미리보기 · 설정한 PIN을 입력하세요" else null,
                onSuccess = onUnlocked,
                onCancel = { mode = Mode.QUIZ },
            )
        }

        AnimatedVisibility(mode == Mode.SOLVED, enter = fadeIn(tween(300)), exit = fadeOut()) {
            Solved(word)
        }

        if (preview) {
            Text(
                "미리보기", color = Sand, fontSize = 11.sp, letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 20.dp, top = 6.dp),
            )
        }
    }
}

private fun Modifier.wallpaper() = drawBehind {
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1D2A2C), Color(0xFF0F1719), Color(0xFF0B1012))))
    drawRect(Brush.radialGradient(listOf(Sand.copy(alpha = .5f), Color.Transparent), Offset(w * .72f, h * .16f), w * .8f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF466868).copy(alpha = .55f), Color.Transparent), Offset(w * .05f, h * .58f), w * .75f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF2A3A46).copy(alpha = .9f), Color.Transparent), Offset(w * .85f, h), w))
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(5_000); now = LocalDateTime.now() } }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
            color = Color.White.copy(alpha = .85f), fontSize = 15.sp, fontWeight = FontWeight.Medium,
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("H:mm")),
            color = Color.White, fontSize = 86.sp, lineHeight = 90.sp,
            fontWeight = FontWeight.Thin, letterSpacing = (-2).sp, fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun Tiles(word: Word, typed: String, revealed: Int, correct: Boolean, modifier: Modifier = Modifier) {
    val letters = word.letters
    val count = maxOf(letters.length, typed.length)
    // Positions (in letter index) where the word has a space, to draw a small gap.
    val gaps = buildSet {
        var i = 0
        word.english.forEach { c -> if (c == ' ') add(i) else i++ }
    }
    val tileW = when { count <= 6 -> 38.dp; count <= 9 -> 30.dp; else -> 24.dp }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (i in 0 until count) {
            if (i in gaps) Spacer(Modifier.width(6.dp))
            val c = typed.getOrNull(i)
            val isCursor = i == typed.length && !correct
            val bg = when {
                correct -> Mint.copy(alpha = .28f)
                c != null -> Color.White.copy(alpha = .18f)
                else -> Color.White.copy(alpha = .08f)
            }
            val border = when {
                correct -> Mint.copy(alpha = .7f)
                isCursor -> Sand
                c != null -> Color.White.copy(alpha = .4f)
                else -> Color.White.copy(alpha = .18f)
            }
            Box(
                Modifier
                    .size(tileW, 46.dp)
                    .background(bg, RoundedCornerShape(10.dp))
                    .border(if (isCursor) 1.5.dp else 1.dp, border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    c != null -> Text(
                        c.toString(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        color = if (i < revealed && c == letters.getOrNull(i)) Sand else Color.White,
                    )
                    i < revealed -> Text(letters[i].toString(), fontSize = 20.sp, color = Sand.copy(alpha = .5f))
                }
            }
        }
    }
}

@Composable
private fun TextLink(label: String, onClick: () -> Unit) {
    Text(
        label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp),
    )
}

@Composable
private fun Keyboard(onKey: (Char) -> Unit, onDelete: () -> Unit, onHint: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x66141A1C))
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEachIndexed { r, row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (r == 1) 18.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEach { ch -> Key(ch.toString(), Modifier.weight(1f)) { onKey(ch) } }
                if (r == 2) Key("⌫", Modifier.weight(1.6f), dim = true, onClick = onDelete)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Key("힌트", Modifier.weight(1.5f), dim = true, small = true, onClick = onHint)
            Key("Word Lock", Modifier.weight(4f), dim = true, small = true, faint = true) {}
            Key("확인", Modifier.weight(2f), accent = true, small = true, onClick = onSubmit)
        }
    }
}

@Composable
private fun Key(
    label: String, modifier: Modifier, dim: Boolean = false, small: Boolean = false,
    accent: Boolean = false, faint: Boolean = false, onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg = when {
        accent -> Sand
        pressed -> Color.White.copy(alpha = .4f)
        dim -> Color.White.copy(alpha = .1f)
        else -> Color.White.copy(alpha = .2f)
    }
    Box(
        modifier
            .height(46.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when { accent -> Color(0xFF1A1712); faint -> Color.White.copy(alpha = .5f); else -> Color.White },
            fontSize = if (small) 13.sp else 19.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PinPad(correctPin: String, hint: String?, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    val context = LocalContext.current
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            delay(150)
            if (pin == correctPin) onSuccess()
            else {
                vibrate(context, 60)
                for (x in listOf(-14f, 14f, -8f, 8f, 0f)) shake.animateTo(x, tween(50))
                pin = ""
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xD9080C0D)).clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("비상 PIN 입력", color = Color.White, fontSize = 16.sp)
        if (hint != null) Text(hint, color = Color.White.copy(alpha = .6f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(26.dp))
        Row(Modifier.graphicsLayer { translationX = shake.value * density }, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(12.dp)
                        .background(if (i < pin.length) Color.White else Color.Transparent, CircleShape)
                        .border(1.2.dp, Color.White, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "취소", "0", "지우기")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                row.forEach { k ->
                    val isText = k.length > 1
                    Box(
                        Modifier.size(76.dp)
                            .background(if (isText) Color.Transparent else Color.White.copy(alpha = .14f), CircleShape)
                            .clickable {
                                when (k) {
                                    "취소" -> onCancel()
                                    "지우기" -> pin = pin.dropLast(1)
                                    else -> if (pin.length < 4) pin += k
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            k, color = Color.White,
                            style = if (isText) TextStyle(fontSize = 14.sp) else TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Light),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Solved(word: Word) {
    Column(
        Modifier.fillMaxSize().background(Color(0x99080C0D)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(word.picture, fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text(word.english, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
        Text(word.korean, color = Color.White.copy(alpha = .7f), fontSize = 16.sp)
    }
}

// ---------------------------------------------------------------- sensors

@Composable
private fun ShakeDetector(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onShake)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (g > 2.6f && now - last > 1200) { last = now; latest() }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
}

private fun vibrate(context: Context, ms: Long) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    runCatching { v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
