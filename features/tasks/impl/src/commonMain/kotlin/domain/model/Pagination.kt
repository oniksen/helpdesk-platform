package domain.model

data class Pagination(
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int
)
