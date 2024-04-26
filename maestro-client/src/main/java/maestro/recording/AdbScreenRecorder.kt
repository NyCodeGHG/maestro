package maestro.recording

import maestro.drivers.AndroidDriver
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

/**
 * Screen recorder implementation which uses the "screenrecord" shell command
 */
class AdbScreenRecorder(private val driver: AndroidDriver) : AndroidScreenRecorder {

    private var recordingState: RecordingState = RecordingState.Stopped

    override fun isAvailable(): Availability {
        return Availability.Available
    }

    override fun record(path: Path): Future<Void> {
        if (recordingState is RecordingState.Running) {
            throw IllegalStateException("Recording is already running on this instance.")
        }
        val deviceRecordingPath = "/sdcard/maestro-screenrecording.mp4"
        val future = CompletableFuture.runAsync {
            try {
                driver.shell("screenrecord --bit-rate '100000' $deviceRecordingPath")
            } catch (e: IOException) {
                throw IOException(
                    "Failed to capture screen recording on the device. Note that some Android emulators do not support screen recording. " +
                            "Try using a different Android emulator (eg. Pixel 5 / API 30) or provide scrcpy instead.",
                    e,
                )
            }
        }
        recordingState = RecordingState.Running(devicePath = deviceRecordingPath, localPath = path, future = future)
        return future
    }

    override fun stop() {
        when (val state = recordingState) {
            is RecordingState.Stopped -> return
            is RecordingState.Running -> {
                driver.shell("killall -INT screenrecord")
                state.future.get()
                driver.pullFile(state.devicePath, state.localPath)
            }
        }
    }

    sealed interface RecordingState {
        object Stopped : RecordingState
        class Running(val devicePath: String, val localPath: Path, val future: Future<Void>) : RecordingState
    }
}