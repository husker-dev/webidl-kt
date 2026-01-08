package com.huskerdev.webidl.jvm

import java.io.File
import java.io.Reader
import java.nio.charset.Charset


class CharIterator(
    val reader: Reader
): Iterator<Char> {

    @Suppress("unused")
    constructor(
        file: File,
        charset: Charset = Charsets.UTF_8,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): this(file.bufferedReader(charset, bufferSize))

    private var nextChar = reader.read()

    override fun hasNext(): Boolean = nextChar != -1

    override fun next(): Char {
        if (!hasNext()) throw NoSuchElementException()

        return nextChar.toChar().also {
            nextChar = reader.read()
            if (nextChar == -1)
                reader.close()
        }
    }
}

fun Reader.iterator() = CharIterator(this)