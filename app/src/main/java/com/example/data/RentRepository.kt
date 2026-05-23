package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RentRepository(private val rentDao: RentDao) {
    
    // Tenants
    val activeTenants: Flow<List<Tenant>> = rentDao.getActiveTenants()
    
    suspend fun getTenant(id: Long): Tenant? = rentDao.getTenantById(id)
    
    suspend fun saveTenant(tenant: Tenant): Long = rentDao.insertTenant(tenant)
    
    suspend fun deleteTenant(tenant: Tenant) {
        // We can do a soft delete to avoid losing related bills or hard delete if there are none.
        // Let's mark it inactive so bills of inactive tenants can still be viewed in history.
        rentDao.updateTenant(tenant.copy(isActive = false))
    }
    
    // Bills
    val allBills: Flow<List<MonthlyBill>> = rentDao.getAllBills()
    
    fun getBillsByTenant(tenantId: Long): Flow<List<MonthlyBill>> = rentDao.getBillsByTenant(tenantId)
    
    suspend fun getBill(id: Long): MonthlyBill? = rentDao.getBillById(id)
    
    suspend fun getLastBillForTenant(tenantId: Long): MonthlyBill? = rentDao.getLastBillForTenant(tenantId)
    
    suspend fun saveBill(bill: MonthlyBill): Long = rentDao.insertBill(bill)
    
    suspend fun deleteBill(bill: MonthlyBill) = rentDao.deleteBill(bill)
    
    // Statistics
    val totalRentCollected: Flow<Double> = rentDao.getTotalRentCollectedFlow().map { it ?: 0.0 }
    val pendingPayments: Flow<Double> = rentDao.getPendingPaymentsFlow().map { it ?: 0.0 }
    
    // Settings
    val settings: Flow<AppSettings> = rentDao.getSettingsFlow().map { it ?: AppSettings() }
    
    suspend fun getSettingsDirect(): AppSettings = rentDao.getSettingsDirect() ?: AppSettings()
    
    suspend fun saveSettings(settings: AppSettings) = rentDao.insertSettings(settings)
}
