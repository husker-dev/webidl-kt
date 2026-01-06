package com.huskerdev.webidl

import com.huskerdev.webidl.ast.WebIDLAST
import com.huskerdev.webidl.parser.WebIDLParser
import kotlin.jvm.JvmStatic

@Suppress("unused")
class WebIDL {
    companion object {

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<String>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(iterator, types).parse()

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(listOf(text).iterator(), types).parse()

        @JvmStatic
        fun parseDefinitions(
            lineSequence: Sequence<String>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(lineSequence.iterator(), types).parse()

        @JvmStatic
        fun parseDefinitions(
            lines: Iterable<String>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(lines.iterator(), types).parse()


        @JvmStatic
        fun parseAST(
            iterable: Iterator<String>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(iterable, env.builtinTypes.keys), env)

        @JvmStatic
        fun parseAST(
            text: String,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(text, env.builtinTypes.keys), env)

        @JvmStatic
        fun parseAST(
            lineSequence: Sequence<String>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(lineSequence, env.builtinTypes.keys), env)

        @JvmStatic
        fun parseAST(
            lines: Iterable<String>,
            env: WebIDLEnv = WebIDLEnv.Default
        ) = WebIDLAST(parseDefinitions(lines, env.builtinTypes.keys), env)
    }
}