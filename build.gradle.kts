import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

val webVersion = WebVersion(0, 3, 0, AppVersion.Stage.Alpha, 6)
val jvmVersion = JVMVersion(0, 1, 0, AppVersion.Stage.Alpha, 1)

// Техническая версия JVM-приложения для упаковки (nativeDistributions.packageVersion).
extra["jvmSystemVersion"] = jvmVersion.createSystemVersion()

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.mokkery) apply false
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))
    parallel = true
    basePath.set(projectDir)
    source.setFrom(
        fileTree(projectDir) {
            include("**/src/*/kotlin/**/*.kt")
            exclude("**/build/**")
            exclude("**/generated/**")
            exclude("**/resources/**")
        }
    )
}

subprojects {
    plugins.withId("com.codingfeline.buildkonfig") {
        extensions.configure<BuildKonfigExtension> {
            packageName = "helpdesk-platform.config"

            defaultConfigs {
                buildConfigField(FieldSpec.Type.STRING, "PROJECT_TECHNICAL_VERSION_WEB", webVersion.createSystemVersion())
                buildConfigField(FieldSpec.Type.STRING, "PROJECT_TECHNICAL_VERSION_JVM", jvmVersion.createSystemVersion())
            }
        }
    }
}

val coverageModules = listOf(
    ":core:architecture",
    ":core:authorization:impl",
    ":core:di",
    ":core:di:infra",
    ":core:exception",
    ":core:network:impl",
    ":core:navigation:impl",
    ":core:uiadaptive",
    ":core:update:impl",
    ":features:authorization:impl",
    ":features:parking:impl",
    ":features:tasks:impl",
    ":features:update:impl",
    ":maxminiappapi:impl"
)

tasks.register("koverXmlReportsAll") {
    group = "verification"
    description = "Generate Kover XML reports for all modules"

    dependsOn(coverageModules.map { "$it:koverXmlReport" })
}

tasks.register<GenerateCoverageBadge>("generateCoverageBadge") {
    group = "verification"
    description = "Generates a local coverage badge SVG for the whole KMP project"

    dependsOn("koverXmlReportsAll")

    reports.from(
        coverageModules.map { module ->
            layout.projectDirectory.dir(
                "${module.removePrefix(":").replace(':', '/')}/build/reports/kover/report.xml"
            )
        }
    )
    outputFile.set(layout.projectDirectory.file("coverage-badge.svg"))
}

/** Корневая папка артефактов: всё, что собирается, лежит внутри неё. */
val distDir = layout.projectDirectory.dir("dist")

/** Готовые к деплою архивы. */
val packagesDir = distDir.dir("packages")

/** Метаданные сборок: версии, хэши, размеры, целевые пути на сервере. */
val metadataDir = distDir.dir("metadata")

tasks.register("build-web-app") { description = "Сборка основного приложения"
    dependsOn(":webApp:jsBrowserDevelopmentExecutableDistribution") }

tasks.register("build-web-shell") { description = "Сборка бутстрап оболочки с авторизацией и проверкой версии"
    dependsOn(":webShell:jsBrowserDevelopmentExecutableDistribution") }

tasks.register<Delete>("cleanDist") {
    group = "build"
    description = "Удаляет корневую папку артефактов dist/"
    delete(distDir)
}

/** Образ приложения, который создаёт :jvmApp:createReleaseDistributable. */
val jvmAppImageRoot = distDir.dir("jvm/main-release/app").asFile

/** Служебные файлы macOS: resource forks, расширенные атрибуты, настройки Finder. */
val osMetadataExcludes = listOf("**/.DS_Store", "**/__MACOSX/**", "**/._*")

/** Хост-платформа: JVM-таргет собирается только под ту ОС, на которой идёт сборка. */
val hostOsName = System.getProperty("os.name").orEmpty().lowercase()
val isMacHost = hostOsName.contains("mac")
val isWindowsHost = hostOsName.contains("win")

