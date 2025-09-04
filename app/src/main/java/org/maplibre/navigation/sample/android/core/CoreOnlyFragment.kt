package org.maplibre.navigation.sample.android.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.common.toJvm
import org.maplibre.geojson.model.Feature
import org.maplibre.geojson.model.FeatureCollection
import org.maplibre.geojson.model.LineString
import org.maplibre.geojson.model.Point
import org.maplibre.geojson.turf.TurfMisc
import org.maplibre.navigation.core.location.engine.GoogleLocationEngine
import org.maplibre.navigation.core.location.replay.ReplayRouteLocationEngine
import org.maplibre.navigation.core.location.toAndroidLocation
import org.maplibre.navigation.core.models.BannerInstructions
import org.maplibre.navigation.core.models.DirectionsResponse
import org.maplibre.navigation.core.models.DirectionsRoute
import org.maplibre.navigation.core.models.RouteOptions
import org.maplibre.navigation.core.navigation.AndroidMapLibreNavigation
import org.maplibre.navigation.core.navigation.MapLibreNavigationOptions
import org.maplibre.navigation.core.utils.Constants
import org.maplibre.navigation.sample.android.R
import org.maplibre.navigation.sample.android.adapter.SuggestionAdapter
import org.maplibre.navigation.sample.android.databinding.FragmentCoreOnlyBinding
import org.maplibre.navigation.sample.android.model.Suggestion
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class CoreOnlyFragment : Fragment(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "CoreOnlyFragment"
        private const val ROUTE_SOURCE_ID = "route-source"
        private const val ROUTE_LAYER_ID = "route-layer"
        private const val ROUTE_INDEX = "route_index"
        private const val DESTINATION_LAYER_ID = "destination-layer"
        private const val DESTINATION_SOURCE_ID = "destination-source"
        private const val MARKER_ICON = "marker-icon"
        private const val MAP_STYLE_URL = "https://tiles.versatiles.org/assets/styles/colorful/style.json"

        private const val VALHALLA_URL = "https://valhalla1.openstreetmap.de/route"

        private const val GRAPHHOPPER_URL = "https://graphhopper.com/api/1/navigate?key=7088b84f-4cee-4059-96de-fd0cbda2fdff"

        private const val VALHALLA = "valhalla"

        private const val GRAPHHOPPER = "graphhopper"

        private const val PACKAGE = "package"


    }

    private lateinit var binding: FragmentCoreOnlyBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    private lateinit var originPoint: Point
    private lateinit var destinationPoint : Point
    private lateinit var adapter: SuggestionAdapter

    private var selectedRoute: DirectionsRoute? = null

    private var textToSpeech: TextToSpeech? = null

    private var lastSpokenInstruction: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCoreOnlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        ViewCompat.setOnApplyWindowInsetsListener(binding.flOverlayContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textToSpeech = TextToSpeech(requireContext(), this)

        binding.map.getMapAsync { map ->
            map.setStyle(
                Style.Builder()
                    .fromUri(MAP_STYLE_URL)
            ) { style ->
                initializeLocationAndMap(map, style)
            }
        }

        requestLocationPermission()


        adapter = SuggestionAdapter(emptyList()){selectedItem ->

            destinationPoint = Point(selectedItem.lon, selectedItem.lat)
            fetchDestination()
        }


        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.searchBar.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if(hasFocus) {
                binding.cancelButton.visibility  = View.VISIBLE
                binding.blankView.visibility = View.VISIBLE
            }else {
                binding.cancelButton.visibility  = View.GONE
                binding.blankView.visibility = View.GONE
            }

            binding.searchResultsContainer.visibility =
                if (hasFocus) View.VISIBLE else View.GONE
        }

        binding.cancelButton.setOnClickListener {
            binding.searchBar.setQuery("",false)
            binding.searchBar.clearFocus()
            binding.cancelButton.visibility = View.GONE
            binding.searchResultsContainer.visibility = View.GONE

        }


        binding.searchBar.setOnQueryTextListener(object :SearchView.OnQueryTextListener {
           override fun onQueryTextSubmit(query: String?): Boolean {
               fetchDestination()
               return true
           }

           override fun onQueryTextChange(newText: String?): Boolean {
               if (!newText.isNullOrEmpty()) {
                   fetchSuggestion(newText)
               } else {
                   adapter.updateData(emptyList())
               }
               return true
           }

       })
        binding.map.onCreate(savedInstanceState)
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)

            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, getString(R.string.language_not_supported))
            }else {
                Log.d(TAG, getString(R.string.initialization_successfully))
            }
        }else {
            Log.d(TAG, getString(R.string.initialization_failed))
        }
    }


    override fun onDestroy() {
        binding.map.onDestroy()
        textToSpeech?.shutdown()
        super.onDestroy()
    }


    private fun speak(instruction: String?) {
        textToSpeech?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            binding.map.getMapAsync { map ->
                map.getStyle { style ->
                    initializeLocationAndMap(map, style)
                }
            }
        } else {
            val snackBar = Snackbar.make(binding.main,R.string.location_permission_denied,Snackbar.LENGTH_SHORT)

            snackBar.setAction(R.string.settings){
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts(PACKAGE, requireContext().packageName, null)
                startActivity(intent)
            }
            snackBar.show()

        }
    }

    private fun requestLocationPermission() {
        val permissionGranted = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if(permissionGranted){
            getUserLocation { point ->

            }

        }else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }


    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getUserLocation(onReady: (Point) -> Unit) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onReady(Point(location.longitude, location.latitude))
            } else {
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,  2000
                ).build()

                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            super.onLocationResult(result)

                            result.lastLocation?.let { loc ->
                                onReady(Point(loc.longitude, loc.latitude))
                            }
                        }

                    },
                    Looper.getMainLooper()
                )
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initializeLocationAndMap(map: MapLibreMap, style: Style) {
        getUserLocation { location ->
            originPoint = location
            enableLocationComponent(map, style)
            map.locationComponent.forceLocationUpdate(
                Location("").apply {
                    latitude = location.latitude
                    longitude = location.longitude
                }
            )
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("SetTextI18n")
    private fun loadRoute(map: MapLibreMap, style: Style, destinationPoint: Point) {

        requireActivity().runOnUiThread {
            binding.searchBar.visibility = View.INVISIBLE
            binding.tvManuever.text = context?.getString(R.string.loading)
        }

        getUserLocation{userLocation ->
            lifecycleScope.launch {
                val directionsResponse = fetchRoute(userLocation, destinationPoint)
                Log.d(TAG, "${directionsResponse.routes.size}")

                val routes = directionsResponse.routes.mapIndexed { index, route ->
                    route.copy(
                        routeOptions = RouteOptions(
                        // These dummy route options are not not used to create directions,
                        // but currently they are necessary to start the navigation
                        // and to use the banner & voice instructions.
                        // Again, this isn't ideal, but it is a requirement of the framework.
                            baseUrl = "https://valhalla.routing",
                            profile = "valhalla",
                            user = "valhalla",
                            accessToken = "valhalla",
                            voiceInstructions = true,
                            bannerInstructions = true,
                            language = "en-US",
                            coordinates = listOf(
                                userLocation,
                                destinationPoint
                            ),
                            requestUuid = "0000-0000-0000-0000"
                        )
                    )
                }

                drawRoute(map,style, routes)

                addDestinationMarker(style, destinationPoint)

                enableLocationComponent(map, style)

                handleRouteClick(routes, map, style)

                val locationEngine = ReplayRouteLocationEngine()
//                val locationEngine = GoogleLocationEngine(
//                    requireContext(),
//                    looper = Looper.getMainLooper(),
//                )
                val options = MapLibreNavigationOptions(
                    defaultMilestonesEnabled = true
                )

                val mlNavigation = AndroidMapLibreNavigation(
                    context = requireContext(),
                    locationEngine = locationEngine,
                    options = options
                )
                mlNavigation.addProgressChangeListener { location, routeProgress ->
                    map.locationComponent.forceLocationUpdate(location.toAndroidLocation())

                    val style = map.style

                    val selectedRouteIndex = routes.indexOf(selectedRoute)
                        val source = style?.getSourceAs<GeoJsonSource>("$ROUTE_SOURCE_ID-$selectedRouteIndex")

                    val fullLine = LineString(routeProgress.directionsRoute.geometry, Constants.PRECISION_6)

                    val currentPoint = Point(location.longitude, location.latitude)

                    val endPoint = fullLine.coordinates.last()
                    val remaining = TurfMisc.lineSlice(currentPoint, endPoint, fullLine)

                    source?.setGeoJson(remaining.toJvm())

                    val distanceRemaining = routeProgress.distanceRemaining

                    if(distanceRemaining < 10) {
                        mlNavigation.stopNavigation()
                        style?.removeLayer(ROUTE_LAYER_ID)
                        style?.removeSource(ROUTE_SOURCE_ID)

                    }



                    val voiceInstruction =  routeProgress.currentLegProgress.currentStep.voiceInstructions

                    val remainingStepDistanceMeters =
                        routeProgress.currentLegProgress.currentStepProgress.distanceRemaining
                    routeProgress.currentLegProgress.currentStep.bannerInstructions?.first()
                        ?.let { bannerInstruction: BannerInstructions ->

                            binding.tvManuever.text =
                                " ${bannerInstruction.primary.text.removeSuffix(".")} after ${remainingStepDistanceMeters.roundToInt()}m"

                            val iconImage = IconMapper.getIconImage(bannerInstruction.primary.type, bannerInstruction.primary.modifier)

                            binding.imvManuever.setImageResource(iconImage)
                        }

                    voiceInstruction?.lastOrNull() { remainingStepDistanceMeters <= it.distanceAlongGeometry }?.let { instruction ->
                        if (lastSpokenInstruction != instruction.announcement) {
                            speak(instruction.announcement)
                            lastSpokenInstruction = instruction.announcement
                        }
                    }

                }

                selectedRoute = routes.firstOrNull()

                binding.startNavButton.setOnClickListener {
                    selectedRoute?.let {route ->
                        locationEngine.assign(route)
                        mlNavigation.startNavigation(route)
                        binding.instructionBar.visibility = View.VISIBLE
                        binding.startNavButton.visibility = View.GONE
                        binding.bottomSheet.bottomLayout.visibility = View.VISIBLE
                    }

                    routes.forEachIndexed { index, route ->
                        if (route != selectedRoute) {
                            style.removeLayer("$ROUTE_LAYER_ID-$index")
                            style.removeSource("$ROUTE_SOURCE_ID-$index")
                        }
                    }

                    followLocation(map)

                }

                binding.bottomSheet.cancelNavButton.setOnClickListener {
                    mlNavigation.stopNavigation()
                    binding.bottomSheet.bottomLayout.visibility = View.GONE
                    binding.instructionBar.visibility = View.GONE
                    binding.searchBar.visibility = View.VISIBLE
                    binding.searchBar.setQuery("",false)
                    routes.forEachIndexed { index, _ ->
                        style.removeLayer("$ROUTE_LAYER_ID-$index")
                        style.removeSource("$ROUTE_SOURCE_ID-$index")
                    }

                    selectedRoute = null
                }

            }

        }

    }



    private fun handleRouteClick(routes: List<DirectionsRoute>, map: MapLibreMap, style: Style) {

        map.addOnMapClickListener { point ->

            val screenPoint = map.projection.toScreenLocation(point)
            val features = map.queryRenderedFeatures(
                screenPoint,
                *routes.indices.map { "$ROUTE_LAYER_ID-$it" }.toTypedArray()
            )

            if (features.isNotEmpty()) {
                val clickedFeature= features[0]

                val routeIndex = clickedFeature.getNumberProperty(ROUTE_INDEX)?.toInt()

                Log.d(TAG, "$routeIndex")

                if (routeIndex != null) {
                    selectedRoute = routes[routeIndex]

                    routes.forEachIndexed { i, _ ->
                        val color = if (i == routeIndex) Color.BLUE else Color.LTGRAY
                        style.getLayer("$ROUTE_LAYER_ID-$i")?.setProperties(
                            lineColor(color)
                        )
                    }

                    val selectedLayerId = "$ROUTE_LAYER_ID-$routeIndex"
                    val selectedLayer = style.getLayer(selectedLayerId)
                    if (selectedLayer != null) {
                        style.removeLayer(selectedLayer)
                        style.addLayer(selectedLayer)
                    }
                }

            }
            true
        }

    }


    private fun addDestinationMarker(style: Style, destinationPoint: Point) {

        style.removeLayer(DESTINATION_LAYER_ID)
        style.removeSource(DESTINATION_SOURCE_ID)

        style.getSource(DESTINATION_SOURCE_ID)
        style.getLayer(DESTINATION_LAYER_ID)

        val destinationSource = GeoJsonSource(DESTINATION_SOURCE_ID, destinationPoint.toJvm())
        style.addSource(destinationSource)

        style.addImage(MARKER_ICON, ContextCompat.getDrawable(requireContext(), R.drawable.marker)!!)

        val layer = SymbolLayer(DESTINATION_LAYER_ID, DESTINATION_SOURCE_ID)
            .withProperties(
                iconImage(MARKER_ICON),
                PropertyFactory.iconSize(
                    Expression.interpolate(
                        Expression.linear(), Expression.zoom(),
                        Expression.stop(0,0.15f),
                        Expression.stop(22,0.15f)
                    )
                ),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)

            )

        style.addLayer(layer)

    }


    private suspend fun fetchRoute(origin: Point, destinationPoint: Point): DirectionsResponse = suspendCoroutine { continuation ->
        val provider = VALHALLA

        val requestBody = if(provider == GRAPHHOPPER) {
            mapOf(
                "type" to "mapbox",
                "profile" to "car",
                "locale" to "en-US",
                "points" to listOf(
                    listOf(origin.longitude, origin.latitude),

                    listOf(destinationPoint.longitude, destinationPoint.latitude)
                )
            )
        } else {
            mapOf(
                "format" to "osrm",
                "costing" to "auto",
                "banner_instructions" to true,
                "voice_instructions" to true,
                "language" to "en-US",
                "alternates" to 3,
                "directions_options" to mapOf(
                    "units" to "kilometers"
                ),
                "costing_options" to mapOf(
                    "auto" to mapOf(
                        "top_speed" to 130
                    )
                ),
                "locations" to listOf(

                    mapOf(
                        "lon" to origin.longitude,
                        "lat" to origin.latitude,
                        "type" to "break"
                    ),
                    mapOf(
                        "lon" to destinationPoint.longitude,
                        "lat" to destinationPoint.latitude,
                        "type" to "break"
                    )
                )
            )
        }

        val requestBodyJson = Gson().toJson(requestBody)
        val client = OkHttpClient()

        val url = if (provider == VALHALLA) VALHALLA_URL
        else GRAPHHOPPER_URL

        val request = Request.Builder()
            .header("User-Agent", "ML Nav - Android Sample App")
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
                Log.d(TAG, "${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val raw = response.body!!.string()
                Log.d(TAG, raw)
                val directionsResponse = DirectionsResponse.fromJson(raw)
                Log.d(TAG, "${directionsResponse.routes.size}")
                continuation.resume(directionsResponse)
            }
        })
    }


    private fun fetchSuggestion(query: String?) {
        val client = OkHttpClient()
        val url = "https://photon.komoot.io/api/?q=$query&lon=${originPoint.longitude}&lat=${originPoint.latitude}&limit=10&lang=en&location_bias_scale=1.0&bbox=68.0,6.5,97.5,36.9"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val response = response.body?.string() ?: return

                Log.d(TAG, response)

                val root = JSONObject(response)

                val features  = root.getJSONArray("features")

                val suggestions = mutableListOf<Suggestion>()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val properties = feature.getJSONObject("properties")
                    val geometry = feature.getJSONObject("geometry")
                    val coords = geometry.getJSONArray("coordinates")

                    val name = properties.optString("name")
                    val city = properties.optString("city")
                    val state = properties.optString("state")
                    val country = properties.optString("country")
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)


                    suggestions.add(Suggestion(name, city, state, country, lon, lat))
                }

                requireActivity().runOnUiThread {
                    adapter.updateData(suggestions)
                }

                if(features.length() > 0) {
                    val first = features.getJSONObject(0)

                    val geometry = first.getJSONObject("geometry")

                    val coords = geometry.getJSONArray("coordinates")

                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)

                    Log.d(TAG,"$lon $lat")


                    destinationPoint = Point(lon, lat)
                }
            }

        })
    }

    private fun fetchDestination() {

        binding.map.getMapAsync { map ->
            map.getStyle { style ->
                loadRoute(map, style, destinationPoint)
            }
        }

    }


    private fun drawRoute(map: MapLibreMap ,style: Style, routes: List<DirectionsRoute>) {
        style.removeLayer(ROUTE_LAYER_ID)
        style.removeSource(ROUTE_SOURCE_ID)

        routes.forEachIndexed { index, route ->
            val routeLine = LineString(route.geometry, Constants.PRECISION_6)

            val feature = Feature(routeLine)
            feature.addProperty(ROUTE_INDEX, index)
            val sourceId = "$ROUTE_SOURCE_ID-$index"
            val layerId = "$ROUTE_LAYER_ID-$index"

            val routeSource = GeoJsonSource(sourceId, FeatureCollection(listOf(feature)).toJvm())

            style.addSource(routeSource)

            val routeLayer = LineLayer(layerId, sourceId).withProperties(
                lineWidth( if (index == 0) 6f else 5f ),
                lineColor( if (index == 0) Color.BLUE else Color.LTGRAY )
            )

            if (index == 0) {
                style.addLayer(routeLayer)
            } else {
                style.addLayerBelow(routeLayer, "$ROUTE_LAYER_ID-${index-1}")
            }
            binding.startNavButton.visibility = View.VISIBLE

        }
    }


    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(map: MapLibreMap, style: Style) {
        map.locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), style)
                .useDefaultLocationEngine(false)
                .useSpecializedLocationLayer(true)
                .build()
        )

        followLocation(map)

        map.locationComponent.isLocationComponentEnabled = true
    }

    private fun followLocation(map: MapLibreMap) {
        if (!map.locationComponent.isLocationComponentActivated) {
            return
        }

        map.locationComponent.renderMode = RenderMode.GPS
        map.locationComponent.setCameraMode(
            CameraMode.TRACKING_GPS,
            object :
                OnLocationCameraTransitionListener {
                override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                    map.locationComponent.zoomWhileTracking(17.0)
                    map.locationComponent.tiltWhileTracking(60.0)
                }

                override fun onLocationCameraTransitionCanceled(cameraMode: Int) {}
            }
        )
    }

}