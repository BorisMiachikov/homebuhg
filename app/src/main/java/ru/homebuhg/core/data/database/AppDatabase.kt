package ru.homebuhg.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.homebuhg.core.data.database.dao.AccountDao
import ru.homebuhg.core.data.database.dao.BudgetDao
import ru.homebuhg.core.data.database.dao.CategoryDao
import ru.homebuhg.core.data.database.dao.HouseholdDao
import ru.homebuhg.core.data.database.dao.MerchantDao
import ru.homebuhg.core.data.database.dao.ReceiptItemDao
import ru.homebuhg.core.data.database.dao.RecurringRuleDao
import ru.homebuhg.core.data.database.dao.SmsRuleDao
import ru.homebuhg.core.data.database.dao.TransactionDao
import ru.homebuhg.core.data.database.dao.UserDao
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.BudgetEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.HouseholdEntity
import ru.homebuhg.core.data.database.entity.HouseholdMemberEntity
import ru.homebuhg.core.data.database.entity.MerchantEntity
import ru.homebuhg.core.data.database.entity.ReceiptItemEntity
import ru.homebuhg.core.data.database.entity.RecurringRuleEntity
import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class,
        ReceiptItemEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
        SmsRuleEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun householdDao(): HouseholdDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun transactionDao(): TransactionDao
    abstract fun receiptItemDao(): ReceiptItemDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun smsRuleDao(): SmsRuleDao
}
