/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package memories

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class App {
    val greeting: String
        get() {
            return "Hello World!"
        }
}

private val cpuCount = Runtime.getRuntime().availableProcessors()

fun main(args: Array<String>) {
    val allocators = setup()
    waitForStop(allocators)
}


fun setup(): List<Allocator> {
    return (1..8 * cpuCount).map {
        createShortLivedObjectsAllocator()
    } + (1 .. cpuCount).map {
        createLongLivedObjectsAllocator()
    }
}

fun waitForStop(stuffToWait: List<Allocator>) {
    try {
        Thread.sleep(Integer.MAX_VALUE.toLong())
        println(SimpleBlackHole.getValue())
    } catch (_: InterruptedException) {
    }
    stuffToWait.forEach { it.stop() }
    stuffToWait.forEach { it.join() }
}

private val counter = AtomicInteger(0)

class Allocator(private val allocateLoop: () -> Unit) {

    private val stop = AtomicBoolean(false)
    private val thread: Thread

    init {
        val threadName = "Allocator-thread-${counter.incrementAndGet()}"
        thread = Thread {
            while (!stop.get()) {
                allocateLoop()
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {

                }
            }
        }.apply {
            name = threadName
            isDaemon = true
            start()
        }
    }

    fun stop() {
        stop.set(true)
    }

    fun join() {
        stop()
        thread.join()
    }
}

private val smallArraySize = 23
private val bigArraySize = 63233

fun createShortLivedObjectsAllocator(): Allocator {
    return Allocator {
        val bytes = ByteArray(bigArraySize)
        ThreadLocalRandom.current().nextBytes(bytes)
        SimpleBlackHole.consume(bytes)
    }
}

fun createLongLivedObjectsAllocator(): Allocator {
    val treeLevelSize = 32
    val releaseCycle = 64
    val initializer: (Int) -> Array<ByteArray?> = {
        Array(treeLevelSize) {
            null
        }
    }
    val arr: Array<Array<Array<Array<ByteArray?>>>> = Array(treeLevelSize) {
        Array(treeLevelSize) {
            Array(treeLevelSize, initializer)
        }
    }
    var loopCounter = 0

    fun forRootArrays(arr: Array<Array<Array<Array<ByteArray?>>>>, arrConsumer: (Array<Array<ByteArray?>>) -> Unit) {
        arr.forEach {
            it.forEach { someArrays ->
                arrConsumer(someArrays)
            }
        }
    }

    return Allocator {
        forRootArrays(arr) {
            it.forEach { arrayOfByteArrays ->
                arrayOfByteArrays.forEachIndexed { index, _ ->
                    val bytes = ByteArray(smallArraySize)
                    ThreadLocalRandom.current().nextBytes(bytes)
                    arrayOfByteArrays[index] = bytes
                }
            }
        }
        forRootArrays(arr) {
            SimpleBlackHole.consume(it)
        }
        if (loopCounter++ == releaseCycle) {
            loopCounter = 0
            forRootArrays(arr) {
                it.forEachIndexed { index, _ ->
                    it[index] = initializer(index)
                }
            }
        }
    }
}
