package com.stemtutor.core.data.classroom

import com.stemtutor.core.data.model.Subject
import kotlin.random.Random

/**
 * Join keys look like BI09-K4X2. The server issues real keys; this mirrors its rules for input
 * handling and for the fake repository. The alphabet has no 0, O, 1, I or L.
 */
object JoinKeys {
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 8 // characters, without the dash
    private val PATTERN = Regex("^(BI|CH|PH)(0[6-9]|1[0-2])-[$ALPHABET]{4}$")
    private val IN_TEXT = Regex("(BI|CH|PH)(0[6-9]|1[0-2])-?[$ALPHABET]{4}", RegexOption.IGNORE_CASE)

    /** Keeps letters and digits, uppercased, up to 8 characters (no dash). */
    fun sanitize(raw: String): String =
        raw.uppercase().filter { it.isLetterOrDigit() && it.code < 128 }.take(LENGTH)

    /** "BI09K4X2" -> "BI09-K4X2"; partial input gets the dash once it's 5+ characters. */
    fun format(compact: String): String =
        if (compact.length > 4) compact.substring(0, 4) + "-" + compact.substring(4) else compact

    fun isComplete(compact: String): Boolean = PATTERN.matches(format(compact))

    /** Finds a key inside scanned or shared text, such as https://host/join/BI09-K4X2. */
    fun findIn(text: String): String? =
        IN_TEXT.find(text)?.value?.let { format(sanitize(it)) }?.takeIf { PATTERN.matches(it) }

    fun generate(subject: Subject, grade: Int, random: Random = Random.Default): String {
        val suffix = (1..4).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        return "${subject.code}${grade.toString().padStart(2, '0')}-$suffix"
    }

    fun webLink(host: String, key: String) = "https://$host/join/$key"

    fun appLink(key: String) = "stemtutor://join/$key"
}
