"""Sample AI output for the fake LLM (tests and development): a Class 9 Biology course on the
cell, with one flagged question. Hindi and Kannada lines here are drafts."""

import json
import re
from typing import Any

from app.ai.llm import LLMRequest


def _tri(en: str, hi: str, kn: str) -> dict[str, str]:
    return {"en": en, "hi": hi, "kn": kn}


def _call(hi_prompt: str, kn_prompt: str, hi_opts: list[str], kn_opts: list[str]) -> dict[str, Any]:
    return {
        "hi": {"prompt": hi_prompt, "options": hi_opts, "explanation": ""},
        "kn": {"prompt": kn_prompt, "options": kn_opts, "explanation": ""},
    }


OUTLINE: dict[str, Any] = {
    "subject": "BI",
    "grade": 9,
    "title": _tri("The Cell", "कोशिका", "ಜೀವಕೋಶ"),
    "units": [
        {
            "title": _tri("Inside the cell", "कोशिका के अंदर", "ಜೀವಕೋಶದ ಒಳಗೆ"),
            "lessons": [
                {
                    "title": _tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"),
                    "summary": "Organelles",
                },
                {
                    "title": _tri(
                        "Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"
                    ),
                    "summary": "Differences",
                },
                {
                    "title": _tri("Energy in the cell", "कोशिका में ऊर्जा", "ಜೀವಕೋಶದಲ್ಲಿ ಶಕ್ತಿ"),
                    "summary": "ATP",
                },
            ],
        }
    ],
    "key_terms": [
        {"key": "mitochondria", "en": "Mitochondria", "source_page": 1},
        {"key": "nucleus", "en": "Nucleus", "source_page": 1},
        {"key": "ribosome", "en": "Ribosome", "source_page": 1},
        {"key": "cell_wall", "en": "Cell wall", "source_page": 1},
        {"key": "chloroplast", "en": "Chloroplast", "source_page": 1},
    ],
}

_TERM_FORMS = {
    "mitochondria": ("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"),
    "nucleus": ("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್"),
    "ribosome": ("Ribosome", "राइबोसोम", "ರೈಬೋಸೋಮ್"),
    "cell_wall": ("Cell wall", "कोशिका भित्ति", "ಕೋಶ ಭಿತ್ತಿ"),
    "chloroplast": ("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್"),
}


def _term(key: str) -> dict[str, Any]:
    en, hi, kn = _TERM_FORMS.get(key, (key.title(), key, key))
    return {
        "key": key,
        "forms": _tri(en, hi, kn),
        "transliteration": _tri(en.lower(), en.lower(), en.lower()),
        "formal": _tri(en, hi, kn),
        "meaning": _tri(
            f"{en} is a part of the cell.", f"{hi} कोशिका का एक भाग है।", f"{kn} ಜೀವಕೋಶದ ಒಂದು ಭಾಗ."
        ),
        "analogy": _tri("Like a room in a house.", "जैसे घर का एक कमरा।", "ಮನೆಯ ಒಂದು ಕೋಣೆಯಂತೆ."),
        "related": [],
        "source_page": 1,
    }


def _mcq(
    prompt: dict[str, str], keys: list[str], correct: int, page: int = 1, **extra: Any
) -> dict[str, Any]:
    return {
        "type": "multiple_choice",
        "source_page": page,
        "confidence": extra.pop("confidence", 0.9),
        "flag_reason": extra.pop("flag_reason", None),
        "prompt": prompt,
        "explanation": _tri(
            "Mitochondria make energy from food, like a stove in the kitchen.",
            "माइटोकॉन्ड्रिया भोजन से ऊर्जा बनाता है, जैसे रसोई का चूल्हा।",
            "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಆಹಾರದಿಂದ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ, ಅಡುಗೆಮನೆಯ ಒಲೆಯಂತೆ.",
        ),
        "call_safe": True,
        "call_script": _call(
            prompt["hi"],
            prompt["kn"],
            [_TERM_FORMS[k][1] for k in keys],
            [_TERM_FORMS[k][2] for k in keys],
        ),
        "options": [{"term_key": k} for k in keys],
        "correct_index": correct,
    }


