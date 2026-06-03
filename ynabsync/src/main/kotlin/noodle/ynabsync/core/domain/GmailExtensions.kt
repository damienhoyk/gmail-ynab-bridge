package noodle.ynabsync.core.domain

private val HTML_TAG_REGEX = "<[^>]*>".toRegex()
private val NON_BREAKING_SPACE_REGEX = "\\u00A0|&nbsp;".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()
private val LINE_BREAK_REGEX = "[\\r\\n]+".toRegex()

internal fun String.stripHtml(): String =
    replace(HTML_TAG_REGEX, "")
        .replace(NON_BREAKING_SPACE_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")

internal fun String.stripLineBreaks(): String = replace(LINE_BREAK_REGEX, " ")
