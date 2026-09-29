@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Этот метод является временным и будет изменён или удалён. Не использовать его в продакшн-коде"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TemporaryArchitectureStab
