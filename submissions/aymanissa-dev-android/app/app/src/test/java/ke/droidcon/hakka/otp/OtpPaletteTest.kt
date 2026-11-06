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
        assertEquals(0xFF2E7D45.toInt(), dark.digitFilledBg)
        assertEquals(0xFF161A17.toInt(), dark.sheet)
        assertEquals(0xCC000000.toInt(), dark.scrim)
        assertNotEquals(light.sheet, dark.sheet)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertNotEquals(light.buttonBg, dark.buttonBg)
        assertEquals(light, parseOtpPalette(json, dark = false))
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
