package com.stemtutor.core.data.learning

import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.QuestionBody
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Checks an answer on the phone so feedback is instant. The server grades again and is the
 * one that awards XP and takes hearts (PRD: all of that is calculated on the server).
 */
object Grading {
    fun isCorrect(body: QuestionBody, answer: Answer): Boolean = when (body) {
        is QuestionBody.MultipleChoice -> (answer as? Answer.Choice)?.index == body.correctIndex
        is QuestionBody.TrueFalse -> (answer as? Answer.TrueFalse)?.value == body.answer
        is QuestionBody.FillFormula -> (answer as? Answer.Chip)?.chip == body.answer
        is QuestionBody.MatchPairs -> (answer as? Answer.Pairs)?.let { it.mistakes == 0 && it.count == body.pairs.size } == true
    }

    /** Wrong pairs in match pairs cost no heart; every other wrong answer costs one. */
    fun costsHeart(body: QuestionBody): Boolean = body !is QuestionBody.MatchPairs

    /** The JSON the server's grader expects. */
    fun encode(answer: Answer): JsonObject = when (answer) {
        is Answer.Choice -> buildJsonObject { put("index", answer.index) }
        is Answer.TrueFalse -> buildJsonObject { put("value", answer.value) }
        is Answer.Chip -> buildJsonObject { put("chip", answer.chip) }
        is Answer.Pairs -> buildJsonObject {
            put(
                "pairs",
                buildJsonArray {
                    repeat(answer.count) { i -> add(buildJsonArray { add(JsonPrimitive(i)); add(JsonPrimitive(i)) }) }
                },
            )
            put("mistakes", answer.mistakes)
        }
    }
}
