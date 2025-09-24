package org.maplibre.navigation.sample.android.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.maplibre.navigation.sample.android.model.RouteOptions

class NavigationViewModel: ViewModel() {
    private val _routeOptions = MutableLiveData(RouteOptions())
    val routeOptions: LiveData<RouteOptions> = _routeOptions

    fun updateAvoidToll(enabled: Boolean) {
        val current = _routeOptions.value ?: RouteOptions()
        _routeOptions.value = current.copy(avoidToll = enabled)
    }

    fun updateAvoidFerry(enabled: Boolean) {
        val current = _routeOptions.value ?: RouteOptions()
        _routeOptions.value = current.copy(avoidFerries = enabled)
    }

    fun updateAvoidHighway(enabled: Boolean) {
        val current = _routeOptions.value ?: RouteOptions()
        _routeOptions.value = current.copy(avoidHighways = enabled)
    }

    fun updateUseDistance(useDistance: Int) {
        val current = _routeOptions.value ?: RouteOptions()
        _routeOptions.value = current.copy(useDistance = useDistance)
    }
}