/** macOS упаковывает приложение в бандл `*.app`, Windows — в папку с `.exe`. */
val jvmAppImageDirName = if (isMacHost) "com.lpmti.helpdesk.app" else "com.lpmti.helpdesk"
val jvmAppContentPattern = if (isMacHost) "Contents/**" else "**"
val jvmAppArchiveSuffix = if (isMacHost) "macos" else "windows"
val jvmAppImageDir = jvmAppImageRoot.resolve(jvmAppImageDirName)

tasks.register<Zip>("build-jvm-app") {
    description = "Сборка JVM таргета текущей ОС и zip-архива для обновления"
    dependsOn(":jvmApp:createReleaseDistributable")

    // Локальные значения: замыкания задач не должны ссылаться на скрипт (Configuration Cache)
    val imageDir = jvmAppImageDir
    val windowsHost = isWindowsHost
    val macHost = isMacHost

    // Корень архива — директория установки: папка с .exe (Windows) либо Contents/ бандла (macOS)
    from(imageDir) {
        include(jvmAppContentPattern)
        exclude(osMetadataExcludes)
    }

    archiveFileName.set("helpdesk-jvm-$jvmAppArchiveSuffix-${jvmVersion.createSystemVersion()}.zip")
    destinationDirectory.set(packagesDir)
    includeEmptyDirs = false
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doFirst {
        if (!windowsHost && !macHost) {
            throw GradleException(
                "build-jvm-app собирает JVM таргет текущей ОС: поддерживаются только Windows и macOS, " +
                    "текущая ОС — ${System.getProperty("os.name")}",
            )
        }
        if (!imageDir.isDirectory) {
            throw GradleException(
                "Нет образа приложения: $imageDir. " +
                    "Сначала должна пройти :jvmApp:createReleaseDistributable.",
            )
        }
    }
}

/** Человекочитаемая (UI) версия web-таргета. */
val webUiVersion = webVersion.createUiVersion()

/** Техническая версия web-таргета — попадает в манифест обновлений. */
val webTechnicalVersion = webVersion.createSystemVersion()
val webStageName = webVersion.createStageName()
val webChannelName = webVersion.createChannel()

val jvmUiVersion = jvmVersion.createUiVersion()
val jvmTechnicalVersion = jvmVersion.createSystemVersion()
val jvmStageName = jvmVersion.createStageName()
val jvmChannelName = jvmVersion.createChannel()

/**
 * Путь сборки на сервере — тот же, что ожидает `update-manifest-v4.json`.
 *
 * Прод-сборки лежат по фиксированному имени (`<platform>-latest.zip`), сборки
 * каналов — в `dev/<channel>/` с именем `<platform>-<channel>.zip`.
 */
fun serverPath(platform: String, channel: String): String {
    val channelPath = if (channel == "prod") "prod" else "dev/$channel"
    val fileName = if (channel == "prod") "$platform-latest.zip" else "$platform-$channel.zip"
    return "helpdesk-app/channels-v2/$channelPath/$platform/$fileName"
}

tasks.register<GenerateVersionsJson>("versionsJson") {
    group = "build"
    description = "Пишет dist/metadata/versions.json — единый источник версий для CI"
    webUi.set(webUiVersion)
    webTechnical.set(webTechnicalVersion)
    webStage.set(webStageName)
    webChannel.set(webChannelName)
    jvmUi.set(jvmUiVersion)
    jvmTechnical.set(jvmTechnicalVersion)
    jvmStage.set(jvmStageName)
    jvmChannel.set(jvmChannelName)
    outputFile.set(metadataDir.file("versions.json"))
}

