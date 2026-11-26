package dev.momro.fosspdf.data

data class RecentPdf(
    val uri: String,
    val name: String,
    val lastOpened: Long,
)
