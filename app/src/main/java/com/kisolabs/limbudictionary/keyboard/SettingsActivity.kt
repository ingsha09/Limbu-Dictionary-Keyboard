package com.kisolabs.limbudictionary.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kisolabs.limbudictionary.R

val LightBackground = Color(0xFFFCFDFE)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE1E4E8)
val LightTextPrimary = Color(0xFF1F2328)
val LightTextSecondary = Color(0xFF656D76)
val AccentSelection = Color(0xFFE8F0FE)

private val AppLightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentSelection,
    onPrimaryContainer = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF9FBFE),
    onSurfaceVariant = LightTextSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    outline = LightBorder
)

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val googleSans = FontFamily(Font("fonts/googlesans.ttf", assets))
        val customTypography = Typography().run {
            copy(
                displayLarge = displayLarge.copy(fontFamily = googleSans),
                displayMedium = displayMedium.copy(fontFamily = googleSans),
                displaySmall = displaySmall.copy(fontFamily = googleSans),
                headlineLarge = headlineLarge.copy(fontFamily = googleSans),
                headlineMedium = headlineMedium.copy(fontFamily = googleSans),
                headlineSmall = headlineSmall.copy(fontFamily = googleSans),
                titleLarge = titleLarge.copy(fontFamily = googleSans),
                titleMedium = titleMedium.copy(fontFamily = googleSans),
                titleSmall = titleSmall.copy(fontFamily = googleSans),
                bodyLarge = bodyLarge.copy(fontFamily = googleSans),
                bodyMedium = bodyMedium.copy(fontFamily = googleSans),
                bodySmall = bodySmall.copy(fontFamily = googleSans),
                labelLarge = labelLarge.copy(fontFamily = googleSans),
                labelMedium = labelMedium.copy(fontFamily = googleSans),
                labelSmall = labelSmall.copy(fontFamily = googleSans)
            )
        }

        setContent {
            val view = LocalView.current

            SideEffect {
                val window = (view.context as ComponentActivity).window
                val insetsController = WindowCompat.getInsetsController(window, view)
                
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }

            MaterialTheme(
                colorScheme = AppLightColorScheme,
                typography = customTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(onBackClick = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("limbu_prefs", Context.MODE_PRIVATE) }

    var themeOption by remember { mutableIntStateOf(prefs.getInt("theme_option", 0)) }
    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var keyPopupEnabled by remember { mutableStateOf(prefs.getBoolean("key_popup_enabled", true)) }
    var testText by remember { mutableStateOf("") }

    var isKeyboardEnabled by remember { mutableStateOf(isLimbuKeyboardEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isKeyboardEnabled = isLimbuKeyboardEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun updateKeyboardTheme(option: Int) {
        themeOption = option
        prefs.edit().putInt("theme_option", option).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Keyboard Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Setup Section
                SectionHeaderDrawable(iconResId = R.drawable.ic_keyboard, title = "Keyboard Setup")
                CardContainer {
                    Column {
                        if (!isKeyboardEnabled) {
                            SettingActionItem(
                                title = "Enable Keyboard",
                                subtitle = "Enable Limbu Keyboard in system settings",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        SettingActionItem(
                            title = "Select Input Method",
                            subtitle = "Switch current active keyboard to Limbu Keyboard",
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            }
                        )
                    }
                }

                // Keyboard Appearance Theme Options
                SectionHeaderDrawable(iconResId = R.drawable.ic_theme, title = "Keyboard Theme")
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Mode",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedThemePicker(
                            selectedOption = themeOption,
                            onOptionSelected = { updateKeyboardTheme(it) }
                        )
                    }
                }

                // Feedback & Behavior Section
                SectionHeaderDrawable(iconResId = R.drawable.ic_settings, title = "Feedback & Behaviour")
                CardContainer {
                    Column {
                        SettingToggleItem(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on key press",
                            checked = hapticEnabled,
                            onCheckedChange = {
                                hapticEnabled = it
                                prefs.edit().putBoolean("haptic_enabled", it).apply()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        SettingToggleItem(
                            title = "Sound on Keypress",
                            subtitle = "Play audio tap sound",
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                prefs.edit().putBoolean("sound_enabled", it).apply()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        SettingToggleItem(
                            title = "Key Popup Preview",
                            subtitle = "Show enlarged character preview on tap",
                            checked = keyPopupEnabled,
                            onCheckedChange = {
                                keyPopupEnabled = it
                                prefs.edit().putBoolean("key_popup_enabled", it).apply()
                            }
                        )
                    }
                }

                // Test Keyboard Input Field
                SectionHeaderDrawable(iconResId = R.drawable.ic_keyboard, title = "Test Keyboard")
                CardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tap here to test keyboard...") },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun isLimbuKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    val enabledMethods = imm.enabledInputMethodList
    val packageName = context.packageName
    return enabledMethods.any { it.packageName == packageName }
}

@Composable
fun SectionHeaderDrawable(iconResId: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CardContainer(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun SettingActionItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SegmentedThemePicker(selectedOption: Int, onOptionSelected: (Int) -> Unit) {
    val options = listOf("System", "Light", "Dark")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = selectedOption == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
