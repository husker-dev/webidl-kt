package com.huskerdev.webidl

import com.huskerdev.webidl.ast.WebIDLAST
import com.huskerdev.webidl.parser.WebIDLParser
import kotlin.jvm.JvmStatic

@Suppress("unused")
class WebIDL {
    companion object {

        @JvmStatic
        fun parseDefinitions(
            iterator: Iterator<Char>,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(iterator, types).parse()

        @JvmStatic
        fun parseDefinitions(
            text: String,
            types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
        ) = WebIDLParser(text.asSequence().iterator(), types).parse()


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