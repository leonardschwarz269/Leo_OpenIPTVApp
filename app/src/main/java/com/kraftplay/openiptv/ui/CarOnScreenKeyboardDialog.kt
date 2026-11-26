package com.kraftplay.openiptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardLayoutMode { RU, EN, NUM, SYM }

fun keyboardRowsFor(mode: KeyboardLayoutMode): List<List<String>> = when (mode) {
    KeyboardLayoutMode.RU -> listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
        listOf("⇧", "я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "⌫")
    )
    KeyboardLayoutMode.EN -> listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
    )
    KeyboardLayoutMode.NUM -> listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "_", "-", ".", "+", "/", ":", "?", "&", "="),
        listOf("!", "#", "$", "%", "*", "(", ")", "'", "\"", "⌫")
    )
    KeyboardLayoutMode.SYM -> listOf(
        listOf("~", "`", "|", "^", "[", "]", "{", "}", "<", ">"),
        listOf("\\", ";", ",", "€", "£", "₽", "§", "°", "•", "…"),
        listOf("_", "-", "+", "=", "/", "@", ".", ":", "?", "⌫")
    )
}

fun preferredLayoutForInputType(inputType: String, placeholder: String = ""): KeyboardLayoutMode =
    when (inputType.lowercase()) {
        "tel", "number" -> KeyboardLayoutMode.NUM
        "email", "password", "url" -> KeyboardLayoutMode.EN
        else -> {
            val hint = placeholder.lowercase()
            when {
                hint.contains("email") || hint.contains("e-mail") || hint.contains("почт") ||
                    hint.contains("login") || hint.contains("логин") ||
                    hint.contains("password") || hint.contains("парол") -> KeyboardLayoutMode.EN
                hint.contains("телефон") || hint.contains("phone") -> KeyboardLayoutMode.NUM
                else -> KeyboardLayoutMode.RU
            }
        }
    }

fun toggleSymbolMode(current: KeyboardLayoutMode): KeyboardLayoutMode = when (current) {
    KeyboardLayoutMode.RU, KeyboardLayoutMode.EN -> KeyboardLayoutMode.NUM
    KeyboardLayoutMode.NUM -> KeyboardLayoutMode.SYM
    KeyboardLayoutMode.SYM -> KeyboardLayoutMode.NUM
}

fun toggleLanguageOrLetters(current: KeyboardLayoutMode, lastLetter: KeyboardLayoutMode): KeyboardLayoutMode =
    when (current) {
        KeyboardLayoutMode.RU -> KeyboardLayoutMode.EN
        KeyboardLayoutMode.EN -> KeyboardLayoutMode.RU
        KeyboardLayoutMode.NUM, KeyboardLayoutMode.SYM ->
            if (lastLetter == KeyboardLayoutMode.EN) KeyboardLayoutMode.EN else KeyboardLayoutMode.RU
    }

