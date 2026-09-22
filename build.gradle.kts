import javax.xml.parsers.DocumentBuilderFactory

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.detekt)
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

tasks.register("koverXmlReportsAll") {
    group = "verification"
    description = "Generate Kover XML reports for all modules"

    dependsOn(
        ":features:parking:impl:koverXmlReport",
        ":maxminiappapi:impl:koverXmlReport",
        ":core:di:koverXmlReport"
    )
}

tasks.register("generateCoverageBadge") {
    group = "verification"
    description = "Generates a local coverage badge SVG for the whole KMP project"

    outputs.upToDateWhen { false }

    dependsOn("koverXmlReportsAll")

    doLast {
        val xmlFiles = fileTree(rootDir) {
            include("**/build/reports/kover/report.xml")
        }.files

        if (xmlFiles.isEmpty()) {
            println("ERROR: No Kover XML reports found")
            return@doLast
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

        val badgeFile = file("coverage-badge.svg")
        badgeFile.writeText(badgeSvg)
        println("Coverage badge generated: ${badgeFile.absolutePath}")
        println("Coverage: $percentage%")
    }
}