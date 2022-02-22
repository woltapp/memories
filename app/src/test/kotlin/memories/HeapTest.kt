package memories

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import java.nio.file.Files

class HeapTest: DescribeSpec({

    val tempDir = Files.createTempDirectory("heapdumptest").toFile()

    afterTest {
        tempDir.deleteRecursively()
    }

    it("Dumps something to the disk") {
        val file = HeapDumper.dumpHeap(tempDir, false).shouldNotBeNull()
        file.deleteOnExit()
        file.length().shouldBeGreaterThan(0L)
    }
})