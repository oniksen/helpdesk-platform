package data.mapper

import data.dto.FiltersDto
import domain.model.Filters
import domain.model.Sort

internal fun Filters.toDto(): FiltersDto = FiltersDto(
    buildings = this.buildings,
)