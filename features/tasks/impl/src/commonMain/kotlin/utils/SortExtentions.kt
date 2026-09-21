package utils

import domain.model.Sort
import kotlinx.serialization.json.Json

internal fun Sort.toJsonString(): String {
    return Json.encodeToString(mapOf(param.value to order.name))
}