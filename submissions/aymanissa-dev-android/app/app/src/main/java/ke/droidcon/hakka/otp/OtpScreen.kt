package ke.droidcon.hakka.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(palette: OtpPalette, dark: Boolean) {
    var state by remember { mutableStateOf(OtpReducer.initial()) }

    LaunchedEffect(state.resendSecondsRemaining) {
        if (state.resendSecondsRemaining == 0) return@LaunchedEffect
        delay(1_000)
        state = OtpReducer.tick(state)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim.asColor())
            .semantics { testTagsAsResourceId = true },
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Spacer(modifier = Modifier.height(72.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(palette.sheet.asColor())
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.thamani_logo_white else R.drawable.thamani_logo,
                    ),
                    contentDescription = "Thamani logo",
                    modifier = Modifier
                        .height(52.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = OtpCopy.TITLE,
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.textPrimary.asColor(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = OtpCopy.SUBTITLE,
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.textSecondary.asColor(),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(28.dp))
                OtpCodeInput(
                    state = state,
                    palette = palette,
                    onStateChange = { state = it },
                )
                Spacer(modifier = Modifier.height(18.dp))
                ResendLine(
                    state = state,
                    palette = palette,
                    onResend = { state = OtpReducer.resend(state) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine(state = state, palette = palette)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { state = OtpReducer.submit(state) },
                    enabled = state.isComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("otp_submit"),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.buttonBg.asColor(),
                        contentColor = palette.buttonText.asColor(),
                        disabledContainerColor = palette.buttonDisabledBg.asColor(),
                        disabledContentColor = palette.textPrimary.asColor(),
                    ),
                ) {
                    Text(
                        text = OtpCopy.SUBMIT,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ResendLine(state: OtpState, palette: OtpPalette, onResend: () -> Unit) {
    val label = OtpCopy.resendLabel(state.resendSecondsRemaining)
    val ready = state.resendSecondsRemaining == 0
    val text = if (ready) {
        val linkAt = label.lastIndexOf("Resend")
        buildAnnotatedString {
            append(label.substring(0, linkAt))
            withStyle(
                SpanStyle(
                    color = palette.link.asColor(),
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(label.substring(linkAt))
            }
        }
    } else {
        buildAnnotatedString { append(label) }
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier
                .testTag("otp_resend")
                .then(if (ready) Modifier.clickable(onClick = onResend) else Modifier),
            color = palette.textSecondary.asColor(),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusLine(state: OtpState, palette: OtpPalette) {
    val (message, color) = when (state.status) {
        OtpStatus.Success -> OtpCopy.SUCCESS to palette.success
        OtpStatus.Error -> OtpCopy.ERROR to palette.error
        OtpStatus.Editing -> "" to palette.textSecondary
    }
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 22.dp)
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = color.asColor(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

private fun Int.asColor(): Color = Color(this)
