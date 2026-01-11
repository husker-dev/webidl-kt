package com.huskerdev.webidl.parser

interface IdlAttributedHolder: IdlDefinition {
    val attributes: List<IdlExtendedAttribute>
}