package user

enum class AccessLevel(val description: String) {
    EMPLOYEE("Работник"),
    DEPARTMENT_HEAD("Руководитель отдела"),
    COMPANY_DIRECTOR("Директор компании"),
    HOLDING_DIRECTOR("Директор холдинга")
}