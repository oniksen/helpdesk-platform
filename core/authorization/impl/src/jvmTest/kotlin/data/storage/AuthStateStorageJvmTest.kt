package data.storage

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthStateStorageJvmTest {

    private lateinit var workDir: File
    private lateinit var stateFile: File
    private lateinit var storage: AuthStateStorage

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("auth-state-storage-test").toFile()
        stateFile = File(workDir, JvmAuthStateStorage.AUTH_STORAGE_FILE_NAME)
        storage = JvmAuthStateStorage(stateFile)
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `returns null before the state file is created`() {
        assertNull(storage.load(), "Хранилище без файла не должно возвращать состояние")
    }

    @Test
    fun `creates the file with the saved state`() {
        storage.save(STATE_JSON)

        assertTrue(stateFile.isFile, "После сохранения ожидался файл '$stateFile'")
        assertEquals(STATE_JSON, stateFile.readText(), "Содержимое файла отличается от сохранённого состояния")
        assertEquals(STATE_JSON, storage.load(), "Хранилище не вернуло сохранённое состояние")
    }

    @Test
    fun `replaces previously saved state`() {
        storage.save(STATE_JSON)
        storage.save(UPDATED_STATE_JSON)

        assertEquals(UPDATED_STATE_JSON, storage.load(), "Хранилище вернуло устаревшее состояние")
    }

    @Test
    fun `removes the state file on clear`() {
        storage.save(STATE_JSON)

        storage.clear()

        assertFalse(stateFile.exists(), "После удаления файл '$stateFile' не должен существовать")
        assertNull(storage.load(), "После удаления хранилище не должно возвращать состояние")
    }

    @Test
    fun `clear is safe when the state file is missing`() {
        storage.clear()

        assertNull(storage.load(), "Повторное удаление не должно нарушать работу хранилища")
    }

    private companion object {
        const val STATE_JSON = """{"token":"token-1","userEmail":"user@example.com"}"""
        const val UPDATED_STATE_JSON = """{"token":"token-2","userEmail":"user@example.com"}"""
    }
}
