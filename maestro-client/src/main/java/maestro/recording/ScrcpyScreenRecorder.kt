package maestro.recording

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.*

/**
 * Screen recorder implementation which utilizes [scrcpy](https://github.com/Genymobile/scrcpy/)
 * for recording the screen.
 */
class ScrcpyScreenRecorder : AndroidScreenRecorder {

    private val logger = LoggerFactory.getLogger(ScrcpyScreenRecorder::class.java)

    private var stop = false

    override fun isAvailable(): Availability {
        return when (val result = discoverScrcpyBinary()) {
            is Ok -> Availability.Available
            is Err -> result.error
        }
    }

    override fun record(path: Path): Future<Void> {
        val binary = when (val result = discoverScrcpyBinary()) {
            is Err -> error("Scrcpy is not available.")
            is Ok -> result.value
        }
        logger.debug("Using scrcpy binary {}", binary)

        val builder = ProcessBuilder().command(
            binary.pathString,
            "--no-playback",
            "--record=${path.absolutePathString()}",
            "--record-format=mp4",
        )
            .redirectError(Redirect.INHERIT)
            .redirectOutput(Redirect.INHERIT)
            .redirectInput(File("/dev/null"))
        logger.debug("Starting scrcpy with command line: ${builder.command().joinToString(" ")}")
        val process = builder.start()

        when (val result = runCatching {
            CompletableFuture.runAsync {
                process.errorStream
                    .bufferedReader()
                    .lineSequence()
                    .find { it.contains("Recording started to mp4 file") }
            }.get(5, TimeUnit.SECONDS)
        }) {
            is Ok -> logger.debug("Recording started.")
            is Err -> when (result.error) {
                is TimeoutException -> logger.error("Recording didn't start after 5 seconds", result.error)
                else -> throw IllegalStateException(result.error)
            }
        }

        val future = CompletableFuture.runAsync {
            while (!stop && process.isAlive) {
                Thread.sleep(100)
            }
            if (!process.isAlive) {
                logger.warn("Recording scrcpy is not alive anymore.")
            }
            logger.debug("Stopping scrcpy recording.")
            process.destroy()
        }
        return future
    }

    override fun stop() {
        logger.debug("Triggered stop signal.")
        stop = true
    }

    private fun discoverScrcpyBinary(): Result<Path, Availability.Unavailable> {
        val env = System.getenv("MAESTRO_SCRCPY")
        // prefer environment variable over PATH
        if (env != null) {
            val path = Path(env)
            if (path.notExists()) {
                return Err(Availability.Unavailable("The environment variable `MAESTRO_SCRCPY` is set, but contains a path to a file which does not exist or cannot be read."))
            }
            return Ok(path)
        }

        val path = System.getenv("PATH")
            ?: return Err(Availability.Unavailable("PATH is not set. Something seems to be really wrong."))
        val parsedPath = path.splitToSequence(':').map(::Path)
        val binary = findScrcpyInPath(parsedPath)
        if (binary != null) {
            return Ok(binary)
        }
        return Err(Availability.Unavailable("scrcpy is not available in PATH."))
    }

    private fun findScrcpyInPath(path: Sequence<Path>): Path? {
        return path
            .filter { it.exists() }
            .flatMap(Path::listDirectoryEntries)
            .filter { it.name == "scrcpy" }
            .filter { it.exists() }
            .firstOrNull()
    }
}