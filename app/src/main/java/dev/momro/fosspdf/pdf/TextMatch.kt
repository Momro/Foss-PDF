package dev.momro.fosspdf.pdf

fun findMatchIndices(haystack: String, query: String): List<Int> {
    if (query.isEmpty()) return emptyList()
    val h = haystack.lowercase()
    val q = query.lowercase()
    val matches = mutableListOf<Int>()
    var index = 0
    while (index <= h.length - q.length) {
        if (h.regionMatches(index, q, 0, q.length)) {
            matches.add(index)
            index += q.length
        } else {
            index++
        }
    }
    return matches
}
