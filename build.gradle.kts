import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.buildkonfig) apply false
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

version = "0.1.1001"

subprojects {
    plugins.withId("com.codingfeline.buildkonfig") {
        extensions.configure<BuildKonfigExtension> {
            packageName = "helpdesk-platform.config"

            defaultConfigs {
                buildConfigField(FieldSpec.Type.STRING, "PROJECT_VERSION", rootProject.version.toString())
            }
        }
    }
}

tasks.register("koverXmlReportsAll") {
    group = "verification"
    description = "Generate Kover XML reports for all modules"

    dependsOn(
        ":core:architecture:koverXmlReport",
        ":core:authorization:impl:koverXmlReport",
        ":core:di:koverXmlReport",
        ":core:exception:koverXmlReport",
        ":core:network:impl:koverXmlReport",
        ":core:navigation:impl:koverXmlReport",
        ":core:uiadaptive:koverXmlReport",
        ":core:update:impl:koverXmlReport",
        ":features:authorization:impl:koverXmlReport",
        ":features:parking:impl:koverXmlReport",
        ":features:tasks:impl:koverXmlReport",
        ":features:update:impl:koverXmlReport",
        ":maxminiappapi:impl:koverXmlReport"
    )
}

tasks.register<GenerateCoverageBadge>("generateCoverageBadge") {
    group = "verification"
    description = "Generates a local coverage badge SVG for the whole KMP project"

    dependsOn("koverXmlReportsAll")

    reports.from(
        fileTree(rootDir) {
            include("**/build/reports/kover/report.xml")
        }
    )
    outputFile.set(layout.projectDirectory.file("coverage-badge.svg"))
}

tasks.register("build-web-app")   { description = "Сборка основного приложения"
    dependsOn(":webApp:jsBrowserDevelopmentExecutableDistribution") }

tasks.register("build-web-shell") { description = "Сборка бутстрап оболочки с авторизацией и проверкой версии"
    dependsOn(":webShell:jsBrowserDevelopmentExecutableDistribution") }

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