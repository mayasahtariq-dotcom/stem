package com.stemtutor.feature.student.lesson

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri

/** One turn of the lesson: a question, and whether it's the second go at one answered wrongly. */
data class Turn(val questionIndex: Int, val retry: Boolean = false)

/**
 * The order questions are asked in. Questions answered wrongly the first time are asked once
 * more at the end of the lesson (PRD M3); a second miss doesn't add them again.
 */
data class LessonQueue(val turns: List<Turn>, val position: Int = 0) {
    val current: Turn? get() = turns.getOrNull(position)
    val finished: Boolean get() = position >= turns.size
    val progress: Float get() = if (turns.isEmpty()) 0f else position.toFloat() / turns.size

    fun answered(correct: Boolean): LessonQueue {
        val turn = current ?: return this
        return if (!correct && !turn.retry) copy(turns = turns + Turn(turn.questionIndex, retry = true)) else this
    }

    fun next(): LessonQueue = copy(position = position + 1)

    companion object {
        fun of(questionCount: Int) = LessonQueue(List(questionCount) { Turn(it) })
    }
}

/**
 * Match pairs: tap a tile on the left, then its partner on the right. Matched pairs fade out;
 * a wrong pair shakes red but costs no heart. Done when everything is matched.
 * [rightOrder] maps each shown right-hand slot to the pair it belongs to (shuffled).
 */
data class MatchState(
    val pairCount: Int,
    val rightOrder: List<Int>,
    val selectedLeft: Int? = null,
    val matched: Set<Int> = emptySet(),
    val mistakes: Int = 0,
    /** The right-hand slot that was just tapped wrongly (it shakes), cleared on the next tap. */
    val wrongSlot: Int? = null,
) {
    val done: Boolean get() = matched.size == pairCount

    fun tapLeft(pair: Int): MatchState =
        if (pair in matched) this else copy(selectedLeft = if (selectedLeft == pair) null else pair, wrongSlot = null)

    fun tapRight(slot: Int): MatchState {
        val left = selectedLeft ?: return copy(wrongSlot = null)
        val pair = rightOrder.getOrNull(slot) ?: return this
        if (pair in matched) return this
        return if (pair == left) {
            copy(matched = matched + pair, selectedLeft = null, wrongSlot = null)
        } else {
            copy(mistakes = mistakes + 1, wrongSlot = slot, selectedLeft = null)
        }
    }

    companion object {
        /** A shuffled right column that's stable for the same question (so it survives recomposition). */
        fun of(pairCount: Int, seed: Int): MatchState {
            val order = (0 until pairCount).shuffled(kotlin.random.Random(seed))
            val shuffled = if (pairCount > 1 && order == order.sorted()) order.drop(1) + order.first() else order
            return MatchState(pairCount, shuffled)
        }
    }
}

/** The correct answer as text, for the "Not quite" sheet (null for match pairs). */
fun QuestionBody.correctText(trueText: Tri, falseText: Tri): Tri? = when (this) {
    is QuestionBody.MultipleChoice -> options.getOrNull(correctIndex)
    is QuestionBody.TrueFalse -> if (answer) trueText else falseText
    is QuestionBody.FillFormula -> Tri(answer, answer, answer)
    is QuestionBody.MatchPairs -> null
}

/** A science word found in a piece of text, to underline and open as a term card. */
data class TermSpan(val start: Int, val end: Int, val key: String)

/**
 * Finds the course's terms inside [text]: the regional form for the regional line, the English
 * form (case-insensitive) for the English line. Longer forms win, and spans never overlap.
 */
object TermMatcher {
    fun find(text: String, terms: Collection<Term>, mode: LanguageMode?, english: Boolean): List<TermSpan> {
        val forms = terms.mapNotNull { t ->
            val form = if (english) t.forms.en else mode?.let { t.forms.regional(it) }
            form?.trim()?.takeIf { it.length >= 2 }?.let { it to t.key }
        }.sortedByDescending { it.first.length }
        val spans = mutableListOf<TermSpan>()
        for ((form, key) in forms) {
            var from = 0
            while (true) {
                val at = text.indexOf(form, from, ignoreCase = english)
                if (at < 0) break
                val end = at + form.length
                val wordStart = at == 0 || !text[at - 1].isLetterOrDigit()
                val wordEnd = end == text.length || !text[end].isLetterOrDigit() || !english
                if (wordStart && wordEnd && spans.none { at < it.end && end > it.start }) {
                    spans += TermSpan(at, end, key)
                }
                from = end
            }
        }
        return spans.sortedBy { it.start }
    }
}

/** Glossary search: a term matches if any of its forms, transliterations or formal names do. */
fun Term.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return listOf(forms, transliteration, formal).any { t ->
        t.en.contains(q, ignoreCase = true) || t.hi.contains(q) || t.kn.contains(q)
    }
}
