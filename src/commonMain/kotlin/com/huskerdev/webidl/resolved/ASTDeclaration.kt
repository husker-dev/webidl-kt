package com.huskerdev.webidl.resolved

sealed interface WebIDLDeclaration {
    val nullable: Boolean
}

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

    var implements: WebIDLInterface? = null

    val fields = arrayListOf<WebIDLField>()

    val staticFields = arrayListOf<WebIDLField>()

    val functions = arrayListOf<WebIDLFunction>()

    val staticFunctions = arrayListOf<WebIDLFunction>()

    val constructors = arrayListOf<WebIDLConstructor>()


    var isIterable: Boolean = false
        private set

    var isAsyncIterable: Boolean = false
        private set

    var isMap: Boolean = false
        private set

    var isSet: Boolean = false
        private set

    var iterableType: Pair<WebIDLType, WebIDLType?>? = null
        set(value) {
            field = value
            isIterable = value != null
        }

    var asyncIterableType: Pair<WebIDLType, WebIDLType?>? = null
        set(value) {
            field = value
            isAsyncIterable = value != null
        }

    var mapType: Pair<WebIDLType, WebIDLType>? = null
        set(value) {
            field = value
            isMap = value != null
        }

    var setType: WebIDLType? = null
        set(value) {
            field = value
            isSet = value != null
        }

    fun applyMixin(mixin: WebIDLInterface){
        fields += mixin.fields
        functions += mixin.functions
        iterableType = mixin.iterableType
        asyncIterableType = mixin.asyncIterableType
        mapType = mixin.mapType
        setType = mixin.setType
    }
}

class WebIDLDictionary(
    val name: String
): WebIDLDeclaration {
    override val nullable = true

    val fields = arrayListOf<WebIDLField>()
}

class WebIDLEnum(
    val name: String
): WebIDLDeclaration {
    override val nullable = false
}

class WebIDLTypeDef(
    val name: String,
    val source: com.huskerdev.webidl.parser.IdlType
): WebIDLDeclaration {
    lateinit var linked: WebIDLType
        private set

    override var nullable = false
        private set

    fun resolve(idl: WebIDLAST) {
        linked = idl.findType(source)
        nullable = linked.nullable
    }
}

class WebIDLCallback(
    val name: String,
): WebIDLDeclaration {
    override val nullable = false

    lateinit var type: WebIDLType
    lateinit var args: List<WebIDLField>
}

class WebIDLNamespace(
    val name: String
): WebIDLDeclaration {
    override val nullable: Boolean = false

    val fields = arrayListOf<WebIDLField>()

    val functions = arrayListOf<WebIDLFunction>()
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
