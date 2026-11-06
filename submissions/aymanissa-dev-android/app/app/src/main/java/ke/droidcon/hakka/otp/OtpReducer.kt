package ke.droidcon.hakka.otp

object OtpReducer {
    fun initial(): OtpState = OtpState()

    fun applyInput(state: OtpState, previous: String, incoming: String): OtpState {
        if (state.status == OtpStatus.Success) return state
        if (incoming.length < previous.length) {
            val digits = incoming.filter(::isAsciiDigit)
            return if (state.code.isNotEmpty() && digits == state.code.dropLast(1)) {
                backspace(state)
            } else {
                withCode(state, digits)
            }
        }
        val added = if (previous.isNotEmpty() && incoming.startsWith(previous)) {
            incoming.substring(previous.length)
        } else {
            incoming
        }
        val addedDigits = added.filter(::isAsciiDigit)
        if (addedDigits.isEmpty()) return state
        return if (added.length == 1) {
            withCode(state, state.code + addedDigits)
        } else {
            withCode(state, addedDigits)
        }
    }

    fun backspace(state: OtpState): OtpState {
        if (state.status == OtpStatus.Success || state.code.isEmpty()) return state
        return withCode(state, state.code.dropLast(1))
    }

    fun submit(state: OtpState): OtpState {
        if (state.status == OtpStatus.Success || state.code.length != OTP_LENGTH) return state
        return if (state.code == SUCCESS_CODE) {
            state.copy(status = OtpStatus.Success)
        } else {
            state.copy(code = "", status = OtpStatus.Error)
        }
    }

    fun resend(state: OtpState): OtpState {
        if (state.resendSecondsRemaining > 0) return state
        return state.copy(resendSecondsRemaining = RESEND_SECONDS)
    }

    fun tick(state: OtpState): OtpState {
        if (state.resendSecondsRemaining <= 0) return state
        return state.copy(resendSecondsRemaining = state.resendSecondsRemaining - 1)
    }

    private fun withCode(state: OtpState, rawDigits: String): OtpState {
        val next = rawDigits.filter(::isAsciiDigit).take(OTP_LENGTH)
        if (next == state.code && state.status != OtpStatus.Error) return state
        val status = if (state.status == OtpStatus.Error) OtpStatus.Editing else state.status
        return state.copy(code = next, status = status)
    }

    private fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'
}
