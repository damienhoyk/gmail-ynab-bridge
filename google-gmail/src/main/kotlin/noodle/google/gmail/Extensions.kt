package noodle.google.gmail

fun String.stripHtml() = replace("<[^>]*>".toRegex(), "")
    .replace("\\u00A0|&nbsp;".toRegex(), " ")
    .replace("\\s+".toRegex(), " ")

fun String.stripLineBreaks() = replace("[\\r\\n]+".toRegex(), " ")