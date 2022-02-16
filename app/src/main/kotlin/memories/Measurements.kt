package memories

import java.lang.management.ManagementFactory

object MBeans {

    val threads = ManagementFactory.getThreadMXBean()
    val sunThreads = (threads as? com.sun.management.ThreadMXBean)

    fun currentThreadAllocations(): Long {
        return sunThreads?.currentThreadAllocatedBytes ?: -1
    }

    init {
        threads.isThreadCpuTimeEnabled = true
    }

}

@JvmInline
value class HeapAndCPU(private val both: Long) {

    /**
     * In nanoseconds.
     */
    fun getCPU(): Long = both.and(0xFFFFFFFFL)

    /**
     * In bytes.
     */
    fun getMemory(): Long = both.shr(Integer.SIZE)

    fun hasMemory(): Boolean = getMemory() >= 0L

    companion object {
        fun createFrom(cpuTime: Long, memoryUsage: Long): HeapAndCPU {
            check(cpuTime in (Integer.MIN_VALUE..Integer.MAX_VALUE) && memoryUsage in (Integer.MIN_VALUE..Integer.MAX_VALUE))
            return HeapAndCPU(cpuTime.and(0xFFFFFFFFL).or(memoryUsage.shl(Integer.SIZE)))
        }
    }
}

/**
 * In nanoseconds.
 */
inline fun measureCPU(f: () -> Unit): Long {
    return measure(f).second.getCPU()
}

/**
 * In bytes.
 */
inline fun measureMemory(f: () -> Unit): Long {
    return measure(f).second.getMemory()
}

const val REPEATS: Int = 1

inline fun <T> measure(f: () -> T): Pair<T, HeapAndCPU> {
    var smallestCPU = Long.MAX_VALUE
    var smallestMemory = Long.MAX_VALUE
    var res: T? = null
    for (i in 0 until REPEATS) {
        val start = MBeans.threads.currentThreadCpuTime
        val startAllocations = MBeans.currentThreadAllocations()
        res = f()
        val timeUsed = MBeans.threads.currentThreadCpuTime - start
        val memoryUsed = MBeans.currentThreadAllocations() - startAllocations
        if (smallestCPU > timeUsed) smallestCPU = timeUsed
        if (smallestMemory > memoryUsed) smallestMemory = memoryUsed
    }
    return res!! to HeapAndCPU.createFrom(smallestCPU, smallestMemory)
}

