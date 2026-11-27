package com.huskerdev.webidl

import com.huskerdev.webidl.parser.IdlParser
import com.huskerdev.webidl.parser.IdlParserConsumer
import com.huskerdev.webidl.resolver.IdlResolver
import kotlin.jvm.JvmStatic

@Suppress("unused")
class WebIDL {
    companion object {

        // Stream definitions

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

        // Parse all definitions

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<Char>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().run {
            streamDefinitions(iterator, this, types)
            root to errors
        }

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().run {
            streamDefinitions(text, this, types)
            root to errors
        }

        // Resolve all definitions

        @JvmStatic
        fun resolve(
            iterable: Iterator<Char>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = parseDefinitions(iterable, env.builtinTypes.keys).run {
            IdlResolver(first, second, env)
        }

        @JvmStatic
        fun resolve(
            text: String,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = parseDefinitions(text, env.builtinTypes.keys).run {
            IdlResolver(first, second, env)
        }
    }
}