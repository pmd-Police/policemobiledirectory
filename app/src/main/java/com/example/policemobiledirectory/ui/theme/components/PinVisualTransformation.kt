package com.example.policemobiledirectory.ui.theme.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PinVisualTransformation(
    private val maskChar: Char = '•'
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val maskedText = AnnotatedString(maskChar.toString().repeat(text.text.length))
        return TransformedText(maskedText, OffsetMapping.Identity)
    }
}