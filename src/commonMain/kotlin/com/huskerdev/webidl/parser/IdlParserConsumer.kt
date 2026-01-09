package com.huskerdev.webidl.parser

interface IdlParserConsumer {

    fun enter(definition: IdlDefinition)
    fun exit()

    fun consume(definition: IdlDefinition) {
        enter(definition)
        exit()
    }

    class Collector: IdlParserConsumer {
        lateinit var root: IdlDefinitionRoot
            private set

        private val stack = arrayListOf<IdlDefinition>()

        override fun enter(definition: IdlDefinition) {
            if(definition is IdlDefinitionRoot)
                root = definition

            when(val container = stack.lastOrNull()) {
                is IdlEnum -> {
                    container.definitions += definition as? IdlEnumElement
                        ?: throw UnsupportedOperationException()
                }
                is IdlDefaultDefinitionContainer -> {
                    container.definitions += definition
                }
                else -> Unit
            }

            stack += definition
        }

        override fun exit() {
            stack.removeLast()
        }
    }
}