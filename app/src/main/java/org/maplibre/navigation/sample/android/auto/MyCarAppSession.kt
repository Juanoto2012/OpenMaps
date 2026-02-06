package org.maplibre.navigation.sample.android.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class MyCarAppSession : Session() {

    private var mapScreen: MapScreen? = null

    init {
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                FavoritesRepository.initialize(carContext)
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        val screen = MapScreen(carContext)
        mapScreen = screen
        return screen
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intents if needed
    }
}