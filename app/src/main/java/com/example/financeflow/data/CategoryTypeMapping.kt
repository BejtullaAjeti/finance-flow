package com.example.financeflow.data

fun categoryTypeFor(type: TransactionType): CategoryType =
    if (type == TransactionType.PERSONAL) CategoryType.PERSONAL else CategoryType.BUSINESS