def _tf(statement: dict[str, str], answer: bool, page: int = 1) -> dict[str, Any]:
    return {
        "type": "true_false",
        "source_page": page,
        "confidence": 0.9,
        "flag_reason": None,
        "prompt": _tri("True or false?", "सही या गलत?", "ಸರಿ ಅಥವಾ ತಪ್ಪು?"),
        "statement": statement,
        "answer": answer,
        "explanation": _tri("The notes say so.", "नोट्स में यही लिखा है।", "ನೋಟ್ಸ್‌ನಲ್ಲಿ ಹೀಗೆ ಇದೆ."),
        "call_safe": True,
        "call_script": _call(statement["hi"], statement["kn"], ["सही", "गलत"], ["ಸರಿ", "ತಪ್ಪು"]),
    }


def lesson_questions() -> dict[str, Any]:
    q_power = _tri(
        "Which part of the cell makes energy?",
        "कोशिका का कौन सा भाग ऊर्जा बनाता है?",
        "ಜೀವಕೋಶದ ಯಾವ ಭಾಗ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ?",
    )
    q_control = _tri(
        "Which part controls the cell?",
        "कौन सा भाग कोशिका को नियंत्रित करता है?",
        "ಯಾವ ಭಾಗ ಜೀವಕೋಶವನ್ನು ನಿಯಂತ್ರಿಸುತ್ತದೆ?",
    )
    q_plant = _tri(
        "Which part is found only in plant cells?",
        "कौन सा भाग सिर्फ़ पादप कोशिका में होता है?",
        "ಯಾವ ಭಾಗ ಸಸ್ಯ ಕೋಶದಲ್ಲಿ ಮಾತ್ರ ಇದೆ?",
    )
    q_protein = _tri(
        "Which part makes proteins?", "कौन सा भाग प्रोटीन बनाता है?", "ಯಾವ ಭಾಗ ಪ್ರೋಟೀನ್ ಮಾಡುತ್ತದೆ?"
    )
    return {
        "questions": [
            _mcq(q_power, ["nucleus", "mitochondria", "ribosome"], 1),
            _mcq(q_control, ["nucleus", "mitochondria", "chloroplast"], 0),
            _mcq(
                q_plant,
                ["chloroplast", "mitochondria", "nucleus"],
                0,
                page=2,
                confidence=0.55,
                flag_reason="Handwriting on page 2 is unclear: is this word 'chloroplast'?",
            ),
            _mcq(q_protein, ["ribosome", "nucleus", "cell_wall"], 0),
            _tf(
                _tri(
                    "Plant cells have a cell wall.",
                    "पादप कोशिका में कोशिका भित्ति होती है।",
                    "ಸಸ್ಯ ಕೋಶದಲ್ಲಿ ಕೋಶ ಭಿತ್ತಿ ಇರುತ್ತದೆ.",
                ),
                True,
            ),
            _tf(
                _tri(
                    "Animal cells have chloroplasts.",
                    "जंतु कोशिका में क्लोरोप्लास्ट होता है।",
                    "ಪ್ರಾಣಿ ಕೋಶದಲ್ಲಿ ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್ ಇರುತ್ತದೆ.",
                ),
                False,
            ),
        ]
    }


def fake_llm_response(request: LLMRequest) -> str:
    """Answers each pipeline prompt with sample JSON, based on which prompt it is."""
    p = request.prompt
    if '"key_terms"' in p and "course outline" in p:
        return json.dumps(OUTLINE, ensure_ascii=False)
    if "term cards" in p:
        keys = re.search(r"Keys: (.*)", p)
        wanted = [k.strip() for k in (keys.group(1) if keys else "").split(",") if k.strip()]
        return json.dumps(
            {"terms": [_term(k) for k in wanted or list(_TERM_FORMS)]}, ensure_ascii=False
        )
    if "Rewrite one practice question" in p:
        q = lesson_questions()["questions"][1]
        return json.dumps({"question": q}, ensure_ascii=False)
    if "practice questions" in p:
        return json.dumps(lesson_questions(), ensure_ascii=False)
    return "{}"
