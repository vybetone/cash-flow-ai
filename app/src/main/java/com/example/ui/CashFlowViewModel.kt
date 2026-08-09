package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BudgetEntity
import com.example.data.CashFlowRepository
import com.example.data.SignalRepository
import com.example.data.TradingSignal
import com.example.data.TradeJournalEntity
import com.example.data.TradingSignalEntity
import com.example.data.TransactionCategory
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CashFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CashFlowRepository
    private val aiSignalRepository: SignalRepository = SignalRepository()

    val highConfidenceAiSignal: StateFlow<TradingSignal?> = aiSignalRepository.latestHighConfidenceSignal

    val allSignals: StateFlow<List<TradingSignalEntity>>
    val allJournalTrades: StateFlow<List<TradeJournalEntity>>
    val totalJournalPnL: StateFlow<Double?>
    val winCount: StateFlow<Int>
    val totalClosedTradesCount: StateFlow<Int>

    val allTransactions: StateFlow<List<TransactionEntity>>
    val allBudgets: StateFlow<List<BudgetEntity>>

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _signalFilter = MutableStateFlow("ALL") // "ALL", "BUY", "SELL", "WAIT"
    val signalFilter: StateFlow<String> = _signalFilter.asStateFlow()

    private val _isAnalyzingChart = MutableStateFlow(false)
    val isAnalyzingChart: StateFlow<Boolean> = _isAnalyzingChart.asStateFlow()

    private val _latestActiveSignal = MutableStateFlow<TradingSignalEntity?>(null)
    val latestActiveSignal: StateFlow<TradingSignalEntity?> = _latestActiveSignal.asStateFlow()

    private val _preferredModel = MutableStateFlow("gemini-3.5-flash")
    val preferredModel: StateFlow<String> = _preferredModel.asStateFlow()

    private val _autoCaptureIntervalSeconds = MutableStateFlow(5)
    val autoCaptureIntervalSeconds: StateFlow<Int> = _autoCaptureIntervalSeconds.asStateFlow()

    private val _isScreenCaptureActive = MutableStateFlow(false)
    val isScreenCaptureActive: StateFlow<Boolean> = _isScreenCaptureActive.asStateFlow()

    private val _showAddJournalDialog = MutableStateFlow(false)
    val showAddJournalDialog: StateFlow<Boolean> = _showAddJournalDialog.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)
    val selectedCategoryFilter: StateFlow<TransactionCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _showAddTransactionSheet = MutableStateFlow(false)
    val showAddTransactionSheet: StateFlow<Boolean> = _showAddTransactionSheet.asStateFlow()

    private val _transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val transactionToEdit: StateFlow<TransactionEntity?> = _transactionToEdit.asStateFlow()

    private val _showAddBudgetDialog = MutableStateFlow(false)
    val showAddBudgetDialog: StateFlow<Boolean> = _showAddBudgetDialog.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CashFlowRepository(
            tradingSignalDao = database.tradingSignalDao(),
            tradeJournalDao = database.tradeJournalDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao()
        )

        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }

        allSignals = repository.allSignals.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allJournalTrades = repository.allJournalTrades.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        totalJournalPnL = repository.totalJournalPnL.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

        winCount = repository.winCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        totalClosedTradesCount = repository.totalClosedTradesCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        allTransactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allBudgets = repository.allBudgets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    val filteredSignals: StateFlow<List<TradingSignalEntity>> = combine(
        allSignals,
        _searchQuery,
        _signalFilter
    ) { signals, query, actionFilter ->
        signals.filter { sig ->
            val matchesQuery = query.isBlank() ||
                    sig.symbol.contains(query, ignoreCase = true) ||
                    sig.reasoning.contains(query, ignoreCase = true) ||
                    sig.detectedPatterns.contains(query, ignoreCase = true)
            val matchesAction = actionFilter == "ALL" || sig.action.equals(actionFilter, ignoreCase = true)
            matchesQuery && matchesAction
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedCategoryFilter
    ) { transactions, query, typeFilter, categoryFilter ->
        transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.category.displayName.contains(query, ignoreCase = true)
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = categoryFilter == null || tx.category == categoryFilter
            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSignalFilter(filter: String) {
        _signalFilter.value = filter
    }

    fun setPreferredModel(model: String) {
        _preferredModel.value = model
    }

    fun setAutoCaptureInterval(seconds: Int) {
        _autoCaptureIntervalSeconds.value = seconds
    }

    fun startScreenCaptureService(context: android.content.Context) {
        _isScreenCaptureActive.value = true
        com.example.data.ScreenCaptureService.startService(context)
    }

    fun stopScreenCaptureService(context: android.content.Context) {
        _isScreenCaptureActive.value = false
        com.example.data.ScreenCaptureService.stopService(context)
    }

    fun onMediaProjectionGranted(context: android.content.Context, resultCode: Int, data: android.content.Intent?) {
        startScreenCaptureService(context)
    }

    fun onMediaProjectionDenied() {
        _isScreenCaptureActive.value = false
    }

    fun toggleScreenCaptureService(context: android.content.Context) {
        if (_isScreenCaptureActive.value) {
            stopScreenCaptureService(context)
        } else {
            startScreenCaptureService(context)
        }
    }

    fun processChartFrameBitmap(
        bitmap: Bitmap,
        source: String = "SCREEN_ANALYSIS"
    ) {
        viewModelScope.launch {
            _isAnalyzingChart.value = true
            try {
                val newSignal = repository.analyzeAndSaveSignal(
                    bitmap = bitmap,
                    context = getApplication(),
                    source = source,
                    preferredModel = _preferredModel.value
                )
                _latestActiveSignal.value = newSignal
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAnalyzingChart.value = false
            }
        }
    }

    fun deleteSignal(signal: TradingSignalEntity) {
        viewModelScope.launch {
            repository.deleteSignal(signal.id)
        }
    }

    fun convertSignalToJournalEntry(signal: TradingSignalEntity) {
        viewModelScope.launch {
            val entryPrice = signal.entryZone.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 100.0
            val slPrice = signal.stopLoss.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: (entryPrice * 0.98)
            val tpPrice = signal.takeProfit.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: (entryPrice * 1.04)

            val trade = TradeJournalEntity(
                signalId = signal.id,
                assetSymbol = signal.symbol,
                tradeType = if (signal.action == "SELL") "SELL" else "BUY",
                entryPrice = entryPrice,
                exitPrice = null,
                stopLoss = slPrice,
                takeProfit = tpPrice,
                positionSize = 1.0,
                status = "OPEN",
                pnl = 0.0,
                notes = "Converted from AI Signal: ${signal.detectedPatterns}",
                screenshotPath = signal.imageUri,
                chartPattern = signal.detectedPatterns
            )
            repository.insertJournalTrade(trade)
        }
    }

    fun recalculateHighConfidenceSignal(symbol: String = "BTC/USD") {
        viewModelScope.launch {
            aiSignalRepository.generateSignal(symbol)
        }
    }

    fun convertAiSignalToJournalEntry(signal: TradingSignal) {
        viewModelScope.launch {
            val trade = TradeJournalEntity(
                assetSymbol = signal.symbol,
                tradeType = if (signal.signalType == "SELL") "SELL" else "BUY",
                entryPrice = signal.entryPrice,
                exitPrice = null,
                stopLoss = signal.stopLoss,
                takeProfit = signal.takeProfit,
                positionSize = 1.0,
                status = "OPEN",
                pnl = 0.0,
                notes = "Converted from Strong AI Signal (${signal.confidencePercentage}%): ${signal.analysisReason}",
                chartPattern = signal.candlePattern
            )
            repository.insertJournalTrade(trade)
        }
    }

    fun saveJournalTrade(
        symbol: String,
        tradeType: String,
        entryPrice: Double,
        exitPrice: Double?,
        stopLoss: Double,
        takeProfit: Double,
        positionSize: Double,
        status: String,
        notes: String
    ) {
        viewModelScope.launch {
            val pnl = if (status.startsWith("CLOSED") && exitPrice != null) {
                if (tradeType == "BUY") (exitPrice - entryPrice) * positionSize else (entryPrice - exitPrice) * positionSize
            } else 0.0

            val trade = TradeJournalEntity(
                assetSymbol = symbol.trim().ifEmpty { "CUSTOM_ASSET" },
                tradeType = tradeType,
                entryPrice = entryPrice,
                exitPrice = exitPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                positionSize = positionSize,
                status = status,
                pnl = pnl,
                notes = notes.trim()
            )
            repository.insertJournalTrade(trade)
            _showAddJournalDialog.value = false
        }
    }

    fun deleteJournalTrade(trade: TradeJournalEntity) {
        viewModelScope.launch {
            repository.deleteJournalTrade(trade.id)
        }
    }

    fun openAddJournalDialog() {
        _showAddJournalDialog.value = true
    }

    fun closeAddJournalDialog() {
        _showAddJournalDialog.value = false
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun openAddTransactionSheet(tx: TransactionEntity? = null) {
        _transactionToEdit.value = tx
        _showAddTransactionSheet.value = true
    }

    fun closeAddTransactionSheet() {
        _showAddTransactionSheet.value = false
        _transactionToEdit.value = null
    }

    fun openAddBudgetDialog() {
        _showAddBudgetDialog.value = true
    }

    fun closeAddBudgetDialog() {
        _showAddBudgetDialog.value = false
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        note: String,
        isRecurring: Boolean
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val existing = _transactionToEdit.value
            if (existing != null) {
                repository.updateTransaction(
                    existing.copy(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        category = category,
                        note = note.trim(),
                        isRecurring = isRecurring
                    )
                )
            } else {
                repository.insertTransaction(
                    TransactionEntity(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        category = category,
                        note = note.trim(),
                        isRecurring = isRecurring
                    )
                )
            }
            closeAddTransactionSheet()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun saveBudget(category: TransactionCategory, monthlyLimit: Double) {
        if (monthlyLimit <= 0.0) return
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit
                )
            )
            closeAddBudgetDialog()
        }
    }
}

