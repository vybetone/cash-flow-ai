package com.example.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class CashFlowRepository(
    private val tradingSignalDao: TradingSignalDao,
    private val tradeJournalDao: TradeJournalDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allSignals: Flow<List<TradingSignalEntity>> = tradingSignalDao.getAllSignals()
    val allJournalTrades: Flow<List<TradeJournalEntity>> = tradeJournalDao.getAllTrades()
    val totalJournalPnL: Flow<Double?> = tradeJournalDao.getTotalPnL()
    val winCount: Flow<Int> = tradeJournalDao.getWinCount()
    val totalClosedTradesCount: Flow<Int> = tradeJournalDao.getTotalClosedTradesCount()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    private val geminiEngine = GeminiTradingEngine()

    suspend fun analyzeAndSaveSignal(
        bitmap: Bitmap,
        context: Context,
        source: String = "SCREEN_ANALYSIS",
        preferredModel: String = "gemini-3.5-flash"
    ): TradingSignalEntity {
        val signal = geminiEngine.analyzeChartImage(bitmap, source, preferredModel)
        
        // Save image to internal storage
        val filename = "chart_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, filename)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val savedSignal = signal.copy(imageUri = file.absolutePath)
        val insertedId = tradingSignalDao.insertSignal(savedSignal)
        return savedSignal.copy(id = insertedId)
    }

    suspend fun insertSignal(signal: TradingSignalEntity): Long = tradingSignalDao.insertSignal(signal)
    suspend fun updateSignal(signal: TradingSignalEntity) = tradingSignalDao.updateSignal(signal)
    suspend fun deleteSignal(id: Long) = tradingSignalDao.deleteSignalById(id)

    suspend fun insertJournalTrade(trade: TradeJournalEntity): Long = tradeJournalDao.insertTrade(trade)
    suspend fun updateJournalTrade(trade: TradeJournalEntity) = tradeJournalDao.updateTrade(trade)
    suspend fun deleteJournalTrade(id: Long) = tradeJournalDao.deleteTradeById(id)

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    suspend fun saveBudget(budget: BudgetEntity) = budgetDao.insertOrUpdateBudget(budget)

    suspend fun seedInitialDataIfNeeded() {
        if (tradingSignalDao.getSignalCountSync() == 0) {
            seedTradingSignalsAndJournal()
        }

        if (transactionDao.getTransactionCount() == 0) {
            val now = System.currentTimeMillis()
            val day = 86400000L

            val defaultTransactions = listOf(
                TransactionEntity(
                    title = "Trading Account Deposit",
                    amount = 10000.00,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.INVESTMENT,
                    dateMillis = now - (day * 1),
                    note = "Initial capital allocation for AI algorithmic execution",
                    isRecurring = false
                ),
                TransactionEntity(
                    title = "NVDA Long Win Realized",
                    amount = 1420.50,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.INVESTMENT,
                    dateMillis = now - (day * 2),
                    note = "AI Signal #104 - 15m Double Bottom Target Hit (+14.2%)"
                ),
                TransactionEntity(
                    title = "BTC/USD Short Win Realized",
                    amount = 890.00,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.INVESTMENT,
                    dateMillis = now - (day * 3),
                    note = "AI Signal #102 - 1h Supply Rejection (+8.9%)"
                ),
                TransactionEntity(
                    title = "Bloomberg Terminal Subscription",
                    amount = 250.00,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.UTILITIES,
                    dateMillis = now - (day * 4),
                    note = "Market data & news feed"
                )
            )

            defaultTransactions.forEach { transactionDao.insertTransaction(it) }
        }

        if (budgetDao.getBudgetCount() == 0) {
            val defaultBudgets = listOf(
                BudgetEntity(category = TransactionCategory.INVESTMENT, monthlyLimit = 15000.0),
                BudgetEntity(category = TransactionCategory.UTILITIES, monthlyLimit = 500.0)
            )
            defaultBudgets.forEach { budgetDao.insertOrUpdateBudget(it) }
        }

        // Seed initial trading signals if empty
        seedTradingSignalsAndJournal()
    }

    private suspend fun seedTradingSignalsAndJournal() {
        val now = System.currentTimeMillis()
        val hour = 3600000L

        val initialSignals = listOf(
            TradingSignalEntity(
                timestamp = now - (hour * 1),
                symbol = "BTC/USD",
                timeframe = "15m",
                action = "BUY",
                confidenceScore = 92,
                reasoning = "High probability liquidity sweep at $95,200 followed by strong bullish engulfing candle on high volume. 20 EMA crossed above 50 EMA with RSI bullish divergence.",
                entryZone = "$95,800 - $96,200",
                stopLoss = "$95,100 (-0.8%)",
                takeProfit = "$98,400 (+2.6%)",
                riskRewardRatio = "1:3.2",
                trendDirection = "BULLISH",
                detectedPatterns = "Liquidity Sweep, Bullish Engulfing, 20/50 EMA Golden Cross",
                keyLevels = "Support: $95,100 | Resistance: $98,400 | Order Block: $95,800",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 890.00
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 4),
                symbol = "NVDA",
                timeframe = "1h",
                action = "BUY",
                confidenceScore = 88,
                reasoning = "Classic 1h Double Bottom formation resting on key 200 EMA support. Bullish MACD crossover in sub-zero territory with expanding green histogram bars.",
                entryZone = "$128.50 - $129.20",
                stopLoss = "$127.10 (-1.2%)",
                takeProfit = "$133.80 (+3.9%)",
                riskRewardRatio = "1:3.1",
                trendDirection = "BULLISH",
                detectedPatterns = "Double Bottom, 200 EMA Support Bounce, MACD Bullish Crossover",
                keyLevels = "Support: $127.10 | Resistance: $134.00 | Pivot: $129.00",
                source = "CAMERA_ANALYSIS",
                tradeOutcome = "WIN",
                actualProfitLoss = 1420.50
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 8),
                symbol = "EUR/USD",
                timeframe = "5m",
                action = "SELL",
                confidenceScore = 84,
                reasoning = "Clean rejection at 1.0920 daily supply zone forming a bearish pinbar. VWAP resistance holding firmly with declining buy volume momentum.",
                entryZone = "1.0915 - 1.0922",
                stopLoss = "1.0935 (+15 pips)",
                takeProfit = "1.0870 (-45 pips)",
                riskRewardRatio = "1:3.0",
                trendDirection = "BEARISH",
                detectedPatterns = "Bearish Pinbar, Supply Zone Rejection, VWAP Breakdown",
                keyLevels = "Supply: 1.0925 | Demand: 1.0865 | VWAP: 1.0918",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "PENDING",
                actualProfitLoss = 0.0
            ),
            TradingSignalEntity(
                timestamp = now - (hour * 12),
                symbol = "AAPL",
                timeframe = "15m",
                action = "WAIT",
                confidenceScore = 71,
                reasoning = "Price is compressing inside a tight symmetrical triangle between $224.00 and $226.50. Await clean high-volume candle close outside boundary before taking position.",
                entryZone = "Wait for $226.80 Breakout",
                stopLoss = "$223.80",
                takeProfit = "$231.50",
                riskRewardRatio = "1:2.3",
                trendDirection = "SIDEWAYS",
                detectedPatterns = "Symmetrical Triangle, Volatility Squeeze",
                keyLevels = "Breakout Level: $226.80 | Breakdown Level: $223.80",
                source = "SCREEN_ANALYSIS",
                tradeOutcome = "PENDING",
                actualProfitLoss = 0.0
            )
        )

        initialSignals.forEach { tradingSignalDao.insertSignal(it) }

        val initialJournalTrades = listOf(
            TradeJournalEntity(
                assetSymbol = "NVDA",
                tradeType = "BUY",
                entryPrice = 128.80,
                exitPrice = 133.50,
                stopLoss = 127.10,
                takeProfit = 133.80,
                positionSize = 300.0,
                status = "CLOSED_WIN",
                pnl = 1410.00,
                notes = "Followed Cash Flow AI 1h Double Bottom signal. Took profit right near resistance level.",
                chartPattern = "Double Bottom"
            ),
            TradeJournalEntity(
                assetSymbol = "BTC/USD",
                tradeType = "BUY",
                entryPrice = 96000.00,
                exitPrice = 98200.00,
                stopLoss = 95100.00,
                takeProfit = 98400.00,
                positionSize = 0.4,
                status = "CLOSED_WIN",
                pnl = 880.00,
                notes = "Captured 15m Liquidity Sweep signal. Execution was fast and precise.",
                chartPattern = "Liquidity Sweep"
            ),
            TradeJournalEntity(
                assetSymbol = "EUR/USD",
                tradeType = "SELL",
                entryPrice = 1.0918,
                exitPrice = null,
                stopLoss = 1.0935,
                takeProfit = 1.0870,
                positionSize = 50000.0,
                status = "OPEN",
                pnl = 120.00,
                notes = "Trade active from live screen HUD signal.",
                chartPattern = "Bearish Pinbar"
            )
        )

        initialJournalTrades.forEach { tradeJournalDao.insertTrade(it) }
    }
}

