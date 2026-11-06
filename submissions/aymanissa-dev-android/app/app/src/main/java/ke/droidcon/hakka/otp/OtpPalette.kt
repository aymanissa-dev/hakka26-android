package ke.droidcon.hakka.otp

data class OtpPalette(
    val sheet: Int,
    val scrim: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val digitFilledBg: Int,
    val digitFilledText: Int,
    val digitFocusedBg: Int,
    val digitFocusedOutline: Int,
    val digitEmptyBg: Int,
    val digitEmptyOutline: Int,
    val digitPlaceholder: Int,
    val buttonBg: Int,
    val buttonText: Int,
    val buttonDisabledBg: Int,
    val link: Int,
    val success: Int,
    val error: Int,
)

fun parseOtpPalette(json: String, dark: Boolean): OtpPalette {
    val colors = extractModeColors(json, if (dark) "dark" else "light")
    fun req(key: String): Int {
        val raw = colors[key]
        require(!raw.isNullOrBlank()) { "Missing colour $key" }
        return parseHexColor(raw)
    }
    return OtpPalette(
        sheet = req("sheet"),
        scrim = req("scrim"),
        textPrimary = req("text_primary"),
        textSecondary = req("text_secondary"),
        digitFilledBg = req("digit_filled_bg"),
        digitFilledText = req("digit_filled_text"),
        digitFocusedBg = req("digit_focused_bg"),
        digitFocusedOutline = req("digit_focused_outline"),
        digitEmptyBg = req("digit_empty_bg"),
        digitEmptyOutline = req("digit_empty_outline"),
        digitPlaceholder = req("digit_placeholder"),
        buttonBg = req("button_bg"),
        buttonText = req("button_text"),
        buttonDisabledBg = req("button_disabled_bg"),
        link = req("link"),
        success = req("success"),
        error = req("error"),
    )
}

internal fun parseHexColor(raw: String): Int {
    val hex = raw.trim().removePrefix("#")
    require(hex.length == 6 || hex.length == 8) { "Unsupported colour $raw" }
    require(hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) { "Unsupported colour $raw" }
    val argb = if (hex.length == 6) "FF$hex" else hex
    return argb.toULong(16).toInt()
}

private fun extractModeColors(json: String, mode: String): Map<String, String> {
    val keyAt = json.indexOf("\"$mode\"")
    require(keyAt >= 0) { "Missing $mode colours" }
    val start = json.indexOf('{', keyAt)
    require(start >= 0) { "Missing $mode colours" }
    var depth = 0
    var end = -1
    for (index in start until json.length) {
        when (json[index]) {
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth == 0) {
                    end = index
                    break
                }
            }
        }
    }
    require(end > start) { "Missing $mode colours" }
    val body = json.substring(start + 1, end)
    val pairs = Regex("\"([a-z0-9_]+)\"\\s*:\\s*\"(#[0-9A-Fa-f]+)\"").findAll(body)
    return pairs.associate { it.groupValues[1] to it.groupValues[2] }
}
