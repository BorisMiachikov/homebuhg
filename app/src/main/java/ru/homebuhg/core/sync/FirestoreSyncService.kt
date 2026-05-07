package ru.homebuhg.core.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ru.homebuhg.core.data.datastore.PreferencesRepository
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.BudgetRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.di.FirebaseHolder
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

private const val BATCH_SIZE = 400

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firebase: FirebaseHolder,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    suspend fun sync(householdId: String) {
        val db = firebase.firestore ?: return
        val auth = firebase.auth ?: return
        if (auth.currentUser == null) return

        val lastSync = preferencesRepository.lastSyncMs.first()
        val base = "households/$householdId"

        withContext(io) {
            upload(db, base, householdId, lastSync)
            download(db, base, householdId, lastSync)
        }

        preferencesRepository.setLastSyncMs(System.currentTimeMillis())
    }

    // ─── Upload ───────────────────────────────────────────────────────────────

    private suspend fun upload(db: FirebaseFirestore, base: String, hid: String, since: Long) {
        batchSet(db, "$base/transactions",
            transactionRepository.getModifiedSince(hid, since).map { it.id to it.toMap() })

        batchSet(db, "$base/accounts",
            accountRepository.getModifiedSince(hid, since).map { it.id to it.toMap() })

        batchSet(db, "$base/categories",
            categoryRepository.getModifiedSince(hid, since).map { it.id to it.toMap() })

        batchSet(db, "$base/budgets",
            budgetRepository.getModifiedSince(hid, since).map { it.id to it.toMap() })
    }

    private suspend fun batchSet(
        db: FirebaseFirestore,
        collectionPath: String,
        docs: List<Pair<String, Map<String, Any?>>>
    ) {
        docs.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (id, data) ->
                batch.set(db.document("$collectionPath/$id"), data)
            }
            batch.commit().await()
        }
    }

    // ─── Download ─────────────────────────────────────────────────────────────

    private suspend fun download(db: FirebaseFirestore, base: String, hid: String, since: Long) {
        db.collection("$base/transactions").whereGreaterThan("updatedAt", since)
            .get().await().documents.forEach { doc ->
                doc.data?.let { transactionRepository.upsert(it.toTransactionEntity()) }
            }

        db.collection("$base/accounts").whereGreaterThan("updatedAt", since)
            .get().await().documents.forEach { doc ->
                doc.data?.let { accountRepository.upsert(it.toAccountEntity()) }
            }

        db.collection("$base/categories").whereGreaterThan("updatedAt", since)
            .get().await().documents.forEach { doc ->
                doc.data?.let { categoryRepository.upsert(it.toCategoryEntity()) }
            }

        db.collection("$base/budgets").whereGreaterThan("updatedAt", since)
            .get().await().documents.forEach { doc ->
                doc.data?.let { budgetRepository.upsert(it.toBudgetEntity()) }
            }
    }
}
