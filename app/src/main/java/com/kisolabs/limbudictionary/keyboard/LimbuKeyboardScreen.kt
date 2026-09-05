package com.kisolabs.limbudictionary.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kisolabs.limbudictionary.R

enum class KeyboardMode { DEFAULT, SHIFT, ALT }
enum class ToolbarView { KEYBOARD, EMOJI, CLIPBOARD }

data class KeySpec(val default: String, val shift: String = "", val alt: String = "")

data class ActivePreview(
    val character: String,
    val position: Offset
)

val LimbuFontFamily = FontFamily(
    Font(R.font.noto_sans_limbu_regular, FontWeight.Normal)
)

// Standard Gboard Light Palette
val GboardLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFF444746),
    surfaceContainerLow = Color(0xFFDCDFE3),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF202124)
)

// Standard Gboard Dark Palette
val GboardDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFC2E7FF),
    surface = Color(0xFF303134),
    onSurface = Color(0xFFE3E5E8),
    surfaceVariant = Color(0xFF1E1F22),
    onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceContainerLow = Color(0xFF242528),
    background = Color(0xFF1E1F22),
    onBackground = Color(0xFFE3E5E8)
)

fun Modifier.allowUnboundedChildren() = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

@Composable
fun LimbuKeyboardScreen(
    resetSignal: Int = 0,
    clipboardHistory: List<String>,
    onInput: (String) -> Unit, 
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    getWordAtCursor: () -> String,
    replaceCurrentWord: (String, Int) -> Unit,
    onOpenSettings: () -> Unit,
    onClearClipboard: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val prefs = remember { context.getSharedPreferences("limbu_prefs", Context.MODE_PRIVATE) }
    var themeOption by remember { mutableIntStateOf(prefs.getInt("theme_option", 0)) }
    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var keyPopupEnabled by remember { mutableStateOf(prefs.getBoolean("key_popup_enabled", true)) }

    var measuredKeyboardHeightDp by remember { mutableStateOf(242.dp) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "theme_option" -> themeOption = p.getInt("theme_option", 0)
                "haptic_enabled" -> hapticEnabled = p.getBoolean("haptic_enabled", true)
                "sound_enabled" -> soundEnabled = p.getBoolean("sound_enabled", true)
                "key_popup_enabled" -> keyPopupEnabled = p.getBoolean("key_popup_enabled", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val playClickSound = {
        if (soundEnabled) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
    }

    val isDarkTheme = when (themeOption) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        LimbuDictionaryHelper.load(context)
    }

    var mode by remember { mutableStateOf(KeyboardMode.DEFAULT) }
    var currentView by remember { mutableStateOf(ToolbarView.KEYBOARD) }
    var currentWord by remember { mutableStateOf("") }
    var activePreview by remember { mutableStateOf<ActivePreview?>(null) }
    var activeAltKey by remember { mutableStateOf<Pair<String, Offset>?>(null) }

    // Clear state when service notifies open/close or field change
    LaunchedEffect(resetSignal) {
        currentWord = ""
        activePreview = null
        activeAltKey = null
    }

    LaunchedEffect(activeAltKey) {
        if (activeAltKey != null) {
            delay(3000)
            activeAltKey = null
        }
    }

    val suggestions: List<String> = remember(currentWord) { 
        LimbuDictionaryHelper.getSuggestions(currentWord) 
    }

    val updateWordState = { currentWord = getWordAtCursor() }

    val handleCharacterInput: (String) -> Unit = { text ->
        playClickSound()
        onInput(text)
        updateWordState()
        activeAltKey = null
    }

    val handleBackspace = {
        playClickSound()
        onBackspace()
        updateWordState()
    }

    val handleSuggestionClick: (String) -> Unit = { selectedWord ->
        playClickSound()
        replaceCurrentWord("$selectedWord ", currentWord.length)
        currentWord = ""
    }

    val handlePaste = {
        playClickSound()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val textToPaste = clipData.getItemAt(0).text?.toString() ?: ""
            if (textToPaste.isNotEmpty()) {
                onInput(textToPaste)
                updateWordState()
            }
        }
    }

    val row1 = listOf(
        KeySpec("᥇", "᥄"), KeySpec("᥈", "@"), KeySpec("᥉", "#"), KeySpec("᥊", "$"),
        KeySpec("᥋", "%"), KeySpec("᥌", "^"), KeySpec("᥍", "&"), KeySpec("᥎", "*"),
        KeySpec("᥏", "("), KeySpec("᥆", ")"), KeySpec("-", "_"), KeySpec("=", "+")
    )
    val row2 = listOf(
        KeySpec("ᤀ", "᤹", "᤹"), KeySpec("ᤘ", "ᤫ"), KeySpec("ᤣ", "ᤧ", "ᤤ"), KeySpec("ᤖ", "ᤪ", "ᤷ"),
        KeySpec("ᤋ", "ᤌ", "ᤳ"), KeySpec("ᤕ", "ᤩ", "ᤊ"), KeySpec("ᤢ", "", ""), KeySpec("ᤡ", "", ""),
        KeySpec("ᤥ", "ᤨ", "ᤦ"), KeySpec("ᤐ", "ᤑ", "ᤵ")
    )
    val row3 = listOf(
        KeySpec("ᤠ", ""), KeySpec("ᤛ", "ᤚ"), KeySpec("ᤍ", "ᤎ"), KeySpec("ᤃ", "ᤄ"),
        KeySpec("ᤜ", "᤺"), KeySpec("ᤈ", "ᤉ"), KeySpec("ᤁ", "ᤂ", "ᤰ"), KeySpec("ᤗ", "", "ᤸ")
    )
    val row4 = listOf(
        KeySpec("ᤙ", ""), KeySpec("᤻", ""), KeySpec("ᤆ", "ᤇ"), KeySpec("᥀", ""),
        KeySpec("ᤒ", "ᤓ"), KeySpec("ᤏ", "ᤱ", "ᤴ"), KeySpec("ᤔ", "ᤅ", "ᤶ"),
        KeySpec(",", "<"), KeySpec(".", ">", "॥"), KeySpec("/", "᥅", "।")
    )

    MaterialTheme(
        colorScheme = if (isDarkTheme) GboardDarkColorScheme else GboardLightColorScheme
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .allowUnboundedChildren()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                ) {
                    // Toolbar Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (suggestions.isNotEmpty() && currentView == ToolbarView.KEYBOARD) {
                                for (word in suggestions) {
                                    TextButton(
                                        onClick = { handleSuggestionClick(word) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = word,
                                            fontSize = 16.sp,
                                            fontFamily = LimbuFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                ToolbarIconButton(
                                    iconRes = R.drawable.ic_paste,
                                    contentDescription = "Paste",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { 
                                        currentView = ToolbarView.KEYBOARD
                                        handlePaste() 
                                    }
                                )

                                ToolbarIconButton(
                                    iconRes = R.drawable.ic_clipboard,
                                    contentDescription = "Clipboard",
                                    tint = if (currentView == ToolbarView.CLIPBOARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { 
                                        playClickSound()
                                        currentView = if (currentView == ToolbarView.CLIPBOARD) ToolbarView.KEYBOARD else ToolbarView.CLIPBOARD
                                    }
                                )

                                ToolbarIconButton(
                                    iconRes = R.drawable.ic_emoji,
                                    contentDescription = "Emoji",
                                    tint = if (currentView == ToolbarView.EMOJI) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { 
                                        playClickSound()
                                        currentView = if (currentView == ToolbarView.EMOJI) ToolbarView.KEYBOARD else ToolbarView.EMOJI 
                                    }
                                )

                                ToolbarIconButton(
                                    iconRes = R.drawable.ic_settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { 
                                        playClickSound()
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }

                    // Main View Content Switcher
                    when (currentView) {
                        ToolbarView.KEYBOARD -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        val heightInDp = with(density) { coordinates.size.height.toDp() }
                                        if (heightInDp > 0.dp) {
                                            measuredKeyboardHeightDp = heightInDp
                                        }
                                    }
                            ) {
                                listOf(row1, row2, row3, row4).forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        row.forEach { key ->
                                            KeyButton(
                                                spec = key,
                                                mode = mode,
                                                hapticEnabled = hapticEnabled,
                                                modifier = Modifier.weight(1f),
                                                onClick = handleCharacterInput,
                                                onLongPressAlt = { altChar, pos ->
                                                    activeAltKey = Pair(altChar, pos)
                                                },
                                                onTriggerPreview = { char, pos ->
                                                    if (keyPopupEnabled) {
                                                        activePreview = ActivePreview(char, pos)
                                                    }
                                                },
                                                onDismissPreview = { activePreview = null }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    DrawableActionButton(
                                        iconRes = R.drawable.ic_arrow_drop_up,
                                        isActive = mode == KeyboardMode.SHIFT,
                                        hapticEnabled = hapticEnabled,
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        playClickSound()
                                        mode = if (mode == KeyboardMode.SHIFT) KeyboardMode.DEFAULT else KeyboardMode.SHIFT
                                    }

                                    TextActionButton(
                                        label = "ALT",
                                        isActive = mode == KeyboardMode.ALT,
                                        hapticEnabled = hapticEnabled,
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        playClickSound()
                                        mode = if (mode == KeyboardMode.ALT) KeyboardMode.DEFAULT else KeyboardMode.ALT
                                    }

                                    TextActionButton(
                                        label = "Space",
                                        isSpacebar = true,
                                        hapticEnabled = hapticEnabled,
                                        modifier = Modifier.weight(3.6f)
                                    ) {
                                        playClickSound()
                                        onInput(" ")
                                        updateWordState()
                                    }

                                    RepeatableDrawableActionButton(
                                        iconRes = R.drawable.ic_backspace,
                                        hapticEnabled = hapticEnabled,
                                        modifier = Modifier.weight(1.3f),
                                        onAction = handleBackspace
                                    )

                                    DrawableActionButton(
                                        iconRes = R.drawable.ic_keyboard_return,
                                        isPrimaryAction = true,
                                        hapticEnabled = hapticEnabled,
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        playClickSound()
                                        onEnter()
                                        updateWordState()
                                    }
                                }
                            }
                        }

                        ToolbarView.EMOJI -> {
                            EmojiLayout(
                                contentHeight = measuredKeyboardHeightDp,
                                onEmojiSelect = { emoji -> 
                                    playClickSound()
                                    onInput(emoji) 
                                },
                                onBackspace = handleBackspace,
                                onSwitchToABC = { 
                                    playClickSound()
                                    currentView = ToolbarView.KEYBOARD 
                                }
                            )
                        }

                        ToolbarView.CLIPBOARD -> {
                            ClipboardLayout(
                                history = clipboardHistory,
                                contentHeight = measuredKeyboardHeightDp,
                                onClipSelect = { text ->
                                    playClickSound()
                                    onInput(text)
                                    currentView = ToolbarView.KEYBOARD
                                },
                                onClearAll = onClearClipboard,
                                onSwitchToABC = {
                                    playClickSound()
                                    currentView = ToolbarView.KEYBOARD
                                }
                            )
                        }
                    }
                }
            }

            // Preview Popup Overlay
            if (keyPopupEnabled) {
                activePreview?.let { preview ->
                    val xDp = with(density) { preview.position.x.toDp() }
                    val yDp = with(density) { preview.position.y.toDp() }

                    Box(
                        modifier = Modifier
                            .offset(x = xDp - 4.dp, y = yDp - 54.dp)
                            .size(width = 48.dp, height = 56.dp)
                            .shadow(6.dp, RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preview.character,
                            fontSize = 24.sp,
                            fontFamily = LimbuFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Long Press Popup Overlay
            activeAltKey?.let { (altChar, pos) ->
                val xDp = with(density) { pos.x.toDp() }
                val yDp = with(density) { pos.y.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = xDp - 4.dp, y = yDp - 48.dp)
                        .shadow(6.dp, RoundedCornerShape(8.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures {
                                handleCharacterInput(altChar)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = altChar,
                        fontSize = 20.sp,
                        fontFamily = LimbuFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarIconButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ClipboardLayout(
    history: List<String>,
    contentHeight: Dp,
    onClipSelect: (String) -> Unit,
    onClearAll: () -> Unit,
    onSwitchToABC: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clipboard),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Clipboard",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clipboard),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Copied items will show up here",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                itemsIndexed(history) { _, clip ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClipSelect(clip) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_paste),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = clip,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${clip.length} chars",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextActionButton(
                label = "ᤁᤂᤃ",
                modifier = Modifier.weight(1.5f),
                onClick = onSwitchToABC
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .weight(5.5f)
                    .fillMaxHeight()
            ) {}
        }
    }
}

@Composable
fun KeyButton(
    spec: KeySpec,
    mode: KeyboardMode,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    onLongPressAlt: (String, Offset) -> Unit,
    onTriggerPreview: (String, Offset) -> Unit,
    onDismissPreview: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var keyPosition by remember { mutableStateOf(Offset.Zero) }

    val activeChar = when (mode) {
        KeyboardMode.DEFAULT -> spec.default
        KeyboardMode.SHIFT -> spec.shift.ifEmpty { spec.default }
        KeyboardMode.ALT -> spec.alt.ifEmpty { spec.default }
    }

    val alternateChar = when (mode) {
        KeyboardMode.DEFAULT -> spec.alt.ifEmpty { spec.shift }
        else -> spec.default
    }

    fun triggerHaptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            keyPosition = coordinates.positionInRoot()
        },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .pointerInput(activeChar, alternateChar, hapticEnabled) {
                    detectTapGestures(
                        onTap = {
                            if (activeChar.isNotEmpty()) {
                                triggerHaptic()
                                onClick(activeChar)
                                onTriggerPreview(activeChar, keyPosition)
                                coroutineScope.launch {
                                    delay(150)
                                    onDismissPreview()
                                }
                            }
                        },
                        onLongPress = {
                            if (alternateChar.isNotEmpty() && alternateChar != activeChar) {
                                triggerHaptic()
                                onLongPressAlt(alternateChar, keyPosition)
                            }
                        }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = activeChar,
                    fontSize = 17.sp,
                    fontFamily = LimbuFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TextActionButton(
    label: String,
    isActive: Boolean = false,
    isSpacebar: Boolean = false,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isSpacebar -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        isSpacebar -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(46.dp)
            .pointerInput(hapticEnabled) {
                detectTapGestures(onTap = {
                    if (hapticEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    onClick()
                })
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}

@Composable
fun DrawableActionButton(
    iconRes: Int,
    isActive: Boolean = false,
    isPrimaryAction: Boolean = false,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val backgroundColor = when {
        isPrimaryAction -> MaterialTheme.colorScheme.primaryContainer
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        isPrimaryAction -> MaterialTheme.colorScheme.onPrimaryContainer
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(46.dp)
            .pointerInput(hapticEnabled) {
                detectTapGestures(onTap = {
                    if (hapticEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    onClick()
                })
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RepeatableDrawableActionButton(
    iconRes: Int,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isPressed) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(46.dp)
            .pointerInput(hapticEnabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (hapticEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        onAction()
                        val job = coroutineScope.launch {
                            delay(400)
                            while (true) {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                onAction()
                                delay(50)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                        isPressed = false
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
