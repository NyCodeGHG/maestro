package maestro.recording

import maestro.Maestro
import maestro.drivers.AndroidDriver
import okio.Sink
import java.nio.file.Path
import java.util.concurrent.Future

interface AndroidScreenRecorder {
    /**
     * Checks if the screen recorder implementation is available on the system.
     * Possible check use cases:
     * - check for external binaries
     * - check for android version
     */
    fun isAvailable(): Availability

    fun record(path: Path): Future<Void>

    fun stop()
}
