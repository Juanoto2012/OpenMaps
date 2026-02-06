package org.maplibre.navigation.sample.android.auto

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.Place
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import org.maplibre.geojson.Point
import org.maplibre.navigation.sample.android.service.NavigationService

class MapScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        FavoritesRepository.getFavorites().forEach { favoritePlace ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(favoritePlace.name)
                    .setOnClickListener {
                        // Start navigation to the selected favorite place
                        startNavigation(favoritePlace)
                    }
                    .build()
            )
        }

        val addFavoriteAction = Action.Builder()
            .setTitle("Add Favorite")
            .setOnClickListener {
                // For demonstration, add a dummy location. In a real app, this would be the current location.
                val newFavorite = FavoritePlace("New Favorite " + System.currentTimeMillis() % 1000, Point.fromLngLat(0.0, 0.0))
                FavoritesRepository.addFavorite(newFavorite)
                invalidate()
            }
            .build()

        val actionStrip = ActionStrip.Builder()
            .addAction(addFavoriteAction)
            .addAction(Action.BACK)
            .build()

        return PlaceListNavigationTemplate.Builder()
            .setTitle("Favorites")
            .setItemList(itemListBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }

    private fun startNavigation(favoritePlace: FavoritePlace) {
        // Create an intent to start navigation
        val navigationIntent = Intent(carContext, NavigationService::class.java).apply {
            action = Intent.ACTION_VIEW
            // Pass the destination here
            putExtra("DESTINATION_LATITUDE", favoritePlace.location.latitude())
            putExtra("DESTINATION_LONGITUDE", favoritePlace.location.longitude())
            putExtra("DESTINATION_NAME", favoritePlace.name)
        }
        carContext.startService(navigationIntent)
        
        // Show a simple confirmation message
        invalidate()
    }

    fun onCarConfigurationChanged() {
        // Handle configuration changes if needed
    }
}