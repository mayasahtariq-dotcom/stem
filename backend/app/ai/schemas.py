"""The JSON shapes the AI must return, checked strictly. Invalid output gets one retry, then
the item is marked as needing review (PRD M2)."""

from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class Tri(BaseModel):
    """The same text in English, Hindi and Kannada."""

    model_config = ConfigDict(extra="ignore")
    en: str = Field(min_length=1)
    hi: str = Field(min_length=1)
    kn: str = Field(min_length=1)


class SpokenScript(BaseModel):
    """What a phone call says for this question in one language (used in M5)."""

    model_config = ConfigDict(extra="ignore")
    prompt: str = Field(min_length=1)
    options: list[str] = []
    explanation: str = ""


class CallScript(BaseModel):
    model_config = ConfigDict(extra="ignore")
    hi: SpokenScript
    kn: SpokenScript


class OptionRef(BaseModel):
    """An answer option. Options point to a term (by key) where they name one; otherwise they
    carry their own text, e.g. a number."""

    model_config = ConfigDict(extra="ignore")
    term_key: str | None = None
    text: Tri | None = None

    @model_validator(mode="after")
    def needs_term_or_text(self) -> "OptionRef":
        if not self.term_key and self.text is None:
            raise ValueError("option needs a term_key or text")
        return self


class QuestionBase(BaseModel):
    model_config = ConfigDict(extra="ignore")
    source_page: int = Field(ge=1)
    confidence: float = Field(ge=0, le=1)
    flag_reason: str | None = None
    prompt: Tri
    explanation: Tri
    call_safe: bool
    call_script: CallScript


class MultipleChoice(QuestionBase):
    type: Literal["multiple_choice"]
    options: list[OptionRef] = Field(min_length=3, max_length=4)
    correct_index: int = Field(ge=0)

    @model_validator(mode="after")
    def correct_in_range(self) -> "MultipleChoice":
        if self.correct_index >= len(self.options):
            raise ValueError("correct_index out of range")
        return self


class MatchPair(BaseModel):
    model_config = ConfigDict(extra="ignore")
    left: OptionRef
    right: OptionRef


class MatchPairs(QuestionBase):
    type: Literal["match_pairs"]
    pairs: list[MatchPair] = Field(min_length=3, max_length=5)


class FillFormula(QuestionBase):
    type: Literal["fill_formula"]
    formula: str = Field(min_length=3)  # contains "__" for the blank
    answer: str = Field(min_length=1)
    chips: list[str] = Field(min_length=2, max_length=8)

    @model_validator(mode="after")
    def blank_and_answer(self) -> "FillFormula":
        if "__" not in self.formula:
            raise ValueError("formula needs a __ blank")
        if self.answer not in self.chips:
            raise ValueError("answer must be one of the chips")
        return self


class TrueFalse(QuestionBase):
    type: Literal["true_false"]
    statement: Tri
    answer: bool


Question = Annotated[
    MultipleChoice | MatchPairs | FillFormula | TrueFalse, Field(discriminator="type")
]


class LessonQuestions(BaseModel):
    model_config = ConfigDict(extra="ignore")
    questions: list[Question] = Field(min_length=6, max_length=10)


class SingleQuestion(BaseModel):
    model_config = ConfigDict(extra="ignore")
    question: Question


class OutlineLesson(BaseModel):
    model_config = ConfigDict(extra="ignore")
    title: Tri
    summary: str = ""


class OutlineUnit(BaseModel):
    model_config = ConfigDict(extra="ignore")
    title: Tri
    lessons: list[OutlineLesson] = Field(min_length=1, max_length=12)


class KeyTerm(BaseModel):
    model_config = ConfigDict(extra="ignore")
    key: str = Field(min_length=1, max_length=60)
    en: str = Field(min_length=1)
    source_page: int = Field(ge=1)

    @field_validator("key")
    @classmethod
    def slug(cls, v: str) -> str:
        return "_".join(v.lower().split())


class Outline(BaseModel):
    model_config = ConfigDict(extra="ignore")
    subject: Literal["BI", "CH", "PH"]
    grade: int = Field(ge=6, le=12)
    title: Tri
    units: list[OutlineUnit] = Field(min_length=1, max_length=8)
    key_terms: list[KeyTerm] = Field(min_length=1, max_length=40)


class Term(BaseModel):
    """A term card: forms in three scripts, transliterations, formal translation, meaning,
    an everyday analogy, related terms and where it came from."""

    model_config = ConfigDict(extra="ignore")
    key: str
    forms: Tri  # the word students hear in class, per language (often a transliteration)
    transliteration: Tri  # how each form is written in Latin letters
    formal: Tri  # formal translation (shown on the term card)
    meaning: Tri
    analogy: Tri
    related: list[str] = []
    source_page: int = Field(ge=1)

    @field_validator("key")
    @classmethod
    def slug(cls, v: str) -> str:
        return "_".join(v.lower().split())


class TermCards(BaseModel):
    model_config = ConfigDict(extra="ignore")
    terms: list[Term] = Field(min_length=1)
