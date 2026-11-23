package dev.ether.app.domain

/** The ways a match can reach someone. Telegram is intentionally not one of them. */
enum class ContactKind {
    WHATSAPP, INSTAGRAM, LINE;

    /** Deep link for an already-normalized [value]. */
    fun url(value: String): String = when (this) {
        WHATSAPP -> "https://wa.me/$value"
        INSTAGRAM -> "https://instagram.com/$value"
        LINE -> "https://line.me/ti/p/~$value"
    }

    /** How a normalized [value] is shown: +62…, @handle, or the LINE ID as is. */
    fun display(value: String): String = when (this) {
        WHATSAPP -> "+$value"
        INSTAGRAM -> "@$value"
        LINE -> value
    }
}

/** One way to reach someone: what to show, and the link that opens the chat. */
data class ContactEntry(val kind: ContactKind, val display: String, val url: String)

/**
 * A profile's contacts, each optional. Values are normalized: [phone] is digits with a country code
 * (no "+"), [instagram] and [lineId] are lowercase without "@". The formats mirror the
 * `profile_contacts` check constraints; the database stays the source of truth.
 */
data class Contacts(val phone: String? = null, val instagram: String? = null, val lineId: String? = null) {
    val isEmpty: Boolean get() = phone == null && instagram == null && lineId == null

    /** Available contacts in display order: WhatsApp, Instagram, LINE. */
    val entries: List<ContactEntry>
        get() = listOf(ContactKind.WHATSAPP to phone, ContactKind.INSTAGRAM to instagram, ContactKind.LINE to lineId)
            .mapNotNull { (kind, value) -> value?.let { ContactEntry(kind, kind.display(it), kind.url(it)) } }

    companion object {
        val NONE = Contacts()

        private val phoneDigits = Regex("[0-9]{8,16}")
        private val phoneSeparators = Regex("[\\s().-]")
        private val instagramHandle = Regex("[a-z0-9._]{1,30}")
        private val instagramUrl = Regex("^(https?://)?(www\\.)?instagram\\.com/", RegexOption.IGNORE_CASE)
        private val lineIdPattern = Regex("[a-z0-9._-]{4,20}")

        /**
         * Digits with a country code, or null when [raw] is not a phone number. Spaces, dashes, dots
         * and brackets are ignored. An Indonesian number typed as 08… becomes 628…; "+62…" and "62…" stay.
         */
        fun normalizePhone(raw: String): String? {
            val compact = raw.trim().replace(phoneSeparators, "")
            val digits = when {
                compact.startsWith("+") -> compact.drop(1)
                compact.startsWith("0") -> "62" + compact.drop(1)
                else -> compact
            }
            return digits.takeIf { phoneDigits.matches(it) }
        }

        /** The handle without "@", lowercased, or null when invalid. A pasted profile link also works. */
        fun normalizeInstagram(raw: String): String? =
            raw.trim().replace(instagramUrl, "").trimEnd('/').removePrefix("@").lowercase()
                .takeIf { instagramHandle.matches(it) }

        /** The LINE ID lowercased, or null when invalid. */
        fun normalizeLineId(raw: String): String? =
            raw.trim().lowercase().takeIf { lineIdPattern.matches(it) }

        /**
         * Builds from stored values, normalizing them and dropping any that are blank or no longer
         * valid, so the UI only ever sees openable links.
         */
        fun of(phone: String?, instagram: String?, lineId: String?) = Contacts(
            phone?.let(::normalizePhone),
            instagram?.let(::normalizeInstagram),
            lineId?.let(::normalizeLineId),
        )
    }
}
