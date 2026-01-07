package com.huskerdev.webidl

import com.huskerdev.webidl.ast.WebIDLAST
import com.huskerdev.webidl.parser.WebIDLParser
import com.huskerdev.webidl.parser.WebIDLParserConsumer
import kotlin.jvm.JvmStatic

@Suppress("unused")
class WebIDL {
    companion object {

        @JvmStatic
        fun streamDefinitions(
            iterator: Iterator<Char>,
            consumer: WebIDLParserConsumer,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
        ) = WebIDLParser(iterator, consumer, types).parse()

        @JvmStatic
        fun streamDefinitions(
            text: String,
            consumer: WebIDLParserConsumer,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
        ) = WebIDLParser(text.asSequence().iterator(), consumer, types).parse()

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<Char>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParserConsumer.Collector().run {
            WebIDLParser(iterator, this, types).parse()
            root
        }

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParserConsumer.Collector().run {
            WebIDLParser(text.asSequence().iterator(), this, types).parse()
            root
        }

        @JvmStatic
        fun parseAST(
            iterable: Iterator<Char>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(iterable, env.builtinTypes.keys), env)

        @JvmStatic
        fun parseAST(
            text: String,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(text, env.builtinTypes.keys), env)

    }
}