val packageWebApp = tasks.register<Zip>("packageWebApp") {
    group = "build"
    description = "Упаковка дистрибутива webApp в dist/packages"
    dependsOn(":webApp:jsBrowserDevelopmentExecutableDistribution")
    from(distDir.dir("web"))
    archiveFileName.set("web-$webUiVersion.zip")
    destinationDirectory.set(packagesDir)
    includeEmptyDirs = false
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val packageWebShell = tasks.register<Zip>("packageWebShell") {
    group = "build"
    description = "Упаковка гейта webShell в dist/packages"
    // stampGateIndex проставляет ?v=<hash> в index.html и должен отработать до упаковки
    dependsOn(":webShell:jsBrowserDevelopmentExecutableDistribution", ":webShell:stampGateIndex")
    from(distDir.dir("web-shell"))
    archiveFileName.set("webshell-$webUiVersion.zip")
    destinationDirectory.set(packagesDir)
    includeEmptyDirs = false
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register<WriteBuildMetadata>("writeWebAppMetadata") {
    group = "build"
    description = "Метаданные сборки webApp для манифеста обновлений"
    dependsOn(packageWebApp)
    archive.set(packagesDir.file("web-$webUiVersion.zip"))
    component.set("web")
    platform.set("web")
    uiVersion.set(webUiVersion)
    technicalVersion.set(webTechnicalVersion)
    stage.set(webStageName)
    channel.set(webChannelName)
    commitSha.set(providers.environmentVariable("GITHUB_SHA").orElse(""))
    targetPath.set(serverPath("web", webChannelName))
    metadataFile.set(metadataDir.file("web-app.metadata.json"))
    checksumsFile.set(metadataDir.file("SHA256SUMS-web.txt"))
}

tasks.register<WriteBuildMetadata>("writeWebShellMetadata") {
    group = "build"
    description = "Метаданные сборки гейта webShell"
    dependsOn(packageWebShell)
    archive.set(packagesDir.file("webshell-$webUiVersion.zip"))
    component.set("webshell")
    platform.set("web")
    uiVersion.set(webUiVersion)
    technicalVersion.set(webTechnicalVersion)
    stage.set(webStageName)
    channel.set(webChannelName)
    commitSha.set(providers.environmentVariable("GITHUB_SHA").orElse(""))
    targetPath.set("helpdesk-app/pwa/")
    gateIndex.set(distDir.file("web-shell/index.html"))
    metadataFile.set(metadataDir.file("web-shell.metadata.json"))
    checksumsFile.set(metadataDir.file("SHA256SUMS-webshell.txt"))
}

tasks.register<WriteBuildMetadata>("writeJvmMetadata") {
    group = "build"
    description = "Метаданные сборки JVM-таргета текущей ОС"
    dependsOn("build-jvm-app")
    archive.set(packagesDir.file("helpdesk-jvm-$jvmAppArchiveSuffix-$jvmTechnicalVersion.zip"))
    component.set("jvm")
    platform.set(jvmAppArchiveSuffix)
    uiVersion.set(jvmUiVersion)
    technicalVersion.set(jvmTechnicalVersion)
    stage.set(jvmStageName)
    channel.set(jvmChannelName)
    commitSha.set(providers.environmentVariable("GITHUB_SHA").orElse(""))
    targetPath.set(serverPath(jvmAppArchiveSuffix, jvmChannelName))
    metadataFile.set(metadataDir.file("jvm-$jvmAppArchiveSuffix.metadata.json"))
    checksumsFile.set(metadataDir.file("SHA256SUMS-jvm-$jvmAppArchiveSuffix.txt"))
}

/**
 * Собирает единый бейдж покрытия по XML-отчётам Kover всех подключённых модулей.
 *
 * Реализовано отдельным типом задачи, а не через `doLast`, чтобы быть совместимым
 * с Configuration Cache: внутри `doLast` нельзя обращаться к объекту скрипта и к
 * `Project`, иначе запись кэша не сериализуется.
 */
abstract class GenerateCoverageBadge : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reports: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val xmlFiles = reports.files.filter { it.isFile }

        if (xmlFiles.isEmpty()) {
            error("No Kover XML reports found")
        }

        var totalCovered = 0
        var totalMissed = 0

        xmlFiles.forEach { xmlFile ->
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.parse(xmlFile)
            doc.documentElement.normalize()

            val counterNodes = doc.getElementsByTagName("counter")
            for (i in 0 until counterNodes.length) {
                val node = counterNodes.item(i)
                val type = node.attributes.getNamedItem("type").nodeValue
                if (type == "LINE") {
                    totalCovered += node.attributes.getNamedItem("covered").nodeValue.toInt()
                    totalMissed += node.attributes.getNamedItem("missed").nodeValue.toInt()
                }
            }
        }

        val total = totalCovered + totalMissed
        val percentage = if (total == 0) 0 else (totalCovered * 100 / total)

        val badgeColor = when {
            percentage >= 80 -> "#4c1"
            percentage >= 50 -> "#dfb317"
            else -> "#e05d44"
        }

        val badgeSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="110" height="20">
              <linearGradient id="b" x2="0" y2="100%">
                <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
                <stop offset="1" stop-opacity=".1"/>
              </linearGradient>
              <mask id="a">
                <rect width="110" height="20" rx="3" fill="#fff"/>
              </mask>
              <g mask="url(#a)">
                <rect width="60" height="20" fill="#555"/>
                <rect x="60" width="50" height="20" fill="$badgeColor"/>
                <rect width="110" height="20" fill="url(#b)"/>
              </g>
              <g fill="#fff" text-anchor="middle" font-family="Verdana" font-size="11">
                <text x="30" y="14">coverage</text>
                <text x="85" y="14">$percentage%</text>
              </g>
            </svg>
        """.trimIndent()

        val badgeFile = outputFile.get().asFile
        badgeFile.writeText(badgeSvg)
        logger.lifecycle("Coverage badge generated: ${badgeFile.absolutePath}")
        logger.lifecycle("Coverage: $percentage%")
    }
}

abstract class AppVersion(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val stage: Stage, // alpha, beta, rc, release
    private val buildIteration: Int,
) {
    enum class Stage(val value: String) {
        Alpha("alpha"), Beta("beta"), Rc("rc"), Release("release")
    }

    fun createUiVersion(): String {
        return when(stage) {
            Stage.Release -> "$major.$minor.$patch"
            else -> "$major.$minor.$patch-${stage.value}.$buildIteration"
        }
    }

    fun createSystemVersion(): String {
        return "$major.$minor.${calculateTechnicalPatch(stage, patch, buildIteration)}"
    }

    /** Имя стадии для CI: `alpha`, `beta`, `rc`, `release`. */
    fun createStageName(): String = stage.value

    /** Канал доставки: alpha/beta уходят в canary, rc — в rc, release — в prod. */
    fun createChannel(): String = when (stage) {
        Stage.Alpha, Stage.Beta -> "canary"
        Stage.Rc -> "rc"
        Stage.Release -> "prod"
    }

    private fun calculateTechnicalPatch(stage: Stage, patch: Int, iteration: Int): Int {
        val stageOffset = when(stage) {
            Stage.Alpha -> 1_000
            Stage.Beta -> 2_000
            Stage.Rc -> 3_000
            else -> 4_000 // release
        }
        return patch * 10_000 + stageOffset + iteration
    }
}

class WebVersion(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val stage: AppVersion.Stage,
    private val buildIteration: Int,
): AppVersion(major, minor, patch, stage, buildIteration)
class JVMVersion(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val stage: AppVersion.Stage,
    private val buildIteration: Int,
): AppVersion(major, minor, patch, stage, buildIteration)

/**
 * Пишет `dist/metadata/versions.json` — единственный источник версий для CI.
 *
 * Формат документа описан в дизайне `docs/superpowers/specs/2026-11-20-ci-build-and-release-design.md`:
 * по компоненту (`web`, `jvm`) — UI-версия, техническая версия, тэг, стадия,
 * канал и признак пререлиза.
 */
abstract class GenerateVersionsJson : DefaultTask() {
    @get:Input
    abstract val webUi: Property<String>

    @get:Input
    abstract val webTechnical: Property<String>

    @get:Input
    abstract val webStage: Property<String>

    @get:Input
    abstract val webChannel: Property<String>

    @get:Input
    abstract val jvmUi: Property<String>

    @get:Input
    abstract val jvmTechnical: Property<String>

    @get:Input
    abstract val jvmStage: Property<String>

    @get:Input
    abstract val jvmChannel: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val web = componentJson("web", webUi.get(), webTechnical.get(), webStage.get(), webChannel.get())
        val jvm = componentJson("jvm", jvmUi.get(), jvmTechnical.get(), jvmStage.get(), jvmChannel.get())

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("{\n  \"web\": $web,\n  \"jvm\": $jvm\n}\n")

        logger.lifecycle("Версии: ${file.absolutePath}")
    }

    private fun componentJson(
        prefix: String,
        ui: String,
        technical: String,
        stage: String,
        channel: String,
    ): String {
        val prerelease = stage != AppVersion.Stage.Release.value
        return """{ "ui": "$ui", "technical": "$technical", "tag": "$prefix-v$ui", """ +
            """"stage": "$stage", "channel": "$channel", "prerelease": $prerelease }"""
    }
}

/**
 * Формирует метаданные собранного архива: версия, канал, размер, SHA-256 и путь
 * на сервере. Это готовый фрагмент для `update-manifest-v4.json`.
 */
abstract class WriteBuildMetadata : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:Input
    abstract val component: Property<String>

    @get:Input
    abstract val platform: Property<String>

    @get:Input
    abstract val uiVersion: Property<String>

    @get:Input
    abstract val technicalVersion: Property<String>

    @get:Input
    abstract val stage: Property<String>

    @get:Input
    abstract val channel: Property<String>

    @get:Input
    abstract val commitSha: Property<String>

    @get:Input
    abstract val targetPath: Property<String>

    /** `index.html` гейта — источник контентного хэша версии webShell. */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val gateIndex: RegularFileProperty

    @get:OutputFile
    abstract val metadataFile: RegularFileProperty

    @get:OutputFile
    abstract val checksumsFile: RegularFileProperty

    @TaskAction
    fun write() {
        val file = archive.get().asFile
        val sha256 = file.inputStream().buffered().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }

        val gateHash = gateIndex.orNull?.asFile?.let(::readGateHash)
        val gateLine = gateHash?.let { field -> "  \"gateHash\": \"$field\",\n" }.orEmpty()

        val json = buildString {
            append("{\n")
            append("  \"component\": \"${component.get()}\",\n")
            append("  \"platform\": \"${platform.get()}\",\n")
            append("  \"uiVersion\": \"${uiVersion.get()}\",\n")
            append("  \"technicalVersion\": \"${technicalVersion.get()}\",\n")
            append("  \"stage\": \"${stage.get()}\",\n")
            append("  \"channel\": \"${channel.get()}\",\n")
            append("  \"commitSha\": \"${commitSha.get()}\",\n")
            append("  \"date\": \"${java.time.Instant.now()}\",\n")
            append("  \"fileName\": \"${file.name}\",\n")
            append("  \"size\": ${file.length()},\n")
            append("  \"sha256\": \"$sha256\",\n")
            append(gateLine)
            append("  \"serverPath\": \"${targetPath.get()}\"\n")
            append("}\n")
        }

        val metadata = metadataFile.get().asFile
        metadata.parentFile.mkdirs()
        metadata.writeText(json)

        val checksums = checksumsFile.get().asFile
        checksums.parentFile.mkdirs()
        checksums.writeText("$sha256  ${file.name}\n")

        logger.lifecycle("Архив:   ${file.absolutePath}")
        logger.lifecycle("Размер:  ${file.length()} байт")
        logger.lifecycle("SHA-256: $sha256")
        logger.lifecycle("Метаданные: ${metadata.absolutePath}")
    }

    private fun readGateHash(index: File): String? {
        val match = GATE_VERSION.find(index.readText()) ?: return null
        return match.groupValues[1]
    }

    private companion object {
        /** Контентный хэш версии гейта, который `stampGateIndex` пишет в ссылки. */
        val GATE_VERSION = Regex("""webShell\.js\?v=([0-9a-f]{12})""")
    }
}
