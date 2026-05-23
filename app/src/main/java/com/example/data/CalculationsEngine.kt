package com.example.data

import kotlin.math.max
import kotlin.math.min

object CalculationsEngine {

    data class ElectricityBreakdown(
        val mainUnits: Double,
        val tenantUnits: Double,
        val tenantFraction: Double,
        val perUnitRate: Double,
        val baseElectricityShare: Double,
        val taxShareAdded: Double,
        val finalElectricityAmount: Double
    )

    data class WaterBreakdown(
        val splitRule: String,
        val totalAmount: Double,
        val tenantPersons: Int,
        val totalPersonsInHouse: Int,
        val finalWaterAmount: Double
    )

    fun calculateElectricity(
        bill: MonthlyBill,
        tenantPersons: Int,
        taxPercent: Double = 9.0
    ): ElectricityBreakdown {
        val mainUnits = max(0.0, bill.mainMeterCurr - bill.mainMeterPrev)
        val tenantUnits = max(0.0, bill.tenantSubmeterCurr - bill.tenantSubmeterPrev)
        
        val tenantFraction = if (mainUnits > 0.0) {
            min(1.0, tenantUnits / mainUnits)
        } else {
            0.0
        }

        var finalElectricityAmount = 0.0
        var perUnitRate = 0.0
        var baseElectricityShare = 0.0
        var taxShareAdded = 0.0

        if (bill.manualOverrideAmount > 0.0) {
            finalElectricityAmount = bill.manualOverrideAmount
            if (tenantUnits > 0.0) {
                perUnitRate = finalElectricityAmount / tenantUnits
            }
        } else if (bill.calculationMode == "CUSTOM") {
            perUnitRate = bill.customElectricityRate
            baseElectricityShare = tenantUnits * perUnitRate
            taxShareAdded = if (bill.includeTaxInDivision) {
                baseElectricityShare * (taxPercent / 100.0)
            } else {
                0.0
            }
            finalElectricityAmount = baseElectricityShare + taxShareAdded
        } else {
            // PROPORTIONAL MODE
            if (mainUnits > 0.0) {
                if (bill.divideOnlyEnergyCharges) {
                    // Divide only estimated pure energy base: Board charges minus fixed charges, taxes, and other adjustments
                    val pureEnergyBase = max(
                        0.0,
                        bill.electricityBoardBillAmount - bill.fixedChargesAmount - bill.taxAmount - bill.otherAdjustmentsAmount
                    )
                    perUnitRate = pureEnergyBase / mainUnits
                    baseElectricityShare = tenantUnits * perUnitRate
                    
                    // Add tax if configured
                    taxShareAdded = if (bill.includeTaxInDivision) {
                        baseElectricityShare * (taxPercent / 100.0)
                    } else {
                        0.0
                    }
                    finalElectricityAmount = baseElectricityShare + taxShareAdded
                } else if (bill.excludeFixedCharges) {
                    // Divide total bill EXCEPT the fixed connection charges
                    val splitBase = max(0.0, bill.electricityBoardBillAmount - bill.fixedChargesAmount)
                    perUnitRate = splitBase / mainUnits
                    baseElectricityShare = tenantUnits * perUnitRate
                    finalElectricityAmount = baseElectricityShare
                } else {
                    // Standard proportional division of the entire board bill
                    perUnitRate = bill.electricityBoardBillAmount / mainUnits
                    baseElectricityShare = tenantUnits * perUnitRate
                    finalElectricityAmount = baseElectricityShare
                }
            }
        }

        return ElectricityBreakdown(
            mainUnits = mainUnits,
            tenantUnits = tenantUnits,
            tenantFraction = tenantFraction,
            perUnitRate = perUnitRate,
            baseElectricityShare = baseElectricityShare,
            taxShareAdded = taxShareAdded,
            finalElectricityAmount = Math.round(finalElectricityAmount * 100.0) / 100.0
        )
    }

    fun calculateWater(
        bill: MonthlyBill,
        tenantPersons: Int,
        totalActiveTenants: Int,
        ownerPersons: Int = 2 // assume owner has 2 family members as default, or configurable
    ): WaterBreakdown {
        var finalWaterAmount = 0.0
        val totalPersons = max(1, tenantPersons + ownerPersons) // owner + current tenant if per person

        when (bill.waterSplitRule) {
            "MANUAL" -> {
                finalWaterAmount = bill.tenantWaterCustomAmount
            }
            "PER_PERSON" -> {
                val perPersonRate = bill.waterBillAmount / totalPersons
                finalWaterAmount = tenantPersons * perPersonRate
            }
            "EQUAL" -> {
                // Divided equally among active tenants + landlord (1)
                val totalParties = max(1, totalActiveTenants + 1)
                finalWaterAmount = bill.waterBillAmount / totalParties
            }
        }

        return WaterBreakdown(
            splitRule = bill.waterSplitRule,
            totalAmount = bill.waterBillAmount,
            tenantPersons = tenantPersons,
            totalPersonsInHouse = totalPersons,
            finalWaterAmount = Math.round(finalWaterAmount * 100.0) / 100.0
        )
    }
}
