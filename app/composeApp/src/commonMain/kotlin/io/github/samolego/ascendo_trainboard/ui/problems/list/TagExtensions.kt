package io.github.samolego.ascendo_trainboard.ui.problems.list

import io.github.samolego.ascendo_trainboard.api.generated.models.Attempt
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.Tag

/**
 * JSON / API keys for the generated [Tag] properties.
 */
const val TAG_KEY_NEGATED = "negated"
const val TAG_KEY_TEKMVALNI = "Tekmovalni"
const val TAG_KEY_ZMAGVALNI = "Zmagovalni"
const val TAG_KEY_AVTOR = "Avtor"
const val TAG_KEY_SPREMENJENI_ZA_DATUMOM = "SpremenjeniZaDatumom"
const val TAG_KEY_SPLEZANI = "Splezani"
const val TAG_KEY_IME = "Ime"
const val TAG_KEY_MIN_GRADE = "MinGrade"
const val TAG_KEY_MAX_GRADE = "MaxGrade"
const val TAG_KEY_SECTOR_ID = "SectorId"

/**
 * A user-facing recommendation shown while filtering a tag.
 */
data class TagRecommendation(
    val label: String,
    val value: String,
)

/**
 * Describes one generated tag that the user can search by.
 *
 * Each instance knows its [apiKey] (how the tag is encoded for the backend),
 * a friendly [label], how to create a concrete [Tag] from typed input, and
 * which values can be recommended.
 */
sealed class SearchableTag(
    val apiKey: String,
    val label: String,
) {
    /** Whether this tag represents a boolean property. Boolean tags get Da/Ne buttons instead of a negate toggle. */
    open val isBoolean: Boolean = false

    /**
     * Try to create a [Tag] from the user's typed [value] and [negated] flag.
     * Returns `null` when [value] is not valid for this tag type.
     */
    abstract fun createTag(value: String, negated: Boolean = false): Tag?

    /**
     * Recommendations to show for this tag kind. [sectors] is supplied so that
     * sector names can be resolved.
     */
    abstract fun recommendations(sectors: List<SectorSummary>): List<TagRecommendation>

    data object Tekmovalni : SearchableTag(TAG_KEY_TEKMVALNI, "Tekmovalni") {
        override val isBoolean = true

        override fun createTag(value: String, negated: Boolean): Tag? {
            val bool = when (value.lowercase()) {
                "da", "yes", "true" -> true
                "ne", "no", "false" -> false
                else -> value.toBooleanStrictOrNull()
            } ?: return null
            return Tag(tekmovalni = bool, negated = negated)
        }

        override fun recommendations(sectors: List<SectorSummary>) = listOf(
            TagRecommendation("Da", "true"),
            TagRecommendation("Ne", "false"),
        )
    }

    data object Zmagovalni : SearchableTag(TAG_KEY_ZMAGVALNI, "Zmagovalni") {
        override val isBoolean = true

        override fun createTag(value: String, negated: Boolean): Tag? {
            val bool = when (value.lowercase()) {
                "da", "yes", "true" -> true
                "ne", "no", "false" -> false
                else -> value.toBooleanStrictOrNull()
            } ?: return null
            return Tag(zmagovalni = bool, negated = negated)
        }

        override fun recommendations(sectors: List<SectorSummary>) = listOf(
            TagRecommendation("Da", "true"),
            TagRecommendation("Ne", "false"),
        )
    }

    data object Splezani : SearchableTag(TAG_KEY_SPLEZANI, "Splezani") {
        override fun createTag(value: String, negated: Boolean): Tag? {
            val attempt = Attempt.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: return null
            return Tag(splezani = attempt, negated = negated)
        }

        override fun recommendations(sectors: List<SectorSummary>) =
            Attempt.entries.map { TagRecommendation(it.name, it.name) }
    }

    data object Avtor : SearchableTag(TAG_KEY_AVTOR, "Avtor") {
        override fun createTag(value: String, negated: Boolean): Tag? =
            value.takeIf { it.isNotBlank() }?.let { Tag(avtor = it, negated = negated) }

        override fun recommendations(sectors: List<SectorSummary>) = emptyList<TagRecommendation>()
    }

    data object Ime : SearchableTag(TAG_KEY_IME, "Ime") {
        override fun createTag(value: String, negated: Boolean): Tag? =
            value.takeIf { it.isNotBlank() }?.let { Tag(ime = it, negated = negated) }

        override fun recommendations(sectors: List<SectorSummary>) = emptyList<TagRecommendation>()
    }

    data object SectorId : SearchableTag(TAG_KEY_SECTOR_ID, "Sektor") {
        override fun createTag(value: String, negated: Boolean): Tag? =
            value.toIntOrNull()?.let { Tag(sectorId = it, negated = negated) }

        override fun recommendations(sectors: List<SectorSummary>) =
            sectors.map { TagRecommendation(it.name, it.id.toString()) }
    }
}

/**
 * All tags that can be selected in the search UI.
 *
 * [Tag.minGrade]/[Tag.maxGrade] are omitted because they are handled by the
 * dedicated grade-range selector. [Tag.spremenjeniZaDatumom] is omitted because
 * a raw timestamp is not user-friendly.
 */
val SearchableTags: List<SearchableTag> = listOf(
    SearchableTag.Tekmovalni,
    SearchableTag.Zmagovalni,
    SearchableTag.Splezani,
    SearchableTag.Avtor,
    SearchableTag.Ime,
    SearchableTag.SectorId,
)

/**
 * Returns the matching [SearchableTag] definition for this tag instance,
 * or `null` if the tag has no active property.
 */
fun Tag.searchableTag(): SearchableTag? =
    searchableKey()?.let { key -> SearchableTags.find { it.apiKey == key } }

/**
 * Returns the API key of the single active property in this tag, or `null` if
 * none is set. [Tag.negated] is ignored because it is a modifier, not a filter
 * kind.
 */
fun Tag.searchableKey(): String? = when {
    tekmovalni != null -> TAG_KEY_TEKMVALNI
    zmagovalni != null -> TAG_KEY_ZMAGVALNI
    avtor != null -> TAG_KEY_AVTOR
    spremenjeniZaDatumom != null -> TAG_KEY_SPREMENJENI_ZA_DATUMOM
    splezani != null -> TAG_KEY_SPLEZANI
    ime != null -> TAG_KEY_IME
    minGrade != null -> TAG_KEY_MIN_GRADE
    maxGrade != null -> TAG_KEY_MAX_GRADE
    sectorId != null -> TAG_KEY_SECTOR_ID
    else -> null
}

/**
 * Returns a user-friendly label for this tag's active property.
 */
fun Tag.searchableLabel(): String =
    searchableTag()?.label ?: searchableKey() ?: ""

/**
 * Returns the value of this tag formatted for display.
 */
fun Tag.displayValue(sectorNameResolver: (Int) -> String?): String = when {
    tekmovalni != null -> if (tekmovalni) "Da" else "Ne"
    zmagovalni != null -> if (zmagovalni) "Da" else "Ne"
    splezani != null -> splezani.name
    sectorId != null -> sectorNameResolver(sectorId) ?: sectorId.toString()
    avtor != null -> avtor
    ime != null -> ime
    spremenjeniZaDatumom != null -> spremenjeniZaDatumom.toString()
    else -> ""
}
