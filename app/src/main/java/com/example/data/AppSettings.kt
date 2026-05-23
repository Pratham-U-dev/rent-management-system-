package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val defaultWaterSplitRule: String = "EQUAL", // "EQUAL", "MANUAL", "PER_PERSON"
    val defaultElectricityTaxPercent: Double = 9.0,
    val defaultFixedCharges: Double = 150.0,
    val currencySymbol: String = "₹",
    val calculationPreferences: String = "PROPORTIONAL", // "PROPORTIONAL", "CUSTOM"
    val ownerName: String = "",
    val ownerPhone: String = "",
    val defaultWaterRatePerPerson: Double = 100.0 // rate per person if status is per person
)
