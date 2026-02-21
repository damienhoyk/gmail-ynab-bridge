package noodle.google.gmail

// Pre-compile regex patterns to avoid recompilation on every call to stripHtml or stripLineBreaks
private val HTML_TAGS_REGEX = "<[^>]*>".toRegex()
private val NBSP_REGEX = "\\u00A0|&nbsp;".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()
private val LINE_BREAKS_REGEX = "[\\r\\n]+".toRegex()

fun String.stripHtml() = replace(HTML_TAGS_REGEX, "")
    .replace(NBSP_REGEX, " ")
    .replace(WHITESPACE_REGEX, " ")

fun String.stripLineBreaks() = replace(LINE_BREAKS_REGEX, " ")
