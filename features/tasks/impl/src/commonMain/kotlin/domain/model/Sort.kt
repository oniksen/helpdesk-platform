package domain.model

data class Sort(
    val param: SortParam,
    val order: Order = Order.asc,
)