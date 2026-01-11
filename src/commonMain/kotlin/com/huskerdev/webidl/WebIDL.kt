package com.huskerdev.webidl

import com.huskerdev.webidl.resolver.IdlResolver
import com.huskerdev.webidl.parser.IdlParser
import com.huskerdev.webidl.parser.IdlParserConsumer
import kotlin.jvm.JvmStatic

@Suppress("unused")
class WebIDL {
    companion object {

        @JvmStatic
        fun streamDefinitions(
            iterator: Iterator<Char>,
            consumer: IdlParserConsumer,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
        ) = IdlParser(iterator, consumer, types).parse()

        @JvmStatic
        fun streamDefinitions(
            text: String,
            consumer: IdlParserConsumer,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
        ) = IdlParser(text.asSequence().iterator(), consumer, types).parse()

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<Char>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().run {
            IdlParser(iterator, this, types).parse()
            root
        }

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().run {
            IdlParser(text.asSequence().iterator(), this, types).parse()
            root
        }

        @JvmStatic
        fun resolve(
            iterable: Iterator<Char>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = IdlResolver(parseDefinitions(iterable, env.builtinTypes.keys), env)

        @JvmStatic
        fun resolve(
            text: String,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = IdlResolver(parseDefinitions(text, env.builtinTypes.keys), env)

    }
}