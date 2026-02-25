package noodle.google.gmail

private val htmlRegex = "<[^>]*>".toRegex()
private val nbspRegex = "\\u00A0|&nbsp;".toRegex()
private val whitespaceRegex = "\\s+".toRegex()

fun String.stripHtml() = replace(htmlRegex, "")
    .replace(nbspRegex, " ")
    .replace(whitespaceRegex, " ")
