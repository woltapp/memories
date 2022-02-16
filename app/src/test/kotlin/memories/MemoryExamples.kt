package memories

import org.junit.jupiter.api.Test


class MemoryExamples {

    @Test
    fun `List of 8192 Longs allocates`() {
        val usedMemory = measureMemory {
            List(8192) {
                (Int.MAX_VALUE + 1L)
            }
        }
        println(usedMemory)
    }

    @Test
    fun `Array of 8192 Ints allocates`() {
        val usedMemory = measureMemory {
            IntArray(8192)
        }
        println(usedMemory)

    }

}