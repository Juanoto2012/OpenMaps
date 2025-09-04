package org.maplibre.navigation.sample.android.model

data class Suggestion(
    val name: String,
    val city: String,
    val state: String,
    val country: String,
    val lon: Double,
    val lat: Double
)