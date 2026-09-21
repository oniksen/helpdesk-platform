package user

enum class UserRole(val description: String) {
    NONE(""),
    MANAGER("Менеджер"),
    DISPATCHER("Диспетчер"),
}