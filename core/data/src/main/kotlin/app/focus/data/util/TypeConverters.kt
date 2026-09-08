package app.focus.data.util

/** Simple JSON-like list parser for Room TypeConverters. */
fun List<String>.toJson(): String = joinToString("|")

/** Simple JSON-like list parser for Room TypeConverters. */
fun String.fromJsonList(): List<String> {
    if (isBlank()) return emptyList()
    return split("|").map { it.trim('"', '[', ']', ',') }.filter { it.isNotBlank() }
}

/** Convert Set<String> to pipe-separated string for Room storage. */
fun Set<String>.toJsonString(): String = joinToString("|")

/** Parse pipe-separated string back to Set<String>. */
fun String.fromJsonStringSet(): Set<String> {
    if (isBlank()) return emptySet()
    return split("|").filter { it.isNotBlank() }.toSet()
}
