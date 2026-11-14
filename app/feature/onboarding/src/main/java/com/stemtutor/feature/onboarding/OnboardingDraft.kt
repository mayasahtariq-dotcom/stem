package com.stemtutor.feature.onboarding

import com.stemtutor.core.data.model.Profile
import com.stemtutor.core.data.model.Role
import javax.inject.Inject
import javax.inject.Singleton

/** Answers collected across the onboarding screens until the profile is saved. */
@Singleton
class OnboardingDraft @Inject constructor() {
    var phone: String = ""
    var role: Role? = null

    fun clear() {
        phone = ""
        role = null
    }
}

/** Where onboarding sends the user next. */
sealed interface OnboardingNav {
    data object Welcome : OnboardingNav
    data object Phone : OnboardingNav
    data object Otp : OnboardingNav
    data object Language : OnboardingNav
    data object Role : OnboardingNav
    data object Name : OnboardingNav
    data class Home(val role: com.stemtutor.core.data.model.Role) : OnboardingNav
}

/** The role a returning user lands on: the one they last used, else their first role. */
fun homeRoleFor(profile: Profile, lastRole: Role?): Role? =
    lastRole?.takeIf { it in profile.roles } ?: profile.roles.firstOrNull()
