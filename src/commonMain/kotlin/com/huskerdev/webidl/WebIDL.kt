package com.huskerdev.webidl

import com.huskerdev.webidl.parser.IdlParser
import com.huskerdev.webidl.parser.IdlParserConsumer
import com.huskerdev.webidl.resolver.IdlResolver
import kotlinx.io.RawSource
import kotlinx.io.buffered
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

        @JvmStatic
        fun streamDefinitions(
            rawSource: RawSource,
            consumer: IdlParserConsumer,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
        ) = IdlParser(rawSource.asCharIterator(), consumer, types).parse()

        // Parse all definitions

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<Char>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().apply {
            streamDefinitions(iterator, this, types)
        }.root

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().apply {
            streamDefinitions(text, this, types)
        }.root

        @JvmStatic
        fun parseDefinitions(
            rawSource: RawSource,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = IdlParserConsumer.Collector().apply {
            streamDefinitions(rawSource, this, types)
        }.root

        // Resolve all definitions

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

        @JvmStatic
        fun resolve(
            rawSource: RawSource,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = IdlResolver(parseDefinitions(rawSource, env.builtinTypes.keys), env)
    }
}

private fun RawSource.asCharIterator() = object: Iterator<Char> {
    private val source = buffered()

    override fun next(): Char =
        source.readInt().toChar()

    override fun hasNext(): Boolean =
        !source.exhausted()
}