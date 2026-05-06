package ru.homebuhg.core.data.database

import androidx.room.TypeConverter
import ru.homebuhg.core.data.database.entity.AccountType
import ru.homebuhg.core.data.database.entity.BudgetPeriod
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.MemberRole
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionType

class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name
    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromSourceType(v: SourceType): String = v.name
    @TypeConverter fun toSourceType(v: String): SourceType = SourceType.valueOf(v)

    @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
    @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun fromMemberRole(v: MemberRole): String = v.name
    @TypeConverter fun toMemberRole(v: String): MemberRole = MemberRole.valueOf(v)
}
