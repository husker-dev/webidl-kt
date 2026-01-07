package com.huskerdev.webidl.parser

interface WebIDLParserConsumer {

    fun enter(definition: WebIDLDefinition)
    fun exit()

    fun consume(definition: WebIDLDefinition) {
        enter(definition)
        exit()
    }

    class Collector: WebIDLParserConsumer {
        lateinit var root: WebIDLDefinitionRoot
            private set

        private val stack = arrayListOf<WebIDLDefinition>()

        override fun enter(definition: WebIDLDefinition) {
            if(definition is WebIDLDefinitionRoot)
                root = definition

            when(val container = stack.lastOrNull()) {
                is WebIDLEnumDef -> {
                    container.definitions += definition as? WebIDLEnumElementDef
                        ?: throw UnsupportedOperationException()
                }
                is WebIDLSimpleDefinitionContainer -> {
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