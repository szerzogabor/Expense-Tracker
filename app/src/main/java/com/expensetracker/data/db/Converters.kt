package com.expensetracker.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun typeToString(t: TransactionType): String = t.name
    @TypeConverter fun stringToType(s: String): TransactionType = TransactionType.valueOf(s)

    @TypeConverter fun modeToString(m: GenerationMode): String = m.name
    @TypeConverter fun stringToMode(s: String): GenerationMode = GenerationMode.valueOf(s)
}
