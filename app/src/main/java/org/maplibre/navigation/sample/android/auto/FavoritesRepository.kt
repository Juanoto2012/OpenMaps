package org.maplibre.navigation.sample.android.auto

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.maplibre.geojson.Point

data class FavoritePlace(val name: String, val location: Point)

object FavoritesRepository {
    private const val PREFS_NAME = "FavoritePlaces"
    private const val FAVORITES_KEY = "favorites"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var favoritePlaces: MutableList<FavoritePlace> = mutableListOf()

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFavorites()
    }

    fun getFavorites(): List<FavoritePlace> {
        return favoritePlaces.toList()
    }

    fun addFavorite(place: FavoritePlace) {
        if (!favoritePlaces.contains(place)) {
            favoritePlaces.add(place)
            saveFavorites()
        }
    }

    fun removeFavorite(place: FavoritePlace) {
        if (favoritePlaces.remove(place)) {
            saveFavorites()
        }
    }

    private fun saveFavorites() {
        val json = gson.toJson(favoritePlaces)
        sharedPreferences.edit().putString(FAVORITES_KEY, json).apply()
    }

    private fun loadFavorites() {
        val json = sharedPreferences.getString(FAVORITES_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<FavoritePlace>>() {}.type
            favoritePlaces = gson.fromJson(json, type)
        }
    }
}
