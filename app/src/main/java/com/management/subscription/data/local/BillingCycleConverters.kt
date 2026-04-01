package com.management.subscription.data.local

import androidx.room.TypeConverter
import com.management.subscription.data.BillingCycle

class BillingCycleConverters {
    @TypeConverter
    fun toBillingCycle(value: String): BillingCycle = BillingCycle.valueOf(value)

    @TypeConverter
    fun fromBillingCycle(value: BillingCycle): String = value.name
}
