import `helpdesk-platform`.config.BuildKonfig

/**
 * Техническая версия JVM-приложения в формате `X.Y.Z`.
 *
 * BuildKonfig генерируется как internal внутри модуля, поэтому потребители
 * (точка входа `jvmApp`) читают версию через этот публичный хелпер.
 */
fun currentTechnicalVersion(): String = BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM
