package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun digitlessPasteLeavesTheCodeUntouched() {
        val empty = OtpReducer.initial()
        assertSame(empty, OtpReducer.applyInput(empty, "", "   "))
        assertSame(empty, OtpReducer.applyInput(empty, "", "\n\t "))

        val partial = OtpState(code = "44")
        assertSame(partial, OtpReducer.applyInput(partial, "44", "   "))
        assertSame(partial, OtpReducer.applyInput(partial, "44", "\r\n\t"))

        val full = OtpState(code = "123456")
        assertSame(full, OtpReducer.applyInput(full, "123456", "      "))
        assertSame(full, OtpReducer.applyInput(full, "123456", "\n\n\n\n\n\n\n"))
    }

    @Test
    fun pasteFromMidCodeOrAFullCodeTakesTheFirstSixDigits() {
        val overflow = OtpReducer.applyInput(OtpReducer.initial(), "", "1234 5678 90")
        assertEquals("123456", overflow.code)
        assertEquals(5, overflow.focusIndex)
        assertTrue(overflow.isComplete)

        val mid = OtpReducer.applyInput(OtpState(code = "12"), "12", "12987654321")
        assertEquals("987654", mid.code)
        assertEquals(5, mid.focusIndex)

        val shortMid = OtpReducer.applyInput(OtpState(code = "12"), "12", "12-98")
        assertEquals("98", shortMid.code)
        assertEquals(2, shortMid.focusIndex)
        assertFalse(shortMid.isComplete)

        val grown = OtpReducer.applyInput(
            OtpState(code = "555555"),
            previous = "555555",
            incoming = "555555123456789",
        )
        assertEquals("123456", grown.code)
        assertEquals(5, grown.focusIndex)

        val replaced = OtpReducer.applyInput(
            OtpState(code = "555555"),
            previous = "555555",
            incoming = "999888777666",
        )
        assertEquals("999888", replaced.code)
        assertTrue(replaced.isComplete)
    }

    @Test
    fun mixedUnicodeDigitsDoNotCountAsInput() {
        val mixed = OtpReducer.applyInput(
            OtpReducer.initial(),
            previous = "",
            incoming = "12\uFF15\uFF167\uFF189",
        )
        assertEquals("1279", mixed.code)
        assertEquals(4, mixed.focusIndex)

        val devanagari = OtpReducer.applyInput(
            OtpState(code = "44"),
            previous = "44",
            incoming = "\u096A\u096A8\u096829\u09673",
        )
        assertEquals("8293", devanagari.code)
        assertEquals(4, devanagari.focusIndex)
    }

    @Test
    fun typedZeroIsADigitWhileEmptyBoxesStayBlank() {
        val empty = OtpReducer.initial()
        assertEquals(null, empty.digitAt(0))
        assertEquals("\u2013", OtpCopy.PLACEHOLDER)

        val zero = type(empty, '0')
        assertEquals("0", zero.code)
        assertEquals('0', zero.digitAt(0))
        assertEquals(null, zero.digitAt(1))
        assertEquals(1, zero.focusIndex)
        assertNotEquals(OtpCopy.PLACEHOLDER, zero.digitAt(0).toString())
        assertFalse(zero.isComplete)

        val pasted = OtpReducer.applyInput(empty, "", "Your code is 012345")
        assertEquals("012345", pasted.code)
        assertEquals('0', pasted.digitAt(0))
        assertEquals('5', pasted.digitAt(5))
        assertEquals(5, pasted.focusIndex)
        assertTrue(pasted.isComplete)
    }

    @Test
    fun clearingOneBoxDisablesSubmitUntilItIsFilledAgain() {
        var state = OtpState(code = "123456", resendSecondsRemaining = 21)
        assertTrue(state.isComplete)

        state = OtpReducer.applyInput(state, "123456", "12345")
        assertEquals("12345", state.code)
        assertEquals('5', state.digitAt(4))
        assertEquals(null, state.digitAt(5))
        assertEquals(5, state.focusIndex)
        assertFalse(state.isComplete)
        assertEquals(OtpStatus.Editing, state.status)
        assertEquals(21, state.resendSecondsRemaining)
        assertSame(state, OtpReducer.submit(state))

        state = type(state, '9')
        assertEquals("123459", state.code)
        assertEquals('1', state.digitAt(0))
        assertEquals('9', state.digitAt(5))
        assertTrue(state.isComplete)

        val rejected = OtpReducer.submit(state)
        assertEquals(OtpStatus.Error, rejected.status)
        assertEquals("", rejected.code)
        assertFalse(rejected.isComplete)
    }

    @Test
    fun replacingADigitInAFilledBoxKeepsTheOthers() {
        val full = OtpState(code = "123456")
        val middle = OtpReducer.applyInput(full, previous = "123456", incoming = "193456")
        assertEquals("193456", middle.code)
        assertEquals(5, middle.focusIndex)
        assertTrue(middle.isComplete)
        assertEquals(OtpStatus.Editing, middle.status)

        val last = OtpReducer.applyInput(full, "123456", "123450")
        assertEquals("123450", last.code)
        assertEquals('0', last.digitAt(5))
        assertEquals('1', last.digitAt(0))
        assertNotEquals(OtpCopy.PLACEHOLDER, last.digitAt(5).toString())

        val partial = OtpState(code = "1234")
        val fourth = OtpReducer.applyInput(partial, "1234", "1230")
        assertEquals("1230", fourth.code)
        assertEquals('0', fourth.digitAt(3))
        assertEquals(4, fourth.focusIndex)
        assertEquals(null, fourth.digitAt(4))
        assertFalse(fourth.isComplete)
    }

    @Test
    fun inputAfterAnErrorReturnsToEditingUntilSubmit() {
        val error = OtpReducer.submit(OtpState(code = "000000"))
        assertSame(error, OtpReducer.applyInput(error, "", "   "))
        assertSame(error, OtpReducer.applyInput(error, "", "abc\uFF15"))
        assertSame(error, OtpReducer.backspace(error))

        val pastedSuccess = OtpReducer.applyInput(error, "", "Your code is 123456")
        assertEquals("123456", pastedSuccess.code)
        assertEquals(OtpStatus.Editing, pastedSuccess.status)
        assertEquals("", message(pastedSuccess))
        assertTrue(pastedSuccess.isComplete)
        val success = OtpReducer.submit(pastedSuccess)
        assertEquals(OtpStatus.Success, success.status)
        assertEquals(OtpCopy.SUCCESS, message(success))
        assertEquals("123456", success.code)

        val pastedFailure = OtpReducer.applyInput(error, "", "Your code is 654321 extra")
        assertEquals("654321", pastedFailure.code)
        assertEquals(OtpStatus.Editing, pastedFailure.status)
        assertEquals("", message(pastedFailure))
        val failedAgain = OtpReducer.submit(pastedFailure)
        assertEquals(OtpStatus.Error, failedAgain.status)
        assertEquals("", failedAgain.code)
        assertEquals(0, failedAgain.focusIndex)
        assertEquals(OtpCopy.ERROR, message(failedAgain))

        var typed = error
        "129".forEach { typed = type(typed, it) }
        assertEquals(OtpStatus.Editing, typed.status)
        assertEquals("129", typed.code)
        assertEquals("", message(typed))
        typed = OtpReducer.backspace(typed)
        assertEquals("12", typed.code)
        assertEquals(OtpStatus.Editing, typed.status)
    }

    @Test
    fun resendKeepsEnteredDigitsAndLabelsEverySecond() {
        var state = OtpReducer.initial()
        "48291".forEach { state = type(state, it) }
        assertEquals("48291", state.code)
        assertEquals(OtpStatus.Editing, state.status)

        for (remaining in RESEND_SECONDS downTo 1) {
            assertEquals(remaining, state.resendSecondsRemaining)
            assertEquals(
                "Resend in 0:%02d".format(remaining),
                OtpCopy.resendLabel(state.resendSecondsRemaining),
            )
            assertSame(state, OtpReducer.resend(state))
            assertEquals("48291", state.code)
            state = OtpReducer.tick(state)
        }

        assertEquals(0, state.resendSecondsRemaining)
        assertEquals("48291", state.code)
        assertEquals(OtpCopy.RESEND_READY, OtpCopy.resendLabel(state.resendSecondsRemaining))
        assertSame(state, OtpReducer.tick(state))

        state = OtpReducer.resend(state)
        assertEquals(RESEND_SECONDS, state.resendSecondsRemaining)
        assertEquals("48291", state.code)
        assertEquals(OtpStatus.Editing, state.status)
        assertEquals("Resend in 0:30", OtpCopy.resendLabel(state.resendSecondsRemaining))
        assertSame(state, OtpReducer.resend(state))

        val succeeded = OtpReducer.submit(
            OtpState(code = SUCCESS_CODE, resendSecondsRemaining = 0),
        )
        val resent = OtpReducer.resend(succeeded)
        assertEquals(SUCCESS_CODE, resent.code)
        assertEquals(OtpStatus.Success, resent.status)
        assertEquals(RESEND_SECONDS, resent.resendSecondsRemaining)
    }

    @Test
    fun focusIgnoresAnOutOfRangeIndex() {
        val empty = OtpReducer.initial()
        val partial = OtpState(code = "123")
        val full = OtpState(code = SUCCESS_CODE)
        listOf(empty, partial, full).forEach { state ->
            assertEquals(null, state.digitAt(-1))
            assertEquals(null, state.digitAt(OTP_LENGTH))
            assertEquals(null, state.digitAt(OTP_LENGTH + 3))
        }
        assertEquals(0, empty.focusIndex)
        assertEquals(3, partial.focusIndex)
        assertEquals('1', partial.digitAt(0))
        assertEquals(null, partial.digitAt(3))
        assertEquals(OTP_LENGTH - 1, full.focusIndex)
        assertEquals('6', full.digitAt(OTP_LENGTH - 1))
        assertEquals(OTP_LENGTH - 1, OtpState(code = "123456789").focusIndex)
    }

    @Test
    fun otherWrongCodesResetWithoutTouchingTheCountdown() {
        listOf("000001", "012345", "111111", "123455", "999999").forEach { code ->
            val result = OtpReducer.submit(OtpState(code = code, resendSecondsRemaining = 14))
            assertEquals(code, OtpStatus.Error, result.status)
            assertEquals(code, "", result.code)
            assertEquals(code, 0, result.focusIndex)
            assertEquals(code, OtpCopy.ERROR, message(result))
            assertFalse(code, result.isComplete)
            assertEquals(code, 14, result.resendSecondsRemaining)
            assertSame(result, OtpReducer.submit(result))
        }

        val success = OtpReducer.submit(OtpState(code = "123456", resendSecondsRemaining = 14))
        assertEquals(OtpStatus.Success, success.status)
        assertEquals("123456", success.code)
        assertEquals(OtpCopy.SUCCESS, message(success))
        assertEquals(14, success.resendSecondsRemaining)
        assertTrue(success.isComplete)
    }

    private fun type(state: OtpState, digit: Char): OtpState =
        OtpReducer.applyInput(state, state.code, state.code + digit)

    private fun message(state: OtpState): String = when (state.status) {
        OtpStatus.Success -> OtpCopy.SUCCESS
        OtpStatus.Error -> OtpCopy.ERROR
        OtpStatus.Editing -> ""
    }
}
