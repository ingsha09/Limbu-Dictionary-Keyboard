package com.kisolabs.limbudictionary.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kisolabs.limbudictionary.R

object EmojiData {
    val categories = listOf("😃", "👍", "❤️", "🐶", "🍕", "⚽", "💡", "🚩")

    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊",
        "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪",
        "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏",
        "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸", "😎",
        "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦", "😧"
    )

    val gestures = listOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🤟", "🤘",
        "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
        "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾",
        "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️"
    )

    val hearts = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕",
        "💞", "💓", "💗", "💖", "💘", "💝", "💟", "🔥", "✨", "🌟", "💫", "💥",
        "💢", "💦", "💧", "💤", "💬", "👁️‍🗨️", "🗨️", "🗯️", "💭", "💯", "🎉", "🎊"
    )

    val animalsAndNature = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁",
        "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦",
        "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝",
        "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎"
    )

    val foodAndDrink = listOf(
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒",
        "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️",
        "🌽", "🥕", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀",
        "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌭", "🍔"
    )

    val activities = listOf(
        "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓",
        "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🥽",
        "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🏂", "🪂"
    )

    val objectsAndSymbols = listOf(
        "💡", "🔦", "🏮", "🧱", "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
        "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🦯", "🦽", "🦼", "🛴", "🚲", "🛵",
        "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🎛️", "🎙️", "📻"
    )

    val flags = listOf(
        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇸",
        "🇦🇩", "🇦🇴", "🇦🇮", "🇦🇶", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇼", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸",
        "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪", "🇧🇿", "🇧🇯", "🇧🇲", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼"
    )
}

@Composable
fun EmojiLayout(
    onEmojiSelect: (String) -> Unit,
    onBackspace: () -> Unit,
    onSwitchToABC: () -> Unit,
    modifier: Modifier = Modifier,
    contentHeight: Dp = 242.dp
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()

    val currentList by remember {
        derivedStateOf {
            when (selectedCategory) {
                0 -> EmojiData.smileys
                1 -> EmojiData.gestures
                2 -> EmojiData.hearts
                3 -> EmojiData.animalsAndNature
                4 -> EmojiData.foodAndDrink
                5 -> EmojiData.activities
                6 -> EmojiData.objectsAndSymbols
                else -> EmojiData.flags
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        gridState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            EmojiData.categories.forEachIndexed { index, categoryIcon ->
                val isSelected = selectedCategory == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategory = index },
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = categoryIcon,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(9),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(
                items = currentList,
                key = { emoji -> emoji }
            ) { emoji ->
                EmojiCell(emoji = emoji, onClick = { onEmojiSelect(emoji) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            EmojiTextActionButton(
                label = "ᤁᤂᤃ",
                isActive = false,
                modifier = Modifier.weight(1.3f),
                onClick = onSwitchToABC
            )

            EmojiTextActionButton(
                label = "Space",
                isSpacebar = true,
                modifier = Modifier.weight(4.9f),
                onClick = { onEmojiSelect(" ") }
            )

            EmojiRepeatableActionButton(
                iconRes = R.drawable.ic_backspace,
                modifier = Modifier.weight(1.3f),
                onAction = onBackspace
            )
        }
    }
}

@Composable
fun EmojiCell(
    emoji: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                currentOnClick()
            }
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp
        )
    }
}

@Composable
fun EmojiTextActionButton(
    label: String,
    isActive: Boolean = false,
    isSpacebar: Boolean = false,
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
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
fun EmojiRepeatableActionButton(
    iconRes: Int,
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
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onAction()
                        val job = coroutineScope.launch {
                            delay(400)
                            while (true) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
