package data.mapper

import data.dto.PaginationDto
import data.dto.TaskDataDto
import data.dto.StatusesDto
import data.dto.TasksPageDto
import domain.model.Pagination
import domain.model.Statuses
import domain.model.TaskData
import domain.model.TasksPage

internal fun TasksPageDto.toDomain(): TasksPage {
    return TasksPage(
        data = this.dataDto?.mapNotNull { it?.toDomain() } ?: emptyList(),
        paginationDto = this.paginationDto.toDomain()
    )
}

private fun TaskDataDto.toDomain(): TaskData = TaskData(
    building = this.buildingDto,
    category = this.categoryDto ?: "CURRENT",
    commercial = this.commercialDto ?: false,
    contacts = this.contactsDto?.filterNotNull() ?: emptyList(),
    dateCreate = this.dateCreateDto,
    description = this.descriptionDto ?: "Не указано",
    id = this.idDto?.toInt() ?: error("Не указан ID задачи"),
    initiator = this.initiatorDto ?: "Не указан инициатор",
    reopenCount = this.reopenCountDto ?: 0,
    serviceId = this.serviceIdDto ?: "Не указан ID сервиса",
    status = this.statusDto,
    statuses = this.statusesDto.toDomain(),
    title = this.titleDto ?: "Не указано",
)

private fun StatusesDto?.toDomain(): Statuses = Statuses(
    assignedUsersDto = this?.assignedUsersDto?.filterNotNull() ?: emptyList(),
    completedUsersDto = this?.completedUsersDto?.filterNotNull() ?: emptyList(),
    inProgressUsersDto = this?.inProgressUsersDto?.filterNotNull() ?: emptyList(),
)

private fun PaginationDto?.toDomain(): Pagination = Pagination(
    hasNext = this?.hasNextDto ?: false,
    hasPrevious = this?.hasPreviousDto ?: false,
    page = this?.pageDto ?: -1,
    pageSize = this?.pageSizeDto ?: -1,
    totalItems = this?.totalItemsDto ?: -1,
    totalPages = this?.totalPagesDto ?: -1,
)