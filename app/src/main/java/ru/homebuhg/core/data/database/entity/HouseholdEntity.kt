package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerUid: String,
    val baseCurrency: String = "RUB"
)

@Entity(tableName = "household_members", primaryKeys = ["householdId", "userUid"])
data class HouseholdMemberEntity(
    val householdId: String,
    val userUid: String,
    val role: MemberRole,
    val joinedAt: Long
)

enum class MemberRole { OWNER, MEMBER }
