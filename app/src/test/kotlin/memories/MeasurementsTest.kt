package memories

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class MeasurementsTest: DescribeSpec({

    // This is commonly 12 bytes and aligned to 4 or 8 bytes (32/64 bit JVM). So using 32 should be plenty here.
    val arrayHeaderPoop = 32

    describe("Memory") {
        it("Measures about four bytes per int") {
            val arraySize = 8192
            val usedMemory = measureMemory {
                IntArray(arraySize)
            }
            usedMemory shouldBeInRange (Int.SIZE_BYTES * arraySize.toLong() .. arrayHeaderPoop + Int.SIZE_BYTES * arraySize)
        }

        it("Measures about byte per byte") {
            val arraySize = 8192
            val usedMemory = measureMemory {
                ByteArray(arraySize)
            }
            usedMemory shouldBeInRange (Byte.SIZE_BYTES * arraySize.toLong() .. arrayHeaderPoop + Byte.SIZE_BYTES * arraySize)
        }

        it("Measures about eight bytes per long") {
            val arraySize = 8192
            val usedMemory = measureMemory {
                LongArray(arraySize)
            }
            usedMemory shouldBeInRange (Long.SIZE_BYTES * arraySize.toLong() .. arrayHeaderPoop + Long.SIZE_BYTES * arraySize)
        }
    }

    describe("CPU time") {
        it("Measures non-zero time for a loop") {
            val (res, measurement) = measure {
                var sum = 0
                for (i in (0..1000_000 + Random.nextInt(500))) {
                    sum += i
                }
                sum
            }
            SimpleBlackHole.consume(res)
            val isPositive = measurement.getCPU() > 0
            isPositive shouldBe true
        }
    }

})