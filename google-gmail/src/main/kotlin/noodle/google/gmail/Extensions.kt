package noodle.google.gmail

private val HTML_TAG_REGEX = "<[^>]*>".toRegex()
private val NBSP_REGEX = "\\u00A0|&nbsp;".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()
private val LINE_BREAK_REGEX = "[\\r\\n]+".toRegex()

/**
 * Strips HTML tags, replaces non-breaking spaces with normal spaces,
 * and collapses all whitespace sequences (including line breaks) into a single space.
 */
fun String.stripHtml() = replace(HTML_TAG_REGEX, "")
    .replace(NBSP_REGEX, " ")
    .replace(WHITESPACE_REGEX, " ")

/**
 * Replaces sequences of line breaks with a single space.
 */
fun String.stripLineBreaks() = replace(LINE_BREAK_REGEX, " ")
