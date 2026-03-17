package com.huskerdev.webidl.parser

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline


@Serializable
sealed interface IdlValue {

    @Serializable
    object NullValue: IdlValue

    @Serializable
    object DictionaryInitValue: IdlValue

    @JvmInline
    @Serializable
    value class StringValue(val text: String): IdlValue

    @JvmInline
    @Serializable
    value class BooleanValue(val boolValue: Boolean): IdlValue

    @JvmInline
    @Serializable
    value class IntValue(
        val text: String,
    ): IdlValue {
        val number: Int get() = when {
            "0x" in text.lowercase() -> text.drop(2).toInt(16)
            "0o" in text.lowercase() -> text.drop(2).toInt(8)
            "0b" in text.lowercase() -> text.drop(2).toInt(2)
            else -> text.toInt()
        }
    }

    @JvmInline
    @Serializable
    value class DecimalValue(
        val text: String
    ): IdlValue {
        val number: Double get() = text.toDouble()
    }

}






