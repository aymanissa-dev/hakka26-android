package ke.droidcon.hakka.otp

enum class OtpStatus {
    Editing,
    Success,
    Error,
}

data class OtpState(
    val code: String = "",
    val status: OtpStatus = OtpStatus.Editing,
    val resendSecondsRemaining: Int = RESEND_SECONDS,
) {
    val focusIndex: Int
        get() = if (code.length >= OTP_LENGTH) OTP_LENGTH - 1 else code.length

    val isComplete: Boolean
        get() = code.length == OTP_LENGTH

    fun digitAt(index: Int): Char? = code.getOrNull(index)
}

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30
const val SUCCESS_CODE = "123456"

object OtpCopy {
    const val TITLE = "Enter your code"
    const val SUBTITLE = "A 6-digit code has been sent to your phone."
    const val RESEND_READY = "Didn't receive the code? Resend"
    const val SUCCESS = "You're in."
    const val ERROR = "That code didn't work. Try again."
    const val SUBMIT = "Submit"
    const val PLACEHOLDER = "–"

    fun resendLabel(secondsRemaining: Int): String =
        if (secondsRemaining > 0) {
            "Resend in 0:%02d".format(secondsRemaining)
        } else {
            RESEND_READY
        }

    fun digitAnnouncement(position: Int): String = "Digit $position of 6"
}
