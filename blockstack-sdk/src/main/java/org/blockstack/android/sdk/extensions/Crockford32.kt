package org.blockstack.android.sdk.extensions

fun String.encodeCrockford32() : String {
    return toByteArray(Charsets.UTF_8).encodeCrockford32()
}

fun ByteArray.encodeCrockford32(): String {
    var i = 0
    var index = 0
    var digit: Int
    var currByte: Int
    var nextByte: Int
    val base32 = StringBuffer((size + 7) * 8 / 5)
    val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    while (i < size) {
        currByte = if (this[i] >= 0) this[i].toInt() else this[i] + 256

        if (index > 3) {
            nextByte = if (i + 1 < size) {
                if (this[i + 1] >= 0) this[i + 1].toInt() else this[i + 1] + 256
            } else {
                0
            }

            digit = currByte and (0xFF shr index)
            index = (index + 5) % 8
            digit = digit shl index
            digit = digit or (nextByte shr 8 - index)
            i++
        } else {
            digit = currByte shr 8 - (index + 5) and 0x1F
            index = (index + 5) % 8
            if (index == 0)
                i++
        }
        base32.append(alphabet[digit])
    }

    return base32.toString()
}

fun String.decodeCrockford32(): String {
    return String(decodeCrockford32ToByteArray(), Charsets.UTF_8)
}

fun String.decodeCrockford32ToByteArray(): ByteArray {
    return toByteArray(Charsets.UTF_8).decodeCrockford32ToByteArray()
}

fun ByteArray.decodeCrockford32ToByteArray(): ByteArray {
    if (size < 0) {
        return this
    }
    val buffer = ByteArray((size + 7) * 8 / 5)
    val mask8Bits = 0xff.toLong()

    val numberOfEncodedBitsPerByte = 5
    val numberOfBytesPerBlock = 8
    val pad = '='.toByte()

    var bitMaskWorkArea = 0L
    var encodedBlock = 0
    var currentPos = 0

    (0 until size).forEach { inPos ->
    val b = this[inPos]
        if (b == pad) {
            return@forEach
        } else if (b.isInCrockfordAlphabet()) {
            val result = b.toCrockford32AlphabetByte().toInt()
            encodedBlock = (encodedBlock + 1) % numberOfBytesPerBlock
            bitMaskWorkArea =
                (bitMaskWorkArea shl numberOfEncodedBitsPerByte) + result // collect decoded bytes
            if (encodedBlock == 0) { // we can output the 5 bytes
                buffer[currentPos++] = (bitMaskWorkArea shr 32 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 24 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 16 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 8 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea and mask8Bits).toByte()
            }
        }
    }

    if (encodedBlock >= 2) {
        when (encodedBlock) {
            2 -> buffer[currentPos++] = (bitMaskWorkArea shr 2 and mask8Bits).toByte()
            3 -> buffer[currentPos++] = (bitMaskWorkArea shr 7 and mask8Bits).toByte()
            4 -> {
                bitMaskWorkArea = bitMaskWorkArea shr 4 // drop 4 bits
                buffer[currentPos++] = (bitMaskWorkArea shr 8 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea and mask8Bits).toByte()
            }
            5 -> {
                bitMaskWorkArea = bitMaskWorkArea shr 1
                buffer[currentPos++] = (bitMaskWorkArea shr 16 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 8 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea and mask8Bits).toByte()
            }
            6 -> {
                bitMaskWorkArea = bitMaskWorkArea shr 6
                buffer[currentPos++] = (bitMaskWorkArea shr 16 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 8 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea and mask8Bits).toByte()
            }
            7 -> {
                bitMaskWorkArea = bitMaskWorkArea shr 3
                buffer[currentPos++] = (bitMaskWorkArea shr 24 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 16 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea shr 8 and mask8Bits).toByte()
                buffer[currentPos++] = (bitMaskWorkArea and mask8Bits).toByte()
            }
        }
    }

    val result = ByteArray(currentPos)
    System.arraycopy(buffer, 0, result, 0, currentPos)

    return result
}

fun Byte.isInCrockfordAlphabet(): Boolean {
    return toCrockford32AlphabetByte().toInt() != -1
}

fun Byte.toCrockford32AlphabetByte(): Byte {
    return when (toChar()) {
        '0', 'O', 'o' -> 0
        '1', 'I', 'i', 'L', 'l' -> 1
        '2' -> 2
        '3' -> 3
        '4' -> 4
        '5' -> 5
        '6' -> 6
        '7' -> 7
        '8' -> 8
        '9' -> 9
        'A', 'a' -> 10
        'B', 'b' -> 11
        'C', 'c' -> 12
        'D', 'd' -> 13
        'E', 'e' -> 14
        'F', 'f' -> 15
        'G', 'g' -> 16
        'H', 'h' -> 17
        'J', 'j' -> 18
        'K', 'k' -> 19
        'M', 'm' -> 20
        'N', 'n' -> 21
        'P', 'p' -> 22
        'Q', 'q' -> 23
        'R', 'r' -> 24
        'S', 's' -> 25
        'T', 't' -> 26
        'U', 'u', 'V', 'v' -> 27
        'W', 'w' -> 28
        'X', 'x' -> 29
        'Y', 'y' -> 30
        'Z', 'z' -> 31
        else -> -1
    }
}