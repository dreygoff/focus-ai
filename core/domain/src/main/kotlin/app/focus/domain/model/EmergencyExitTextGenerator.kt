package app.focus.domain.model

import kotlin.random.Random

object EmergencyExitTextGenerator {
    private const val CHARSET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ,.-"
    const val TEXT_LENGTH = 300

    fun generate(random: Random = Random.Default): String = buildString(TEXT_LENGTH) {
        repeat(TEXT_LENGTH) {
            append(CHARSET[random.nextInt(CHARSET.length)])
        }
    }
}
