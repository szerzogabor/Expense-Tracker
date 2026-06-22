package com.expensetracker.ui

/** A filter applied to the transaction list when navigating from category/tag/account. */
data class TxFilter(
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val accountIds: Set<Long> = emptySet(),
    val title: String? = null,
)
