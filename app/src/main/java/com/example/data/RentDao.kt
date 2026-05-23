package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RentDao {
    // Tenants Queries
    @Query("SELECT * FROM tenants WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveTenants(): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getTenantById(id: Long): Tenant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant): Long

    @Update
    suspend fun updateTenant(tenant: Tenant)

    @Delete
    suspend fun deleteTenant(tenant: Tenant)

    // Bills Queries
    @Query("SELECT * FROM monthly_bills ORDER BY billMonth DESC, dateCreated DESC")
    fun getAllBills(): Flow<List<MonthlyBill>>

    @Query("SELECT * FROM monthly_bills WHERE tenantId = :tenantId ORDER BY billMonth DESC")
    fun getBillsByTenant(tenantId: Long): Flow<List<MonthlyBill>>

    @Query("SELECT * FROM monthly_bills WHERE id = :id")
    suspend fun getBillById(id: Long): MonthlyBill?

    @Query("SELECT * FROM monthly_bills WHERE tenantId = :tenantId ORDER BY billMonth DESC LIMIT 1")
    suspend fun getLastBillForTenant(tenantId: Long): MonthlyBill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: MonthlyBill): Long

    @Update
    suspend fun updateBill(bill: MonthlyBill)

    @Delete
    suspend fun deleteBill(bill: MonthlyBill)

    // General Calculations
    @Query("SELECT SUM(calculatedTotalAmount) FROM monthly_bills WHERE paymentStatus = 'PAID'")
    fun getTotalRentCollectedFlow(): Flow<Double?>

    @Query("SELECT SUM(calculatedTotalAmount - paidAmount) FROM monthly_bills WHERE paymentStatus = 'PENDING'")
    fun getPendingPaymentsFlow(): Flow<Double?>

    // App Settings Queries
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}
