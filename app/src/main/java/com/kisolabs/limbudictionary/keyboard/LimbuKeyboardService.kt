package com.kisolabs.limbudictionary.keyboard

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class LimbuKeyboardService : InputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val mLifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val mSavedStateRegistryController = SavedStateRegistryController.create(this)

    private val clipboardHistory = mutableStateListOf<String>()
    private var clipboardManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureClipboardText()
    }

    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        mSavedStateRegistryController.performRestore(null)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
            captureClipboardText()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureClipboardText() {
        try {
            val clipData = clipboardManager?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val text = item?.text?.toString()
                if (!text.isNullOrBlank() && !clipboardHistory.contains(text)) {
                    clipboardHistory.add(0, text)
                    if (clipboardHistory.size > 20) {
                        clipboardHistory.removeAt(clipboardHistory.size - 1)
                    }
                }
            }
        } catch (e: Exception) {
            // Catches potential SecurityExceptions or NullPointerExceptions from ClipboardManager
            e.printStackTrace()
        }
    }

    override fun onCreateInputView(): View {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            isHapticFeedbackEnabled = true

            setViewTreeLifecycleOwner(this@LimbuKeyboardService)
            setViewTreeViewModelStoreOwner(this@LimbuKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@LimbuKeyboardService)

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                LimbuKeyboardScreen(
                    clipboardHistory = clipboardHistory,
                    onInput = { text: String ->
                        try {
                            currentInputConnection?.commitText(text, 1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onBackspace = {
                        val inputConnection = currentInputConnection ?: return@LimbuKeyboardScreen
                        try {
                            val selectedText = inputConnection.getSelectedText(0)
                            if (!selectedText.isNullOrEmpty()) {
                                inputConnection.commitText("", 1)
                            } else {
                                val textBefore = inputConnection.getTextBeforeCursor(2, 0)
                                if (!textBefore.isNullOrEmpty()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        inputConnection.deleteSurroundingTextInCodePoints(1, 0)
                                    } else {
                                        val lastChar = textBefore.last()
                                        val charLength = if (Character.isSurrogate(lastChar)) 2 else 1
                                        inputConnection.deleteSurroundingText(charLength, 0)
                                    }
                                } else {
                                    // Fallback when buffer is empty to avoid Compose range errors
                                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onEnter = {
                        try {
                            currentInputConnection?.sendKeyEvent(
                                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                            )
                            currentInputConnection?.sendKeyEvent(
                                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    getWordAtCursor = {
                        try {
                            val textBefore = currentInputConnection?.getTextBeforeCursor(30, 0)?.toString() ?: ""
                            textBefore.takeLastWhile { !it.isWhitespace() }
                        } catch (e: Exception) {
                            ""
                        }
                    },
                    replaceCurrentWord = { newWord: String, currentLen: Int ->
                        try {
                            val inputConnection = currentInputConnection ?: return@LimbuKeyboardScreen
                            if (currentLen > 0) {
                                inputConnection.deleteSurroundingText(currentLen, 0)
                            }
                            inputConnection.commitText(newWord, 1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onOpenSettings = {
                        try {
                            val intent = Intent(this@LimbuKeyboardService, SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onClearClipboard = {
                        clipboardHistory.clear()
                    }
                )
            }
        }
        return composeView
    }

    override fun onDestroy() {
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
        super.onDestroy()
    }
}
