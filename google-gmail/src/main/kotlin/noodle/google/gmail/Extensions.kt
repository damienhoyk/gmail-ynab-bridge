package noodle.google.gmail

// Pre-compiled Regex objects to avoid redundant compilation on every function call.
private val HTML_TAG_REGEX = "<[^>]*>".toRegex()
private val NBSP_REGEX = "\\u00A0|&nbsp;".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()
private val LINE_BREAK_REGEX = "[\\r\\n]+".toRegex()

/**
 * Strips HTML tags and normalizes whitespace.
 * The whitespace normalization (\s+) also collapses line breaks.
 */
fun String.stripHtml() = replace(HTML_TAG_REGEX, "")
    .replace(NBSP_REGEX, " ")
    .replace(WHITESPACE_REGEX, " ")

/**
 * Collapses multiple line breaks into a single space.
 */
fun String.stripLineBreaks() = replace(LINE_BREAK_REGEX, " ")
