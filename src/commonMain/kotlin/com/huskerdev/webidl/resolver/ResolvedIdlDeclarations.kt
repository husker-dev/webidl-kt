package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.parser.IdlAttributedHolder
import com.huskerdev.webidl.parser.IdlExtendedAttribute

sealed interface ResolvedIdlDeclaration: IdlAttributedHolder {
    val name: String
    val nullable: Boolean
}

class BuiltinIdlDeclaration(
    override val name: String,
    val kind: WebIDLBuiltinKind
): ResolvedIdlDeclaration {
    override val attributes: List<IdlExtendedAttribute> = emptyList()

    override val nullable = kind.nullable
}

class ResolvedIdlInterface(
    override val name: String,
    val isCallback: Boolean,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    override val nullable = true

    var implements: ResolvedIdlInterface? = null

    val fields = arrayListOf<ResolvedIdlField.Declaration>()

    val staticFields = arrayListOf<ResolvedIdlField.Declaration>()

    val operations = arrayListOf<ResolvedIdlOperation>()

    val staticOperations = arrayListOf<ResolvedIdlOperation>()

    val constructors = arrayListOf<ResolvedIdlConstructor>()

    var getter: ResolvedIdlOperation? = null

    var setter: ResolvedIdlOperation? = null


    var stringifierEnabled = false

    var stringifier: ResolvedIdlField.Declaration? = null


    var isIterable: Boolean = false

    var iterableType: Pair<ResolvedIdlType, ResolvedIdlType?>? = null


    var isAsyncIterable: Boolean = false

    var asyncIterableType: Pair<ResolvedIdlType, ResolvedIdlType?>? = null


    var isMap: Boolean = false
    var isReadOnlyMap: Boolean = false

    var mapType: Pair<ResolvedIdlType, ResolvedIdlType>? = null


    var isSet: Boolean = false
    var isReadOnlySet: Boolean = false

    var setType: ResolvedIdlType? = null

    fun applyMixin(mixin: ResolvedIdlInterface){
        fields += mixin.fields
        operations += mixin.operations
        iterableType = mixin.iterableType
        asyncIterableType = mixin.asyncIterableType
        mapType = mixin.mapType
        setType = mixin.setType
    }
}

class ResolvedIdlDictionary(
    override val name: String,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    override val nullable = true

    var implements: ResolvedIdlDictionary? = null

    val fields = arrayListOf<ResolvedIdlField.Declaration>()
}

class ResolvedIdlEnum(
    override val name: String,
    val elements: List<String>,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    override val nullable = false
}

class ResolvedIdlTypeDef(
    override val name: String,
    private var parserType: com.huskerdev.webidl.parser.IdlType?,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    lateinit var type: ResolvedIdlType
        private set

    override var nullable = false
        private set

    fun resolve(idl: IdlResolver) {
        type = idl.findType(parserType!!)
        parserType = null
        nullable = type.isNullable
    }
}

class ResolvedIdlCallbackFunction(
    override val name: String,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    override val nullable = false

    lateinit var type: ResolvedIdlType
    lateinit var args: List<ResolvedIdlField.Argument>
}

class ResolvedIdlNamespace(
    override val name: String,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlDeclaration {
    override val nullable: Boolean = false

    val fields = arrayListOf<ResolvedIdlField.Declaration>()

    val operations = arrayListOf<ResolvedIdlOperation>()
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
