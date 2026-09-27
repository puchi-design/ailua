package com.example.data.ai.prompt

/**
 * MacroResolver — deterministic template-variable substitution (spec §14).
 *
 * Only four macros are supported: `{{char}}`, `{{user}}`, `{{time}}`, `{{date}}`.
 * Unknown macros (e.g. `{{@inject}}`, `{{random}}`) are kept verbatim — they are
 * later passes' concern, never silently erased. Whitespace and letter case inside
 * the braces are tolerated (`{{ CHAR }}` == `{{char}}`); an empty value resolves
 * to an empty string. No locale, clock, or Android API is touched — the values
 * are supplied by the caller so assembly stays pure and platform-free (KMP rule).
 */
class MacroResolver(
    private val charName: String,
    private val userName: String,
    private val date: String,
    private val time: String,
) {
    fun resolve(text: String): String {
        if (!text.contains("{{")) return text
        var out = text
        out = REGEX_CHAR.replace(out) { charName }
        out = REGEX_USER.replace(out) { userName }
        out = REGEX_DATE.replace(out) { date }
        out = REGEX_TIME.replace(out) { time }
        return out
    }

    companion object {
        private val REGEX_CHAR = Regex("""\{\{\s*char\s*\}\}""", RegexOption.IGNORE_CASE)
        private val REGEX_USER = Regex("""\{\{\s*user\s*\}\}""", RegexOption.IGNORE_CASE)
        private val REGEX_DATE = Regex("""\{\{\s*date\s*\}\}""", RegexOption.IGNORE_CASE)
        private val REGEX_TIME = Regex("""\{\{\s*time\s*\}\}""", RegexOption.IGNORE_CASE)
    }
}
