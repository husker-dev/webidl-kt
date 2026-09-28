package com.huskerdev.webidl.parser

import com.huskerdev.webidl.IdlElementBounds
import kotlinx.serialization.Serializable


@Serializable
sealed interface IdlValue {
    val bounds: IdlElementBounds

    @Serializable
    object NullValue: IdlValue {
        override val bounds = IdlElementBounds()
    }

    @Serializable
    object DictionaryInitValue: IdlValue {
        override val bounds = IdlElementBounds()
    }

    @Serializable
    class StringValue(
        val text: String,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): IdlValue

    @Serializable
    class BooleanValue(
        val boolValue: Boolean,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): IdlValue

    @Serializable
    class IntValue(
        val text: String,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): IdlValue {
        val number: Int get() = when {
            "0x" in text.lowercase() -> text.drop(2).toInt(16)
            "0o" in text.lowercase() -> text.drop(2).toInt(8)
            "0b" in text.lowercase() -> text.drop(2).toInt(2)
            else -> text.toInt()
        }
    }

    @Serializable
    class DecimalValue(
        val text: String,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): IdlValue {
        val number: Double get() = text.toDouble()
    }

}






