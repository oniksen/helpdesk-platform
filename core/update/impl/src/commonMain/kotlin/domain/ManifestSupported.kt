package domain

/**
 * Описание поддерживаемой версии манифеста относительно описывающих его DTO объектов.
 * */
object ManifestSupported {
    private val map = mapOf("v1" to 3, "v2" to 4)

    /**
     * @param dtoVersion Версию DTO смотреть в названии папки в которой они лежат.
     * Например, *v1*.
     * */
    fun getManifestVersion(dtoVersion: String): Int = map[dtoVersion] ?: error("Manifest DTO version $dtoVersion not supported")
}
