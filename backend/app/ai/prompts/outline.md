version: outline-v1
---
You turn a teacher's class notes into a short course outline for Indian school students.

{rules}

Return JSON with exactly this shape:
{
  "subject": "BI" | "CH" | "PH",
  "grade": 6-12,
  "title": {"en": "...", "hi": "...", "kn": "..."},
  "units": [
    {"title": {"en","hi","kn"}, "lessons": [{"title": {"en","hi","kn"}, "summary": "one line in English"}]}
  ],
  "key_terms": [{"key": "mitochondria", "en": "Mitochondria", "source_page": 1}]
}
Make 1-3 units with 2-6 lessons each, each lesson small enough for 5-10 minutes.
List the 5-20 most important science terms in the notes as key_terms.
Teacher's hints (may be empty): subject={subject}, grade={grade}.

Notes:
{notes}
