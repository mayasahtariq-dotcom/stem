package com.stemtutor.core.data.course

import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.QuestionType
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.network.QuestionDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Converts between the backend's question JSON and [LessonQuestion]. Options that point to a
 * term (`term_key`) are shown with the term's forms in each language.
 */
object QuestionCodec {
    fun decode(dto: QuestionDto, terms: Map<String, JsonObject>): LessonQuestion {
        val c = dto.content
        val body = when (QuestionType.fromApi(dto.type)) {
            QuestionType.MULTIPLE_CHOICE -> QuestionBody.MultipleChoice(
                options = c.array("options").map { option(it, terms) },
                correctIndex = c["correct_index"]?.jsonPrimitive?.intOrNull ?: 0,
            )
            QuestionType.MATCH_PAIRS -> QuestionBody.MatchPairs(
                c.array("pairs").map { p -> option(p.jsonObject["left"], terms) to option(p.jsonObject["right"], terms) },
            )
            QuestionType.FILL_FORMULA -> QuestionBody.FillFormula(
                formula = c.string("formula"),
                answer = c.string("answer"),
                chips = c.array("chips").map { it.jsonPrimitive.content },
            )
            QuestionType.TRUE_FALSE -> QuestionBody.TrueFalse(
                statement = tri(c["statement"]),
                answer = c["answer"]?.jsonPrimitive?.booleanOrNull ?: true,
            )
        }
        return LessonQuestion(
            id = dto.id,
            position = dto.position,
            prompt = tri(c["prompt"]),
            explanation = tri(c["explanation"]),
            body = body,
            sourcePage = dto.sourcePage,
            confidence = dto.confidence,
            flagReason = dto.flagReason,
            teacherEdited = dto.teacherEdited,
        )
    }

    /** The JSON the backend expects for `PUT /questions/{id}` (options sent as text). */
    fun encode(q: LessonQuestion): JsonObject = buildJsonObject {
        put("type", q.body.type.apiValue)
        put("prompt", tri(q.prompt))
        put("explanation", tri(q.explanation))
        when (val b = q.body) {
            is QuestionBody.MultipleChoice -> {
                put("options", buildJsonArray { b.options.forEach { add(buildJsonObject { put("text", tri(it)) }) } })
                put("correct_index", b.correctIndex)
            }
            is QuestionBody.MatchPairs -> put(
                "pairs",
                buildJsonArray {
                    b.pairs.forEach { (l, r) ->
                        add(
                            buildJsonObject {
                                put("left", buildJsonObject { put("text", tri(l)) })
                                put("right", buildJsonObject { put("text", tri(r)) })
                            },
                        )
                    }
                },
            )
            is QuestionBody.FillFormula -> {
                put("formula", b.formula)
                put("answer", b.answer)
                put("chips", JsonArray(b.chips.map { JsonPrimitive(it) }))
            }
            is QuestionBody.TrueFalse -> {
                put("statement", tri(b.statement))
                put("answer", b.answer)
            }
        }
    }

    private fun option(e: JsonElement?, terms: Map<String, JsonObject>): Tri {
        val o = e as? JsonObject ?: return Tri.EMPTY
        (o["text"] as? JsonObject)?.let { return tri(it) }
        val key = (o["term_key"] as? JsonPrimitive)?.content ?: return Tri.EMPTY
        val forms = terms[key]?.get("forms")
        return if (forms is JsonObject) tri(forms) else Tri(key, key, key)
    }

    private fun tri(e: JsonElement?): Tri {
        val o = e as? JsonObject ?: return Tri.EMPTY
        fun s(k: String) = (o[k] as? JsonPrimitive)?.content.orEmpty()
        return Tri(s("en"), s("hi"), s("kn"))
    }

    private fun tri(t: Tri) = buildJsonObject {
        put("en", t.en)
        put("hi", t.hi)
        put("kn", t.kn)
    }

    private fun JsonObject.array(k: String): List<JsonElement> = (this[k] as? JsonArray)?.jsonArray ?: emptyList()
    private fun JsonObject.string(k: String) = (this[k] as? JsonPrimitive)?.content.orEmpty()
}
