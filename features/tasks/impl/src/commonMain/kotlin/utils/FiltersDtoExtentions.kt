package utils

import data.dto.FiltersDto
import kotlinx.serialization.json.Json

internal fun FiltersDto.toJsonString(): String =
    Json.encodeToString(FiltersDto.serializer(), this)