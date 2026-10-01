package domain.models.manifest.v2

data class Prod(
    val macosDto: TargetData,
    val webDto: TargetData,
    val windowsDto: TargetData,
)
