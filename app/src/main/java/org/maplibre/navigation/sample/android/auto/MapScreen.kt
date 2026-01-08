package org.maplibre.navigation.sample.android.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class MapScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Hello Android Auto!")
            .setTitle("Welcome to OpenMaps")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}