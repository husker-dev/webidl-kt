<img width="100" src="./.github/icon.webp" alt="logo">

#  webidl-kt

Small and optimized Kotlin Multipaltform library for parsing [WebIDL](https://webidl.spec.whatwg.org/). 

Supports streaming and type customization.

## Dependency

```kotlin
dependencies {
    implementation("com.huskerdev:webidl-kt:1.0.0")
}
```

## Definition parsing

The simplest way is to parse text into a definitions AST.

It ignores type incompatibilities and missing identifiers, but uses some basic syntax checks.

```kotlin
val root = WebIDL.parseDefinitions("""
    [Exposed=Window]
    interface TestInterface {
        attribute long a;
    };
""".trimIndent())
```

## Definition streaming
  
It is possible to "stream" definitions as they are parsed, without storing them in memory. 
This can be effective when converting "on the fly".

```kotlin
WebIDL.streamDefinitions("""
    [Exposed=Window]
    interface TestInterface {
        attribute long a;
    };
""".trimIndent(), object: WebIDLParserConsumer {
    override fun enter(definition: WebIDLDefinition) {
        // Definition opened
    }
    override fun exit() {
        // Definition closed
    }
})
```

## Source streaming

Constructor can also accept an `Iterator<Char>`.

So it is possible to work with a data stream (for example, a file).
  
> For working with files, the JVM part of this library has a special helper class that converts an `Reader` to an `Iterator<Char>`

```kotlin
val iterator = File("someFile.idl").bufferedReader().iterator()

// parse definitions
val root = WebIDL.parseDefinitions(iterator)

// or stream
WebIDL.streamDefinitions(iterator, object: WebIDLParserConsumer {
    override fun enter(definition: WebIDLDefinition) {
        // Definition opened
    }
    override fun exit() {
        // Definition closed
    }
})
```
