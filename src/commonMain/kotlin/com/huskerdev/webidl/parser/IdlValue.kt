package com.huskerdev.webidl.parser

import kotlin.jvm.JvmInline


sealed interface IdlValue {

    object NullValue: IdlValue

    object DictionaryInitValue: IdlValue

    @JvmInline
    value class StringValue(val text: String): IdlValue

    @JvmInline
    value class BooleanValue(val boolValue: Boolean): IdlValue

    @JvmInline
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
    value class DecimalValue(
        val text: String
    ): IdlValue {
        val number: Double get() = text.toDouble()
    }

}






