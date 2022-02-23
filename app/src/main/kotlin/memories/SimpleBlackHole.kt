package memories

import java.util.concurrent.atomic.AtomicInteger

internal object SimpleBlackHole {

    private val hash = AtomicInteger(0)

    fun getValue() = hash.get()

    private fun consume(v: Int) {
        hash.getAndUpdate { old -> v.xor(old) }
    }

    private fun consumeNonObjectArray(arr: Any) {
        val hsh = when (arr) {
            is ByteArray -> arr.contentHashCode()
            is CharArray -> arr.contentHashCode()
            is ShortArray -> arr.contentHashCode()
            is IntArray -> arr.contentHashCode()
            is LongArray -> arr.contentHashCode()
            is FloatArray -> arr.contentHashCode()
            is DoubleArray -> arr.contentHashCode()
            else -> arr.hashCode()
        }
        consume(hsh)
    }

    /**
     * Will loop forever for self-referential arrays.
     */
    fun consume(v: Any?) {
        if (v == null) {
            return
        }
        if (v is Array<*>) {
            v.forEach { consume(it) }
        } else if (v::class.java.componentType()?.isPrimitive == true) {
            consumeNonObjectArray(v)
        } else {
            consume(v.hashCode())
        }
    }

}
