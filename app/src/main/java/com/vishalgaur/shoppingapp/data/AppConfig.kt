package com.vishalgaur.shoppingapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CountryConfig(
    val shippingCharge: Double = 0.0,
    val importCharge: Double = 0.0,
    val taxPercentage: Double = 0.0
) : Parcelable

@Parcelize
data class AppConfig(
    val defaultConfig: CountryConfig = CountryConfig(),
    val countryRules: Map<String, CountryConfig> = emptyMap()
) : Parcelable
