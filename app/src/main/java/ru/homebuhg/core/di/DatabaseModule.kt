package ru.homebuhg.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.homebuhg.core.data.database.AppDatabase
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "homebuhg.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideHouseholdDao(db: AppDatabase): HouseholdDao = db.householdDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideMerchantDao(db: AppDatabase): MerchantDao = db.merchantDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideReceiptItemDao(db: AppDatabase): ReceiptItemDao = db.receiptItemDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideRecurringRuleDao(db: AppDatabase): RecurringRuleDao = db.recurringRuleDao()
    @Provides fun provideSmsRuleDao(db: AppDatabase): SmsRuleDao = db.smsRuleDao()
}
