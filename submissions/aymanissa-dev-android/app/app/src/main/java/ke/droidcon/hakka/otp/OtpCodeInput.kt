package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpCodeInput(
    state: OtpState,
    palette: OtpPalette,
    onStateChange: (OtpState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.status) {
        if (state.status == OtpStatus.Success) return@LaunchedEffect
        runCatching {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        DigitRow(state = state, palette = palette)
        HiddenOtpField(
            code = state.code,
            focusRequester = focusRequester,
            onValueChange = { incoming ->
                onStateChange(
                    OtpReducer.applyInput(state, previous = state.code, incoming = incoming),
                )
            },
            onEmptyBackspace = { onStateChange(OtpReducer.backspace(state)) },
        )
    }
}

@Composable
private fun HiddenOtpField(
    code: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onEmptyBackspace: () -> Unit,
) {
    BasicTextField(
        value = code,
        onValueChange = onValueChange,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    code.isEmpty()
                ) {
                    onEmptyBackspace()
                    true
                } else {
                    false
                }
            }
            .size(1.dp)
            .alpha(0f)
            .semantics { hideFromAccessibility() },
        textStyle = TextStyle(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
    )
}

@Composable
private fun DigitRow(state: OtpState, palette: OtpPalette) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val circle = (maxWidth - gap * (OTP_LENGTH - 1)) / OTP_LENGTH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(OTP_LENGTH) { index ->
                DigitBox(
                    index = index,
                    digit = state.digitAt(index),
                    focused = index == state.focusIndex,
                    palette = palette,
                    modifier = Modifier.size(circle),
                )
            }
        }
    }
}

@Composable
private fun DigitBox(
    index: Int,
    digit: Char?,
    focused: Boolean,
    palette: OtpPalette,
    modifier: Modifier = Modifier,
) {
    val filled = digit != null && !focused
    val background = when {
        filled -> palette.digitFilledBg
        focused -> palette.digitFocusedBg
        else -> palette.digitEmptyBg
    }
    val borderColor = when {
        focused -> palette.digitFocusedOutline
        digit == null -> palette.digitEmptyOutline
        else -> background
    }
    val borderWidth = when {
        focused -> 2.dp
        digit == null -> 1.dp
        else -> 0.dp
    }
    val glyph = digit?.toString() ?: OtpCopy.PLACEHOLDER
    val glyphColor = when {
        filled -> palette.digitFilledText
        digit != null -> palette.textPrimary
        else -> palette.digitPlaceholder
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background.asColor())
            .border(borderWidth, borderColor.asColor(), CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = OtpCopy.digitAnnouncement(index + 1)
            }
            .testTag("otp_digit_${index + 1}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = glyphColor.asColor(),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun Int.asColor(): Color = Color(this)
