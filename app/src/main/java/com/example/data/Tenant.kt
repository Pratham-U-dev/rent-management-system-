package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val roomName: String,
    val rentAmount: Double,
    val depositAmount: Double,
    val occupancyStartDate: String,
    val notes: String = "",
    val numberOfPersons: Int = 1,
    val isActive: Boolean = true
)
