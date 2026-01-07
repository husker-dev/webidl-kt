package com.huskerdev.webidl.jvm

import java.io.File
import java.io.InputStream


class InputStreamIterator(
    stream: InputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Iterator<Char> {

    constructor(
        file: File,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): this(file.inputStream(), bufferSize)

    private val reader = stream.buffered(bufferSize)
    private var nextChar = stream.read()

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

fun InputStream.iterator(
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) = InputStreamIterator(this, bufferSize)