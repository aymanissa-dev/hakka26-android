package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

class OtpPaletteTest {
    @Test
    fun lightAndDarkComeFromColoursJson() {
        val json = coloursJson()
        val light = parseOtpPalette(json, dark = false)
        val dark = parseOtpPalette(json, dark = true)

        assertEquals(0xFF12361B.toInt(), light.digitFilledBg)
        assertEquals(0xFFFFFFFF.toInt(), light.digitFilledText)
        assertEquals(0xFFFFFFFF.toInt(), light.digitFocusedBg)
        assertEquals(0xFF111111.toInt(), light.digitFocusedOutline)
        assertEquals(0xFFF1F1F1.toInt(), light.digitEmptyBg)
        assertEquals(0xFF9E9E9E.toInt(), light.digitPlaceholder)
        assertEquals(0x99000000.toInt(), light.scrim)
        assertEquals(0xFFFFFFFF.toInt(), light.sheet)
        assertEquals(0xFF111111.toInt(), light.textPrimary)
        assertEquals(0xFF5F6368.toInt(), light.textSecondary)
        assertEquals(0xFFE0E0E0.toInt(), light.digitEmptyOutline)
        assertEquals(0xFF12361B.toInt(), light.buttonBg)
        assertEquals(0xFFFFFFFF.toInt(), light.buttonText)
        assertEquals(0xFFC9D1CB.toInt(), light.buttonDisabledBg)
        assertEquals(0xFF12361B.toInt(), light.link)
        assertEquals(0xFF1B7F3A.toInt(), light.success)
        assertEquals(0xFFC62828.toInt(), light.error)
        assertEquals(0xFF2E7D45.toInt(), dark.digitFilledBg)
        assertEquals(0xFFFFFFFF.toInt(), dark.digitFilledText)
        assertEquals(0xFF161A17.toInt(), dark.sheet)
        assertEquals(0xFF161A17.toInt(), dark.digitFocusedBg)
        assertEquals(0xFFF5F5F5.toInt(), dark.digitFocusedOutline)
        assertEquals(0xFF262B27.toInt(), dark.digitEmptyBg)
        assertEquals(0xFF353B36.toInt(), dark.digitEmptyOutline)
        assertEquals(0xFF6F7570.toInt(), dark.digitPlaceholder)
        assertEquals(0xCC000000.toInt(), dark.scrim)
        assertEquals(0xFFF5F5F5.toInt(), dark.textPrimary)
        assertEquals(0xFFB0B5B1.toInt(), dark.textSecondary)
        assertEquals(0xFF2E7D45.toInt(), dark.buttonBg)
        assertEquals(0xFFFFFFFF.toInt(), dark.buttonText)
        assertEquals(0xFF2A302B.toInt(), dark.buttonDisabledBg)
        assertEquals(0xFF7BC795.toInt(), dark.link)
        assertEquals(0xFF7BC795.toInt(), dark.success)
        assertEquals(0xFFEF9A9A.toInt(), dark.error)
        assertNotEquals(light, dark)
        assertNotEquals(light.sheet, dark.sheet)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertNotEquals(light.buttonBg, dark.buttonBg)
        assertNotEquals(light.success, light.error)
        assertNotEquals(dark.success, dark.error)
        assertEquals(light, parseOtpPalette(json, dark = false))
        assertEquals(dark, parseOtpPalette(json, dark = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingColourFails() {
        parseOtpPalette("""{"light":{"sheet":"#FFFFFF"}}""", dark = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun badHexFails() {
        parseHexColor("#12GGGG")
    }

    private fun coloursJson(): String {
        val candidates = listOf(
            File("src/main/assets/colours.json"),
            File("app/src/main/assets/colours.json"),
        )
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("colours.json not found from ${File(".").absoluteFile}")
    }
}
