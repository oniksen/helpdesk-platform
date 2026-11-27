package data

import data.dto.authstate.AuthStateDto
import data.storage.AuthStateStorage
import data.storage.JvmAuthStateStorage
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class AuthStateCleanerTest {

    private lateinit var workDir: File
    private lateinit var storage: AuthStateStorage
    private lateinit var cleaner: AuthStateCleaner

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("auth-state-cleaner-test").toFile()
        storage = JvmAuthStateStorage(File(workDir, JvmAuthStateStorage.AUTH_STORAGE_FILE_NAME))
        cleaner = AuthStateCleaner(storage)
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `clears stored session when auto login is disabled`() {
        storage.save(stateJson(autoLogin = false))

        cleaner.clearIfAutoLoginDisabled()

        assertNull(storage.load(), "Сессия с autoLogin=false должна удаляться при выходе")
    }

    @Test
    fun `keeps stored session when auto login is enabled`() {
        val state = stateJson(autoLogin = true)
        storage.save(state)

        cleaner.clearIfAutoLoginDisabled()

        assertEquals(state, storage.load(), "Сессия с autoLogin=true должна сохраняться при выходе")
    }

    private fun stateJson(autoLogin: Boolean): String =
        Json.encodeToString(AuthStateDto(autoLogin = autoLogin, token = "token-1"))
}
