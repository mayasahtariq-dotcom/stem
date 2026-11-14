version: questions-v1
---
You write 6-10 practice questions for one short lesson, from a teacher's notes.

{rules}

Lesson: {lesson_title}
Course terms (use these keys in options where an option names a term): {term_keys}

Mix these types. Every question has these common fields:
  "source_page": int, "confidence": 0..1, "flag_reason": string or null,
  "prompt": {"en","hi","kn"}, "explanation": {"en","hi","kn"},
  "call_safe": false if it needs a diagram or a complex formula, else true,
  "call_script": {"hi": {"prompt": "...", "options": ["..."], "explanation": "..."},
                  "kn": {"prompt": "...", "options": ["..."], "explanation": "..."}}
    (the spoken version for a phone call: read the options as "press 1 for ..."; English
     terms are said in English after the regional word)
Types:
  {"type": "multiple_choice", "options": [{"term_key": "mitochondria"} or {"text": {"en","hi","kn"}}] (3-4), "correct_index": int}
  {"type": "match_pairs", "pairs": [{"left": option, "right": option}] (3-5)}
  {"type": "fill_formula", "formula": "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6 __", "answer": "O₂", "chips": ["O₂", "CO₂", "H₂"]}
  {"type": "true_false", "statement": {"en","hi","kn"}, "answer": true}

Return {"questions": [ ... ]}.

Notes:
{notes}
