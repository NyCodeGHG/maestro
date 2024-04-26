package maestro.recording

sealed class Availability {
    object Available : Availability()
    class Unavailable(val reason: String) : Availability()
}