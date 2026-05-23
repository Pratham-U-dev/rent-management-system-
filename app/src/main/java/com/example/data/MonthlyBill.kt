package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_bills")
data class MonthlyBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val billMonth: String, // Format "yyyy-MM" like "2026-05"
    val dateCreated: Long = System.currentTimeMillis(),
    
    // Meter Readings
    val mainMeterPrev: Double = 0.0,
    val mainMeterCurr: Double = 0.0,
    val tenantSubmeterPrev: Double = 0.0,
    val tenantSubmeterCurr: Double = 0.0,
    
    // Electricity Board Bill Fields
    val electricityBoardBillAmount: Double = 0.0, // main_bill_amount
    val fixedChargesAmount: Double = 0.0, 
    val taxAmount: Double = 0.0, 
    val otherAdjustmentsAmount: Double = 0.0, // surcharges, true up, penalties
    
    // Water Bill Details
    val waterBillAmount: Double = 0.0, 
    val waterSplitRule: String = "EQUAL", // "EQUAL", "MANUAL", "PER_PERSON"
    val tenantWaterCustomAmount: Double = 0.0, // if MANUAL
    
    // Other Monthly Costs
    val otherCharges: Double = 0.0, 
    val maintenanceCharges: Double = 0.0, 
    val rentAmount: Double = 0.0, // default from tenant
    
    // Bill Preferences for Calculation Engine
    val calculationMode: String = "PROPORTIONAL", // "PROPORTIONAL", "CUSTOM"
    val customElectricityRate: Double = 0.0, // if calculationMode == "CUSTOM"
    val includeTaxInDivision: Boolean = true,
    val excludeFixedCharges: Boolean = false,
    val divideOnlyEnergyCharges: Boolean = false,
    val manualOverrideAmount: Double = 0.0, // if landlord wants to manually override tenant's electricity cost
    
    // Cached Calculated Outputs
    val calculatedTenantElectricity: Double = 0.0,
    val calculatedTenantWater: Double = 0.0,
    val calculatedTotalAmount: Double = 0.0,
    
    // Payment Status tracking
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PENDING", // "PENDING", "PAID"
    val paymentDate: Long? = null,
    val notes: String = ""
)
