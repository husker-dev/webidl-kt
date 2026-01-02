package com.huskerdev.webidl.ast

import com.huskerdev.webidl.WebIDL

sealed interface WebIDLDeclaration {
    val nullable: Boolean
}

class WebIDLUnionType(
    val types: List<WebIDLDeclaration>,
    override val nullable: Boolean
): WebIDLDeclaration

class WebIDLBuiltin(
    val name: String,
    val kind: WebIDLBuiltinKind
): WebIDLDeclaration {
    override val nullable = kind.nullable
}

class WebIDLInterface(
    val name: String,
    val isCallback: Boolean,
): WebIDLDeclaration {
    override val nullable = true

    var isIterable: Boolean = false
        private set

    var isAsyncIterable: Boolean = false
        private set

    var isMap: Boolean = false
        private set

    var isSet: Boolean = false
        private set

    var iterableType: WebIDLDeclaration? = null
        set(value) {
            field = value
            isIterable = value != null
        }

    var asyncIterableType: WebIDLDeclaration? = null
        set(value) {
            field = value
            isAsyncIterable = value != null
        }

    var mapType: Map.Entry<WebIDLDeclaration, WebIDLDeclaration>? = null
        set(value) {
            field = value
            isMap = value != null
        }

    var setType: WebIDLDeclaration? = null
        set(value) {
            field = value
            isSet = value != null
        }
}

class WebIDLDictionary(
    val name: String
): WebIDLDeclaration {
    override val nullable = true
}

class WebIDLEnum(
    val name: String
): WebIDLDeclaration {
    override val nullable = false
}

class WebIDLTypeDef(
    val name: String
): WebIDLDeclaration {
    lateinit var linked: WebIDLDeclaration
        private set

    override val nullable = linked.nullable

    fun resolve(idl: WebIDL) {
        linked = idl.findDeclaration(name)
    }
}


enum class WebIDLBuiltinKind(
    val nullable: Boolean = false,
    val types: Int = 0
) {
    ANY(nullable = true),     // Any type
    OBJECT(nullable = true),  // Only objects (without: void, numbers, strings, boolean, symbol, bigint)
    VOID,

    LIST(nullable = true, types = 1),
    MUTABLE_LIST(nullable = true, types = 1),
    MAP(nullable = true, types = 2),
    PROMISE(types = 1),

    BOOLEAN,
    STRING,
    USV_STRING, // URL-safe string
    CHAR,

    INT,
    UNSIGNED_INT,
    BIG_INT,

    FLOAT,
    UNRESTRICTED_FLOAT,

    DOUBLE,
    UNRESTRICTED_DOUBLE,

    BYTE,
    UNSIGNED_BYTE,
    BYTE_SEQUENCE,

    SHORT,
    UNSIGNED_SHORT,

    LONG,
    UNSIGNED_LONG,
}
