package noodle.uri

import java.net.URI

public fun URI.namedSegment(key: String): String? =
    path
        .splitToSequence('/')
        .zipWithNext()
        .firstOrNull { (k, _) -> k == key }
        ?.second
