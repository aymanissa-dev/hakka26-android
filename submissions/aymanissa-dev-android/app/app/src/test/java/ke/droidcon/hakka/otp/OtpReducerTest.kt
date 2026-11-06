package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class OtpReducerTest {
    @Test
    fun typingAdvancesAndRejectsNonDigits() {
        var state = OtpReducer.initial()
        assertEquals(0, state.focusIndex)

        state = type(state, '2')
        state = type(state, '1')
        assertEquals("21", state.code)
        assertEquals(2, state.focusIndex)
        assertEquals('2', state.digitAt(0))
        assertEquals(null, state.digitAt(2))

        val rejected = listOf('a', ' ', '٥', '５', '-', '。')
        rejected.forEach { ch ->
            assertSame(state, OtpReducer.applyInput(state, state.code, state.code + ch))
        }

        repeat(4) { index ->
            state = type(state, ('3'.code + index).toChar())
        }
        assertEquals("213456", state.code)
        assertEquals(5, state.focusIndex)
        assertEquals(state, OtpReducer.applyInput(state, state.code, state.code + "7"))
        assertFalse(state.code.any { it !in '0'..'9' })
    }

    @Test
    fun backspaceOnEmptyBoxClearsPreviousAndStopsOnTheFirstBox() {
        val empty = OtpReducer.initial()
        assertSame(empty, OtpReducer.backspace(empty))
        assertSame(empty, OtpReducer.applyInput(empty, "", ""))

        var state = type(type(OtpReducer.initial(), '2'), '1')
        assertEquals(2, state.focusIndex)
        state = OtpReducer.applyInput(state, "21", "2")
        assertEquals("2", state.code)
        assertEquals(1, state.focusIndex)

        state = OtpReducer.backspace(state)
        assertEquals("", state.code)
        assertEquals(0, state.focusIndex)
        assertSame(state, OtpReducer.backspace(state))
    }

    @Test
    fun pasteExtractsAsciiDigitsAndDropsTheRest() {
        val sentence = OtpReducer.applyInput(
            OtpReducer.initial(),
            previous = "",
            incoming = "Your code is 482913",
        )
        assertEquals("482913", sentence.code)
        assertEquals(5, sentence.focusIndex)

        val overflow = OtpReducer.applyInput(
            OtpState(code = "99"),
            previous = "99",
            incoming = "99Your code is 482913 extra 70",
        )
        assertEquals("482913", overflow.code)

        val replaced = OtpReducer.applyInput(
            OtpState(code = "111111"),
            previous = "111111",
            incoming = "Your code is 482913",
        )
        assertEquals("482913", replaced.code)

        val partial = OtpReducer.applyInput(OtpReducer.initial(), "", "ab12cd3")
        assertEquals("123", partial.code)
        assertEquals(3, partial.focusIndex)

        val unchanged = OtpState(code = "44")
        assertSame(unchanged, OtpReducer.applyInput(unchanged, "44", "no digits here"))
        assertSame(unchanged, OtpReducer.applyInput(unchanged, "44", "٤٨٢٩١٣"))
        assertSame(unchanged, OtpReducer.applyInput(unchanged, "44", "４８２９１３"))

        val cleared = OtpReducer.applyInput(OtpState(code = "482913"), "482913", "")
        assertEquals("", cleared.code)
        assertEquals(0, cleared.focusIndex)
    }

    @Test
    fun submitIsIgnoredUntilCompleteThenSucceedsOrResets() {
        val partial = OtpState(code = "12345")
        assertSame(partial, OtpReducer.submit(partial))
        assertFalse(partial.isComplete)

        val gapped = OtpReducer.applyInput(OtpReducer.initial(), "", "12 9")
        assertEquals("129", gapped.code)
        assertSame(gapped, OtpReducer.submit(gapped))

        val success = OtpReducer.submit(OtpState(code = "123456"))
        assertEquals(OtpStatus.Success, success.status)
        assertEquals(OtpCopy.SUCCESS, message(success))
        assertEquals("123456", success.code)

        val wrong = OtpReducer.submit(OtpState(code = "000000"))
        assertEquals(OtpStatus.Error, wrong.status)
        assertEquals(OtpCopy.ERROR, message(wrong))
        assertEquals("", wrong.code)
        assertEquals(0, wrong.focusIndex)

        val editingAgain = type(wrong, '8')
        assertEquals(OtpStatus.Editing, editingAgain.status)
        assertEquals("8", editingAgain.code)
        assertEquals("", message(editingAgain))
    }

    @Test
    fun countdownBlocksResendUntilZeroThenRestarts() {
        var state = OtpReducer.initial()
        assertEquals(30, state.resendSecondsRemaining)
        assertEquals("Resend in 0:30", OtpCopy.resendLabel(state.resendSecondsRemaining))
        assertSame(state, OtpReducer.resend(state))

        state = OtpReducer.tick(state)
        assertEquals(29, state.resendSecondsRemaining)
        assertEquals("Resend in 0:09", OtpCopy.resendLabel(9))
        assertSame(state, OtpReducer.resend(state))

        repeat(29) { state = OtpReducer.tick(state) }
        assertEquals(0, state.resendSecondsRemaining)
        assertEquals(OtpCopy.RESEND_READY, OtpCopy.resendLabel(state.resendSecondsRemaining))
        assertSame(state, OtpReducer.tick(state))

        state = OtpReducer.resend(state)
        assertEquals(30, state.resendSecondsRemaining)
        assertSame(state, OtpReducer.resend(state))
    }

    @Test
    fun successIgnoresFurtherEdits() {
        val success = OtpReducer.submit(OtpState(code = SUCCESS_CODE))
        assertSame(success, OtpReducer.applyInput(success, success.code, "000000"))
        assertSame(success, OtpReducer.backspace(success))
        assertSame(success, OtpReducer.submit(success))
        assertEquals(SUCCESS_CODE, success.code)
    }

    @Test
    fun copyAndAnnouncementsMatchTheChallenge() {
        assertEquals("Enter your code", OtpCopy.TITLE)
        assertEquals("A 6-digit code has been sent to your phone.", OtpCopy.SUBTITLE)
        assertEquals("You're in.", OtpCopy.SUCCESS)
        assertEquals("That code didn't work. Try again.", OtpCopy.ERROR)
        assertEquals("Didn't receive the code? Resend", OtpCopy.RESEND_READY)
        assertEquals("Submit", OtpCopy.SUBMIT)
        assertFalse(OtpCopy.PLACEHOLDER.any { it in '0'..'9' })
        repeat(OTP_LENGTH) { index ->
            assertEquals("Digit ${index + 1} of 6", OtpCopy.digitAnnouncement(index + 1))
        }
    }

    private fun type(state: OtpState, digit: Char): OtpState =
        OtpReducer.applyInput(state, state.code, state.code + digit)

    private fun message(state: OtpState): String = when (state.status) {
        OtpStatus.Success -> OtpCopy.SUCCESS
        OtpStatus.Error -> OtpCopy.ERROR
        OtpStatus.Editing -> ""
    }
}
