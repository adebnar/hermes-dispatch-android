package co.hermesdispatch.app.domain

/** Pure helpers for pulling shareable artifacts out of assistant text. */
object Artifacts {
    private val URL_REGEX = Regex("""https?://[^\s<>"')\]]+""")
    private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp")

    /** Extract de-duplicated http(s) URLs from a block of text, in order. */
    fun extract(text: String): List<Artifact> {
        if (text.isBlank()) return emptyList()
        val seen = LinkedHashSet<String>()
        for (match in URL_REGEX.findAll(text)) {
            seen.add(match.value.trimEnd('.', ',', ';', ':'))
        }
        return seen.map { Artifact(url = it, isImage = isImageUrl(it)) }
    }

    private fun isImageUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#')
        val ext = path.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXT
    }
}
