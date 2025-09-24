package org.maplibre.navigation.sample.android.model

data class RouteOptions(
    var avoidToll: Boolean = false,
    var avoidHighways: Boolean = false,
    var avoidFerries: Boolean = false,
    var useDistance: Int = 0

)
