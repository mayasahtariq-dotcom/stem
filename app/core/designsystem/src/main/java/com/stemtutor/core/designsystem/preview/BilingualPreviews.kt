package com.stemtutor.core.designsystem.preview

import androidx.compose.ui.tooling.preview.Preview

/** Renders a preview once in Hindi mode and once in Kannada mode. */
@Preview(name = "Hindi", locale = "hi", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Kannada", locale = "kn", showBackground = true, backgroundColor = 0xFFFFFFFF)
annotation class BilingualPreviews
