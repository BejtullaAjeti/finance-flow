package com.example.financeflow.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun toTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun fromCategoryType(value: String?): CategoryType? = value?.let { CategoryType.valueOf(it) }

    @TypeConverter
    fun toCategoryType(type: CategoryType?): String? = type?.name

    @TypeConverter
    fun fromFrequency(value: String?): Frequency? = value?.let { Frequency.valueOf(it) }

    @TypeConverter
    fun toFrequency(frequency: Frequency?): String? = frequency?.name

    @TypeConverter
    fun fromCurrency(value: String?): Currency? = value?.let { Currency.valueOf(it) }

    @TypeConverter
    fun toCurrency(currency: Currency?): String? = currency?.name
}
