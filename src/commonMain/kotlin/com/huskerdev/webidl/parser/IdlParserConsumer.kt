package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDLSyntaxErrorException

interface IdlParserConsumer {

    fun enter(definition: IdlDefinition)
    fun exit()
    fun error(exception: WebIDLSyntaxErrorException)

    class Collector: IdlParserConsumer {
        private val stack = arrayListOf<IdlDefinition>()
        private val collectedErrors = arrayListOf<WebIDLSyntaxErrorException>()

        lateinit var root: IdlRoot
            private set

        val errors: List<WebIDLSyntaxErrorException> = collectedErrors

        override fun enter(definition: IdlDefinition) {
            if(definition is IdlRoot)
                root = definition

            when(val container = stack.lastOrNull()) {
                is IdlEnum -> container.elements.add(definition as IdlEnumElement)
                is IdlContainer -> container.definitions.add(definition)
                else -> Unit
            }

            stack += definition
        }

        override fun exit() {
            stack.removeLastOrNull()
        }

        override fun error(exception: WebIDLSyntaxErrorException) {
            collectedErrors += exception
        }
    }
}

internal fun IdlParserConsumer.consume(definition: IdlDefinition) {
    enter(definition)
    exit()
}