@Composable
fun CarOnScreenKeyboardDialog(
    initialText: String = "",
    initialLayout: KeyboardLayoutMode = KeyboardLayoutMode.RU,
    isPassword: Boolean = false,
    confirmLabel: String = "Найти",
    isSearchAction: Boolean = true,
    placeholder: String = "Введите текст...",
    onTextChange: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    var inputText by remember(initialText) { mutableStateOf(initialText) }

    LaunchedEffect(Unit) {
        com.kraftplay.openiptv.util.hideSystemSoftKeyboard(hostView)
        kotlinx.coroutines.delay(150)
        com.kraftplay.openiptv.util.hideSystemSoftKeyboard(hostView)
    }
    var isCaps by remember { mutableStateOf(false) }
    var layoutMode by remember(initialLayout) { mutableStateOf(initialLayout) }
    var lastLetterLayout by remember(initialLayout) {
        mutableStateOf(if (initialLayout == KeyboardLayoutMode.EN) KeyboardLayoutMode.EN else KeyboardLayoutMode.RU)
    }
    var revealPassword by remember(isPassword) { mutableStateOf(false) }

    fun updateInput(next: String) {
        inputText = next
        onTextChange?.invoke(next)
    }

    val previewText = when {
        inputText.isEmpty() -> placeholder
        isPassword && !revealPassword -> "•".repeat(inputText.length)
        else -> inputText
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0xF2181820),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок и текстовое поле
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2B2B36))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewText,
                            color = if (inputText.isEmpty()) Color.Gray else Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isPassword && inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { revealPassword = !revealPassword },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (revealPassword) "Скрыть пароль" else "Показать пароль",
                                    tint = Color.LightGray
                                )
                            }
                        }
                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { updateInput("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    }

                    if (!isPassword) {
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                com.kraftplay.openiptv.util.VoiceInputHelper.startSpeechRecognition(
                                    context = context,
                                    onResult = { spoken -> updateInput(spoken) }
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00A2E8))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопки клавиатуры
                val rows = keyboardRowsFor(layoutMode)

                for (row in rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (key in row) {
                            val isSpecial = key in listOf("⇧", "⌫")
                            val keyWeight = if (isSpecial) 1.4f else 1.0f
                            val isLetterMode = layoutMode == KeyboardLayoutMode.RU || layoutMode == KeyboardLayoutMode.EN
                            val keyText = if (isCaps && isLetterMode && !isSpecial) key.uppercase() else key

                            Box(
                                modifier = Modifier
                                    .weight(keyWeight)
                                    .height(42.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSpecial) Color(0xFF3B3B4A) else Color(0xFF2B2B36))
                                    .clickable {
                                        when (key) {
                                            "⇧" -> isCaps = !isCaps
                                            "⌫" -> if (inputText.isNotEmpty()) updateInput(inputText.dropLast(1))
                                            else -> {
                                                updateInput(inputText + keyText)
                                                if (isCaps) isCaps = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "⇧" -> Icon(
                                        Icons.Default.KeyboardCapslock,
                                        contentDescription = "Caps",
                                        tint = if (isCaps) Color(0xFFFF9800) else Color.White
                                    )
                                    "⌫" -> Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                    else -> Text(
                                        text = keyText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Нижний ряд клавиш (Символы, Язык, Пробел, Подтверждение)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка цифр и символов
                    Box(
                        modifier = Modifier
                            .weight(1.05f)
                            .height(44.dp)
                            .padding(end = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B3B4A))
                            .clickable {
                                layoutMode = toggleSymbolMode(layoutMode)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (layoutMode) {
                                KeyboardLayoutMode.RU, KeyboardLayoutMode.EN -> "123 / !@#"
                                KeyboardLayoutMode.NUM -> "#+="
                                KeyboardLayoutMode.SYM -> "123"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    // Кнопка переключения языка / возврата к буквам
                    Box(
                        modifier = Modifier
                            .weight(0.95f)
                            .height(44.dp)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B3B4A))
                            .clickable {
                                val next = toggleLanguageOrLetters(layoutMode, lastLetterLayout)
                                layoutMode = next
                                if (next == KeyboardLayoutMode.RU || next == KeyboardLayoutMode.EN) {
                                    lastLetterLayout = next
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (layoutMode) {
                                KeyboardLayoutMode.RU -> "EN"
                                KeyboardLayoutMode.EN -> "RU"
                                KeyboardLayoutMode.NUM, KeyboardLayoutMode.SYM -> "ABC"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    // Клавиша Пробел
                    Box(
                        modifier = Modifier
                            .weight(2.0f)
                            .height(44.dp)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2B2B36))
                            .clickable { updateInput(inputText + " ") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Пробел", color = Color.Gray, fontSize = 14.sp)
                    }

                    // Кнопка подтверждения (Найти / Готово)
                    Button(
                        onClick = {
                            onConfirm(inputText)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.45f)
                            .height(44.dp)
                            .padding(start = 3.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isSearchAction) Icons.Default.Search else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(confirmLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun WebKeyboardModalHost(
    keyboardMode: com.kraftplay.openiptv.util.WebKeyboardMode,
    focusedInput: com.kraftplay.openiptv.util.FocusedWebInput,
    searchInitialText: String = "",
    searchPlaceholder: String = "Поиск...",
    searchConfirmLabel: String = "Найти",
    searchInitialLayout: KeyboardLayoutMode = KeyboardLayoutMode.RU,
    activeWebViewProvider: () -> android.webkit.WebView?,
    onDismiss: () -> Unit,
    onSubmitSearchOrUrl: (String) -> Unit
) {
    if (keyboardMode == com.kraftplay.openiptv.util.WebKeyboardMode.WEB_FIELD) {
        var lastDispatchedText by remember(focusedInput) { mutableStateOf(focusedInput.value) }
        CarOnScreenKeyboardDialog(
            initialText = focusedInput.value,
            initialLayout = preferredLayoutForInputType(focusedInput.inputType, focusedInput.placeholder),
            isPassword = focusedInput.inputType.equals("password", ignoreCase = true),
            confirmLabel = "Готово",
            isSearchAction = false,
            placeholder = focusedInput.placeholder.ifBlank { "Введите текст..." },
            onTextChange = { next ->
                val target = activeWebViewProvider()
                if (focusedInput.isCrossOriginIframe) {
                    com.kraftplay.openiptv.util.dispatchDeltaToWebView(target, lastDispatchedText, next)
                } else {
                    target?.evaluateJavascript(
                        com.kraftplay.openiptv.util.buildSetInputValueScript(next, triggerChange = false),
                        null
                    )
                }
                lastDispatchedText = next
            },
            onDismiss = {
                activeWebViewProvider()?.evaluateJavascript(
                    com.kraftplay.openiptv.util.buildResetInputNotifyScript(),
                    null
                )
                onDismiss()
            },
            onConfirm = { finalText ->
                val target = activeWebViewProvider()
                if (focusedInput.isCrossOriginIframe) {
                    com.kraftplay.openiptv.util.dispatchDeltaToWebView(target, lastDispatchedText, finalText)
                } else {
                    target?.evaluateJavascript(
                        com.kraftplay.openiptv.util.buildSetInputValueScript(finalText, triggerChange = true),
                        null
                    )
                }
                lastDispatchedText = finalText
            }
        )
    } else {
        CarOnScreenKeyboardDialog(
            initialText = searchInitialText,
            initialLayout = searchInitialLayout,
            isPassword = false,
            confirmLabel = searchConfirmLabel,
            isSearchAction = true,
            placeholder = searchPlaceholder,
            onDismiss = {
                activeWebViewProvider()?.evaluateJavascript(
                    com.kraftplay.openiptv.util.buildResetInputNotifyScript(),
                    null
                )
                onDismiss()
            },
            onConfirm = onSubmitSearchOrUrl
        )
    }
}

@Composable
fun AuthPopupWebViewOverlay(
    popupWebView: android.webkit.WebView,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Surface(
            color = Color(0xFF202028),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Авторизация",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть авторизацию",
                        tint = Color.White
                    )
                }
            }
        }
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { popupWebView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
