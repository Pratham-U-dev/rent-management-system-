package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RentViewModel(private val repository: RentRepository) : ViewModel() {

    // --- Database Streams ---
    val tenants: StateFlow<List<Tenant>> = repository.activeTenants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<MonthlyBill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val totalRentCollected: StateFlow<Double> = bills
        .map { list -> list.sumOf { it.paidAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingPayments: StateFlow<Double> = bills
        .map { list ->
            val grouped = list.groupBy { it.tenantId }
            var totalPending = 0.0
            for ((tenantId, tenantBills) in grouped) {
                val summaries = getPaymentSummariesForTenant(tenantId, list)
                val latestBill = tenantBills.maxByOrNull { it.billMonth }
                if (latestBill != null) {
                    val summary = summaries[latestBill.id]
                    if (summary != null) {
                        totalPending += summary.remainingDue
                    }
                }
            }
            totalPending
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- UI Navigation State ---
    // Screens: "dashboard", "tenants", "bills", "add_tenant", "add_bill", "settings", "bill_details"
    var currentScreen by mutableStateOf("dashboard")
        private set

    var selectedBillIdForDetails by mutableStateOf<Long?>(null)
        private set

    fun navigateTo(screen: String, billId: Long? = null) {
        if (screen == "bill_details" && billId != null) {
            selectedBillIdForDetails = billId
        }
        currentScreen = screen
    }

    // --- Tenant Form State ---
    var tenantFormId by mutableStateOf<Long?>(null)
        private set
    var tenantFormName by mutableStateOf("")
    var tenantFormPhone by mutableStateOf("")
    var tenantFormRoom by mutableStateOf("")
    var tenantFormRent by mutableStateOf("")
    var tenantFormDeposit by mutableStateOf("")
    var tenantFormStartDate by mutableStateOf("")
    var tenantFormNotes by mutableStateOf("")
    var tenantFormPersons by mutableStateOf("1")

    fun initTenantForm(tenant: Tenant? = null) {
        if (tenant != null) {
            tenantFormId = tenant.id
            tenantFormName = tenant.name
            tenantFormPhone = tenant.phoneNumber
            tenantFormRoom = tenant.roomName
            tenantFormRent = tenant.rentAmount.toString()
            tenantFormDeposit = tenant.depositAmount.toString()
            tenantFormStartDate = tenant.occupancyStartDate
            tenantFormNotes = tenant.notes
            tenantFormPersons = tenant.numberOfPersons.toString()
        } else {
            tenantFormId = null
            tenantFormName = ""
            tenantFormPhone = ""
            tenantFormRoom = ""
            tenantFormRent = ""
            tenantFormDeposit = ""
            tenantFormStartDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            tenantFormNotes = ""
            tenantFormPersons = "1"
        }
    }

    fun saveTenant() {
        val rent = tenantFormRent.toDoubleOrNull() ?: 0.0
        val deposit = tenantFormDeposit.toDoubleOrNull() ?: 0.0
        val persons = tenantFormPersons.toIntOrNull() ?: 1
        
        val tenant = Tenant(
            id = tenantFormId ?: 0,
            name = tenantFormName,
            phoneNumber = tenantFormPhone,
            roomName = tenantFormRoom,
            rentAmount = rent,
            depositAmount = deposit,
            occupancyStartDate = tenantFormStartDate,
            notes = tenantFormNotes,
            numberOfPersons = persons,
            isActive = true
        )

        viewModelScope.launch {
            repository.saveTenant(tenant)
            navigateTo("tenants")
        }
    }

    fun deleteTenant(tenant: Tenant) {
        viewModelScope.launch {
            repository.deleteTenant(tenant)
        }
    }

    // --- Settings Form State ---
    var settingsFormWaterSplit by mutableStateOf("EQUAL")
    var settingsFormTaxPercent by mutableStateOf("9.0")
    var settingsFormFixedCharges by mutableStateOf("150.0")
    var settingsFormCurrency by mutableStateOf("₹")
    var settingsFormOwnerName by mutableStateOf("")
    var settingsFormOwnerPhone by mutableStateOf("")
    var settingsFormWaterRate by mutableStateOf("100.0")

    fun initSettingsForm(currentSec: AppSettings) {
        settingsFormWaterSplit = currentSec.defaultWaterSplitRule
        settingsFormTaxPercent = currentSec.defaultElectricityTaxPercent.toString()
        settingsFormFixedCharges = currentSec.defaultFixedCharges.toString()
        settingsFormCurrency = currentSec.currencySymbol
        settingsFormOwnerName = currentSec.ownerName
        settingsFormOwnerPhone = currentSec.ownerPhone
        settingsFormWaterRate = currentSec.defaultWaterRatePerPerson.toString()
    }

    fun saveSettings() {
        val tax = settingsFormTaxPercent.toDoubleOrNull() ?: 9.0
        val fixed = settingsFormFixedCharges.toDoubleOrNull() ?: 150.0
        val waterRate = settingsFormWaterRate.toDoubleOrNull() ?: 100.0
        
        val newSettings = AppSettings(
            id = 1,
            defaultWaterSplitRule = settingsFormWaterSplit,
            defaultElectricityTaxPercent = tax,
            defaultFixedCharges = fixed,
            currencySymbol = settingsFormCurrency,
            ownerName = settingsFormOwnerName,
            ownerPhone = settingsFormOwnerPhone,
            defaultWaterRatePerPerson = waterRate
        )
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            navigateTo("dashboard")
        }
    }

    // --- Bill Form State ---
    var billFormId by mutableStateOf<Long?>(null)
    var billFormTenantId by mutableStateOf<Long>(0L)
    var billFormMonth by mutableStateOf("") // Format "yyyy-MM" like "2026-05"
    
    var billFormMainPrev by mutableStateOf("0")
    var billFormMainCurr by mutableStateOf("0")
    var billFormTenantPrev by mutableStateOf("0")
    var billFormTenantCurr by mutableStateOf("0")
    
    var billFormBoardBill by mutableStateOf("0")
    var billFormFixedCharges by mutableStateOf("150")
    var billFormTaxAmount by mutableStateOf("0")
    var billFormOtherAdjustments by mutableStateOf("0")
    
    var billFormWaterAmount by mutableStateOf("0")
    var billFormWaterSplitRule by mutableStateOf("EQUAL")
    var billFormTenantWaterCustom by mutableStateOf("0")
    
    var billFormOtherCharges by mutableStateOf("0")
    var billFormMaintenance by mutableStateOf("0")
    var billFormRent by mutableStateOf("0")
    
    var billFormCalcMode by mutableStateOf("PROPORTIONAL") // "PROPORTIONAL", "CUSTOM"
    var billFormCustomElectricityRate by mutableStateOf("0")
    var billFormIncludeTax by mutableStateOf(true)
    var billFormExcludeFixed by mutableStateOf(false)
    var billFormDivideOnlyEnergy by mutableStateOf(false)
    var billFormManualOverrideElec by mutableStateOf("0")
    var billFormNotes by mutableStateOf("")
    var billFormPaidAmount by mutableStateOf("0")
    var billFormPaymentStatus by mutableStateOf("PENDING")
    var billFormPaymentMethod by mutableStateOf("Cash") // Cash, UPI, Bank Transfer, Other

    fun initBillForm(bill: MonthlyBill? = null) {
        val cal = Calendar.getInstance()
        val currentMonthString = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        
        if (bill != null) {
            billFormId = bill.id
            billFormTenantId = bill.tenantId
            billFormMonth = bill.billMonth
            billFormMainPrev = bill.mainMeterPrev.toString()
            billFormMainCurr = bill.mainMeterCurr.toString()
            billFormTenantPrev = bill.tenantSubmeterPrev.toString()
            billFormTenantCurr = bill.tenantSubmeterCurr.toString()
            billFormBoardBill = bill.electricityBoardBillAmount.toString()
            billFormFixedCharges = bill.fixedChargesAmount.toString()
            billFormTaxAmount = bill.taxAmount.toString()
            billFormOtherAdjustments = bill.otherAdjustmentsAmount.toString()
            billFormWaterAmount = bill.waterBillAmount.toString()
            billFormWaterSplitRule = bill.waterSplitRule
            billFormTenantWaterCustom = bill.tenantWaterCustomAmount.toString()
            billFormOtherCharges = bill.otherCharges.toString()
            billFormMaintenance = bill.maintenanceCharges.toString()
            billFormRent = bill.rentAmount.toString()
            billFormCalcMode = bill.calculationMode
            billFormCustomElectricityRate = bill.customElectricityRate.toString()
            billFormIncludeTax = bill.includeTaxInDivision
            billFormExcludeFixed = bill.excludeFixedCharges
            billFormDivideOnlyEnergy = bill.divideOnlyEnergyCharges
            billFormManualOverrideElec = bill.manualOverrideAmount.toString()
            billFormNotes = bill.notes
            billFormPaidAmount = bill.paidAmount.toString()
            billFormPaymentStatus = bill.paymentStatus
            billFormPaymentMethod = bill.paymentMethod
        } else {
            val appSettings = settings.value
            billFormId = null
            billFormTenantId = tenants.value.firstOrNull()?.id ?: 0L
            billFormMonth = currentMonthString
            billFormMainPrev = "0"
            billFormMainCurr = "0"
            billFormTenantPrev = "0"
            billFormTenantCurr = "0"
            billFormBoardBill = "0"
            billFormFixedCharges = appSettings.defaultFixedCharges.toString()
            billFormTaxAmount = "0"
            billFormOtherAdjustments = "0"
            billFormWaterAmount = "0"
            billFormWaterSplitRule = appSettings.defaultWaterSplitRule
            billFormTenantWaterCustom = "0"
            billFormOtherCharges = "0"
            billFormMaintenance = "0"
            billFormRent = "0"
            billFormCalcMode = appSettings.calculationPreferences
            billFormCustomElectricityRate = "0"
            billFormIncludeTax = true
            billFormExcludeFixed = false
            billFormDivideOnlyEnergy = false
            billFormManualOverrideElec = "0"
            billFormNotes = ""
            billFormPaidAmount = "0"
            billFormPaymentStatus = "PENDING"
            billFormPaymentMethod = "Cash"
            
            // Auto fill based on first tenant if available
            if (billFormTenantId != 0L) {
                onTenantSelectedForBill(billFormTenantId)
            }
        }
    }

    fun onTenantSelectedForBill(tenantId: Long) {
        billFormTenantId = tenantId
        viewModelScope.launch {
            val tenant = repository.getTenant(tenantId)
            if (tenant != null) {
                billFormRent = tenant.rentAmount.toString()
            }
            
            val lastBill = repository.getLastBillForTenant(tenantId)
            if (lastBill != null) {
                // AUTOFILL previous month readings from last month's final readings
                billFormMainPrev = lastBill.mainMeterCurr.toString()
                billFormTenantPrev = lastBill.tenantSubmeterCurr.toString()
                // Encourage normal incrementing behavior
                billFormMainCurr = lastBill.mainMeterCurr.toString()
                billFormTenantCurr = lastBill.tenantSubmeterCurr.toString()
                
                // Prefill previous preferences
                billFormCalcMode = lastBill.calculationMode
                billFormIncludeTax = lastBill.includeTaxInDivision
                billFormExcludeFixed = lastBill.excludeFixedCharges
                billFormDivideOnlyEnergy = lastBill.divideOnlyEnergyCharges
                billFormWaterSplitRule = lastBill.waterSplitRule
            } else {
                billFormMainPrev = "0"
                billFormMainCurr = "0"
                billFormTenantPrev = "0"
                billFormTenantCurr = "0"
            }
        }
    }

    // --- Live Breakdown Display Computations ---
    val liveElectricityBreakdown: Flow<CalculationsEngine.ElectricityBreakdown> = snapshotFlow {
        MonthlyBill(
            tenantId = billFormTenantId,
            billMonth = billFormMonth,
            mainMeterPrev = billFormMainPrev.toDoubleOrNull() ?: 0.0,
            mainMeterCurr = billFormMainCurr.toDoubleOrNull() ?: 0.0,
            tenantSubmeterPrev = billFormTenantPrev.toDoubleOrNull() ?: 0.0,
            tenantSubmeterCurr = billFormTenantCurr.toDoubleOrNull() ?: 0.0,
            electricityBoardBillAmount = billFormBoardBill.toDoubleOrNull() ?: 0.0,
            fixedChargesAmount = billFormFixedCharges.toDoubleOrNull() ?: 0.0,
            taxAmount = billFormTaxAmount.toDoubleOrNull() ?: 0.0,
            otherAdjustmentsAmount = billFormOtherAdjustments.toDoubleOrNull() ?: 0.0,
            calculationMode = billFormCalcMode,
            customElectricityRate = billFormCustomElectricityRate.toDoubleOrNull() ?: 0.0,
            includeTaxInDivision = billFormIncludeTax,
            excludeFixedCharges = billFormExcludeFixed,
            divideOnlyEnergyCharges = billFormDivideOnlyEnergy,
            manualOverrideAmount = billFormManualOverrideElec.toDoubleOrNull() ?: 0.0
        )
    }.map { bill ->
        val appSettings = settings.value
        val tenant = tenants.value.find { it.id == bill.tenantId }
        val persons = tenant?.numberOfPersons ?: 1
        CalculationsEngine.calculateElectricity(bill, persons, appSettings.defaultElectricityTaxPercent)
    }

    val liveWaterBreakdown: Flow<CalculationsEngine.WaterBreakdown> = snapshotFlow {
        MonthlyBill(
            tenantId = billFormTenantId,
            billMonth = billFormMonth,
            waterBillAmount = billFormWaterAmount.toDoubleOrNull() ?: 0.0,
            waterSplitRule = billFormWaterSplitRule,
            tenantWaterCustomAmount = billFormTenantWaterCustom.toDoubleOrNull() ?: 0.0
        )
    }.map { bill ->
        val tenant = tenants.value.find { it.id == bill.tenantId }
        val persons = tenant?.numberOfPersons ?: 1
        val activeCount = tenants.value.filter { it.isActive }.size
        CalculationsEngine.calculateWater(bill, persons, activeCount)
    }

    fun saveBill() {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            val tenant = repository.getTenant(billFormTenantId)
            val persons = tenant?.numberOfPersons ?: 1
            val activeCount = tenants.value.filter { it.isActive }.size
            
            // Build the initial draft bill
            val tempBill = MonthlyBill(
                id = billFormId ?: 0,
                tenantId = billFormTenantId,
                billMonth = billFormMonth,
                mainMeterPrev = billFormMainPrev.toDoubleOrNull() ?: 0.0,
                mainMeterCurr = billFormMainCurr.toDoubleOrNull() ?: 0.0,
                tenantSubmeterPrev = billFormTenantPrev.toDoubleOrNull() ?: 0.0,
                tenantSubmeterCurr = billFormTenantCurr.toDoubleOrNull() ?: 0.0,
                electricityBoardBillAmount = billFormBoardBill.toDoubleOrNull() ?: 0.0,
                fixedChargesAmount = billFormFixedCharges.toDoubleOrNull() ?: 0.0,
                taxAmount = billFormTaxAmount.toDoubleOrNull() ?: 0.0,
                otherAdjustmentsAmount = billFormOtherAdjustments.toDoubleOrNull() ?: 0.0,
                waterBillAmount = billFormWaterAmount.toDoubleOrNull() ?: 0.0,
                waterSplitRule = billFormWaterSplitRule,
                tenantWaterCustomAmount = billFormTenantWaterCustom.toDoubleOrNull() ?: 0.0,
                otherCharges = billFormOtherCharges.toDoubleOrNull() ?: 0.0,
                maintenanceCharges = billFormMaintenance.toDoubleOrNull() ?: 0.0,
                rentAmount = billFormRent.toDoubleOrNull() ?: 0.0,
                calculationMode = billFormCalcMode,
                customElectricityRate = billFormCustomElectricityRate.toDoubleOrNull() ?: 0.0,
                includeTaxInDivision = billFormIncludeTax,
                excludeFixedCharges = billFormExcludeFixed,
                divideOnlyEnergyCharges = billFormDivideOnlyEnergy,
                manualOverrideAmount = billFormManualOverrideElec.toDoubleOrNull() ?: 0.0,
                notes = billFormNotes,
                paidAmount = 0.0, // Calculated below
                paymentStatus = "PENDING", // Calculated below
                paymentDate = null,
                paymentMethod = billFormPaymentMethod
            )

            // Dynamic current month calculations
            val elec = CalculationsEngine.calculateElectricity(tempBill, persons, s.defaultElectricityTaxPercent)
            val water = CalculationsEngine.calculateWater(tempBill, persons, activeCount)
            val currentCharges = tempBill.rentAmount + elec.finalElectricityAmount + water.finalWaterAmount + tempBill.otherCharges + tempBill.maintenanceCharges

            // Calculate historical carry-overs
            val prevPending = getPreviousPendingDues(billFormTenantId, billFormMonth)
            val totalOutstanding = currentCharges + prevPending

            // Formulate what was paid and the corresponding status
            val calculatedPaidAmount = if (billFormPaymentStatus == "PAID") {
                totalOutstanding
            } else {
                billFormPaidAmount.toDoubleOrNull() ?: 0.0
            }

            val calculatedStatus = when {
                calculatedPaidAmount >= totalOutstanding -> "PAID"
                calculatedPaidAmount > 0.0 -> "PARTIAL"
                else -> "PENDING"
            }

            val finalBill = tempBill.copy(
                calculatedTenantElectricity = elec.finalElectricityAmount,
                calculatedTenantWater = water.finalWaterAmount,
                calculatedTotalAmount = currentCharges,
                paidAmount = calculatedPaidAmount,
                paymentStatus = calculatedStatus,
                paymentDate = if (calculatedStatus != "PENDING") System.currentTimeMillis() else null
            )

            repository.saveBill(finalBill)
            navigateTo("bills")
        }
    }

    fun deleteBill(bill: MonthlyBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun updatePaymentStatus(bill: MonthlyBill, status: String, paidAmt: Double) {
        viewModelScope.launch {
            val summary = getSummaryForBill(bill.id)
            val totalOutstanding = summary?.totalOutstanding ?: bill.calculatedTotalAmount
            
            val updatedPaidAmount = if (status == "PAID") totalOutstanding else paidAmt
            val updatedStatus = when {
                updatedPaidAmount >= totalOutstanding -> "PAID"
                updatedPaidAmount > 0.0 -> "PARTIAL"
                else -> "PENDING"
            }

            val updated = bill.copy(
                paymentStatus = updatedStatus,
                paidAmount = updatedPaidAmount,
                paymentDate = if (updatedStatus != "PENDING") System.currentTimeMillis() else null,
                paymentMethod = if (updatedStatus != "PENDING") "Cash" else bill.paymentMethod
            )
            repository.saveBill(updated)
        }
    }

    fun updateBillPaymentDetails(bill: MonthlyBill, status: String, paidAmt: Double, method: String, notes: String) {
        viewModelScope.launch {
            val updated = bill.copy(
                paymentStatus = status,
                paidAmount = paidAmt,
                paymentMethod = method,
                notes = notes,
                paymentDate = if (status != "PENDING") System.currentTimeMillis() else null
            )
            repository.saveBill(updated)
        }
    }

    // --- Shareable Summary Generators ---
    fun getShareableBillText(bill: MonthlyBill, tenant: Tenant, appSettings: AppSettings): String {
        return buildString {
            append("⚡ *Rent & Bills Invoice - ${bill.billMonth}* ⚡\n")
            append("-------------------------------------------\n")
            append("🏠 *Room/Unit:* ${tenant.roomName}\n")
            append("👤 *Tenant:* ${tenant.name}\n\n")
            
            append("💰 *Rent:* ${appSettings.currencySymbol}${bill.rentAmount}\n")
            append("⚡ *Electricity Bill:* ${appSettings.currencySymbol}${bill.calculatedTenantElectricity}\n")
            append("  ├ Current: ${bill.tenantSubmeterCurr} | Prev: ${bill.tenantSubmeterPrev}\n")
            append("  └ Consumed: ${bill.tenantSubmeterCurr - bill.tenantSubmeterPrev} units\n")
            
            if (bill.waterBillAmount > 0) {
                append("💧 *Water share:* ${appSettings.currencySymbol}${bill.calculatedTenantWater}\n")
            }
            if (bill.maintenanceCharges > 0) {
                append("🛠️ *Maintenance:* ${appSettings.currencySymbol}${bill.maintenanceCharges}\n")
            }
            if (bill.otherCharges > 0) {
                append("📝 *Other Charges:* ${appSettings.currencySymbol}${bill.otherCharges}\n")
            }
            
            append("-------------------------------------------\n")
            append("⭐ *TOTAL PAYABLE:* ${appSettings.currencySymbol}${bill.calculatedTotalAmount}\n")
            append("📝 *Status:* ${bill.paymentStatus}\n")
            if (bill.notes.isNotEmpty()) {
                append("🗒️ *Note:* ${bill.notes}\n")
            }
            append("\nGenerated via *RentEase* App")
        }
    }

    fun getPaymentSummariesForTenant(tenantId: Long, allBillsList: List<MonthlyBill> = bills.value): Map<Long, BillPaymentSummary> {
        val tenantBills = allBillsList.filter { it.tenantId == tenantId }.sortedBy { it.billMonth }
        val summaries = mutableMapOf<Long, BillPaymentSummary>()
        var accumulatedPending = 0.0
        for (bill in tenantBills) {
            val prevPending = accumulatedPending
            val currentCharges = bill.calculatedTotalAmount
            val totalOutstanding = currentCharges + prevPending
            val paidAmt = bill.paidAmount
            val remainingDue = if (totalOutstanding > paidAmt) totalOutstanding - paidAmt else 0.0
            
            summaries[bill.id] = BillPaymentSummary(
                billId = bill.id,
                currentCharges = currentCharges,
                previousPending = prevPending,
                totalOutstanding = totalOutstanding,
                paidAmount = paidAmt,
                remainingDue = remainingDue
            )
            accumulatedPending = remainingDue
        }
        return summaries
    }

    fun getSummaryForBill(billId: Long): BillPaymentSummary? {
        val bill = bills.value.find { it.id == billId } ?: return null
        val summaries = getPaymentSummariesForTenant(bill.tenantId)
        return summaries[billId]
    }

    fun getPreviousPendingDues(tenantId: Long, beforeMonth: String): Double {
        val tenantBills = bills.value.filter { it.tenantId == tenantId && it.billMonth < beforeMonth }
        val sortedBills = tenantBills.sortedBy { it.billMonth }
        var accumulatedPending = 0.0
        for (bill in sortedBills) {
            val prevPending = accumulatedPending
            val currentCharges = bill.calculatedTotalAmount
            val totalOutstanding = currentCharges + prevPending
            val paidAmt = bill.paidAmount
            accumulatedPending = if (totalOutstanding > paidAmt) totalOutstanding - paidAmt else 0.0
        }
        return accumulatedPending
    }
}

data class BillPaymentSummary(
    val billId: Long,
    val currentCharges: Double,
    val previousPending: Double,
    val totalOutstanding: Double,
    val paidAmount: Double,
    val remainingDue: Double
)

class RentViewModelFactory(private val repository: RentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
