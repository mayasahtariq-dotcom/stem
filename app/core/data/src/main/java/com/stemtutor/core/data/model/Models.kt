package com.stemtutor.core.data.model

import com.stemtutor.core.common.language.LanguageMode

enum class Role(val apiValue: String) {
    STUDENT("student"),
    TEACHER("teacher"),
    PARENT("parent"),
    ;

    companion object {
        fun fromApi(value: String): Role? = entries.firstOrNull { it.apiValue == value }
    }
}

enum class Subject(val code: String) {
    BIOLOGY("BI"),
    CHEMISTRY("CH"),
    PHYSICS("PH"),
    ;

    companion object {
        fun fromCode(code: String): Subject = entries.firstOrNull { it.code == code } ?: BIOLOGY
    }
}

val GRADES = 6..12

fun LanguageMode.Companion.fromApi(value: String?): LanguageMode = fromStorageKey(value)

data class Profile(
    val id: String,
    val registered: Boolean,
    val name: String = "",
    val phone: String = "",
    val roles: List<Role> = emptyList(),
    val languageMode: LanguageMode = LanguageMode.DEFAULT,
)

data class TeacherClassroom(
    val id: String,
    val name: String,
    val subject: Subject,
    val grade: Int,
    val joinKey: String,
    val allowedModes: List<LanguageMode>,
    val studentCount: Int,
)

data class Member(val id: String, val name: String)

data class ClassroomDetail(val classroom: TeacherClassroom, val members: List<Member>)

data class ClassPreview(
    val id: String,
    val name: String,
    val teacherName: String,
    val subject: Subject,
    val grade: Int,
    val studentCount: Int,
    val allowedModes: List<LanguageMode>,
)

data class StudentClassroom(
    val id: String,
    val name: String,
    val teacherName: String,
    val subject: Subject,
    val grade: Int,
    val studentCount: Int,
)

data class NewClassroom(
    val name: String,
    val subject: Subject,
    val grade: Int,
    val allowedModes: List<LanguageMode>,
)
