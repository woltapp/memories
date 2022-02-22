package memories

import com.sun.management.HotSpotDiagnosticMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object HeapDumper {

    private val dumper = run {
        val server = ManagementFactory.getPlatformMBeanServer()
        val mxBean: HotSpotDiagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(
            server,
            "com.sun.management:type=HotSpotDiagnostic",
            HotSpotDiagnosticMXBean::class.java
        )
        mxBean
    }

    /**
     *
     */
    fun dumpHeap(directory: File, liveOnly: Boolean = true): File? {
        if (directory.isDirectory) {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_hhmmssSSS", Locale.ENGLISH)
            val datetimeString = formatter.format(LocalDateTime.now(ZoneId.of("UTC")))
            val file = File(directory, "heapdump_$datetimeString.hprof")
            dumper.dumpHeap(file.absolutePath, liveOnly)
            return file
        }
        return null
    }
}