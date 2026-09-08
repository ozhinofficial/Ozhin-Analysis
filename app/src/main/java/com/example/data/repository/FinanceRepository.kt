package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.SubscriptionDao
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val subscriptionDao: SubscriptionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun insertSubscription(subscription: SubscriptionEntity): Long =
        subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: SubscriptionEntity) =
        subscriptionDao.updateSubscription(subscription)

    suspend fun deleteSubscription(subscription: SubscriptionEntity) =
        subscriptionDao.deleteSubscription(subscription)

    suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    suspend fun restoreAll(
        transactions: List<TransactionEntity>,
        subscriptions: List<SubscriptionEntity>,
        budgets: List<BudgetEntity>
    ) {
        transactionDao.deleteAll()
        subscriptionDao.deleteAll()
        budgetDao.deleteAll()

        if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)
        if (subscriptions.isNotEmpty()) subscriptionDao.insertAll(subscriptions)
        if (budgets.isNotEmpty()) budgetDao.insertAll(budgets)
    }

    suspend fun seedDefaultDataIfEmpty() {
        val existing = transactionDao.getAllTransactions().first()
        if (existing.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        // Sample initial transactions with notes & tax deductible tags
        val sampleTransactions = listOf(
            TransactionEntity(
                title = "Monthly Salary Deposit",
                amount = 4850.0,
                type = "INCOME",
                category = "Salary",
                notes = "Direct deposit from primary employer. Includes quarterly incentive bonus.",
                date = now - (2 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Apartment Rent",
                amount = 1450.0,
                type = "EXPENSE",
                category = "Housing",
                notes = "September residential lease payment via bank transfer.",
                date = now - (3 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Whole Foods Organic Market",
                amount = 168.45,
                type = "EXPENSE",
                category = "Food & Dining",
                notes = "Weekly grocery staples, produce, and household pantry essentials.",
                date = now - (1 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Consulting & Freelance Retainer",
                amount = 950.0,
                type = "INCOME",
                category = "Freelance",
                notes = "Client project milestone #2 delivery payment for UI design.",
                date = now - (5 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Ergonomic Office Chair & Desk Lamp",
                amount = 320.0,
                type = "EXPENSE",
                category = "Home Office",
                notes = "Equipment purchased for remote software consulting business.",
                date = now - (6 * oneDay),
                currency = "USD",
                isTaxDeductible = true,
                taxCategory = "Home Office Expense"
            ),
            TransactionEntity(
                title = "Electric & Water Utilities",
                amount = 115.30,
                type = "EXPENSE",
                category = "Utilities",
                notes = "Monthly utility bill payment auto-debited.",
                date = now - (7 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Charitable Donation - Red Cross",
                amount = 150.0,
                type = "EXPENSE",
                category = "Charity",
                notes = "Disaster relief contribution with tax acknowledgement receipt.",
                date = now - (9 * oneDay),
                currency = "USD",
                isTaxDeductible = true,
                taxCategory = "501(c)(3) Charitable Contribution"
            ),
            TransactionEntity(
                title = "Subway & Transit Pass",
                amount = 85.0,
                type = "EXPENSE",
                category = "Transportation",
                notes = "Monthly commuter transit pass reload.",
                date = now - (11 * oneDay),
                currency = "USD",
                isTaxDeductible = false
            ),
            TransactionEntity(
                title = "Medical Clinic Checkup & Prescription",
                amount = 95.0,
                type = "EXPENSE",
                category = "Healthcare",
                notes = "Routine preventive health consultation copay and medicine.",
                date = now - (14 * oneDay),
                currency = "USD",
                isTaxDeductible = true,
                taxCategory = "Medical & Dental"
            )
        )
        transactionDao.insertAll(sampleTransactions)

        // Seed Subscriptions
        val sampleSubscriptions = listOf(
            SubscriptionEntity(
                name = "Cloud Server Hosting & Domain",
                amount = 24.99,
                currency = "USD",
                billingCycle = "MONTHLY",
                nextDueDate = now + (2 * oneDay), // due in 2 days -> triggers bill alert!
                category = "Technology",
                notes = "Dedicated cloud VPS instance renewal. Essential for freelance client staging.",
                reminderDaysBefore = 3
            ),
            SubscriptionEntity(
                name = "Netflix 4K Ultra Streaming",
                amount = 22.99,
                currency = "USD",
                billingCycle = "MONTHLY",
                nextDueDate = now + (5 * oneDay),
                category = "Entertainment",
                notes = "Shared family entertainment account."
            ),
            SubscriptionEntity(
                name = "Gym & Wellness Membership",
                amount = 49.00,
                currency = "USD",
                billingCycle = "MONTHLY",
                nextDueDate = now + (9 * oneDay),
                category = "Health & Fitness",
                notes = "Monthly gym pass with locker access."
            ),
            SubscriptionEntity(
                name = "Spotify Premium Duo",
                amount = 14.99,
                currency = "USD",
                billingCycle = "MONTHLY",
                nextDueDate = now + (16 * oneDay),
                category = "Entertainment",
                notes = "High-fidelity audio streaming subscription."
            )
        )
        subscriptionDao.insertAll(sampleSubscriptions)

        // Seed Monthly Budgets
        val sampleBudgets = listOf(
            BudgetEntity(category = "Food & Dining", monthlyLimit = 500.0, monthYear = "2026-09", currency = "USD"),
            BudgetEntity(category = "Housing", monthlyLimit = 1600.0, monthYear = "2026-09", currency = "USD"),
            BudgetEntity(category = "Transportation", monthlyLimit = 200.0, monthYear = "2026-09", currency = "USD"),
            BudgetEntity(category = "Entertainment", monthlyLimit = 150.0, monthYear = "2026-09", currency = "USD"),
            BudgetEntity(category = "Utilities", monthlyLimit = 200.0, monthYear = "2026-09", currency = "USD"),
            BudgetEntity(category = "Healthcare", monthlyLimit = 250.0, monthYear = "2026-09", currency = "USD")
        )
        budgetDao.insertAll(sampleBudgets)
    }
}
