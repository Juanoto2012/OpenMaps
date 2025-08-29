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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.common.toJvm
import org.maplibre.geojson.model.LineString
import org.maplibre.geojson.model.Point
import org.maplibre.geojson.turf.TurfMisc
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class CoreOnlyFragment : Fragment() {

    companion object {
        private const val TAG = "CoreOnlyFragment"
        private const val ROUTE_SOURCE_ID = "route-source"
        private const val ROUTE_LAYER_ID = "route-layer"
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
            Toast.makeText(requireContext(), R.string.navigation_started, Toast.LENGTH_SHORT).show()

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

    override fun onDestroy() {
        binding.map.onDestroy()
        super.onDestroy()
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
            binding.instructionBar.visibility = View.VISIBLE
            binding.searchBar.visibility = View.INVISIBLE
            binding.tvManuever.text = context?.getString(R.string.loading)
        }

        getUserLocation{userLocation ->
            lifecycleScope.launch {
                val directionsResponse = fetchRoute(userLocation, destinationPoint)
                val route = directionsResponse.routes.first().copy(
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

                drawRoute(style, route)

                enableLocationComponent(map, style)

                val locationEngine = ReplayRouteLocationEngine()
//                val locationEngine = AndroidLocationEngineImpl(requireContext())
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
                    val source = style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

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

                    routeProgress.currentLegProgress.currentStep.bannerInstructions?.first()
                        ?.let { bannerInstruction: BannerInstructions ->
                            val remainingStepDistanceMeters =
                                routeProgress.currentLegProgress.currentStepProgress.distanceRemaining
                            binding.tvManuever.text =
                                "${remainingStepDistanceMeters.roundToInt()}m : ${bannerInstruction.primary.type}+${bannerInstruction.primary.modifier} ${bannerInstruction.primary.text}"

                               val iconImage = IconMapper.getIconImage(bannerInstruction.primary.type, bannerInstruction.primary.modifier)
//
                            binding.imvManuever.setImageResource(iconImage)
                        }
                }

                locationEngine.assign(route)
                mlNavigation.startNavigation(route)
            }

        }

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
            }

            override fun onResponse(call: Call, response: Response) {
                val raw = response.body!!.string()
                val directionsResponse = DirectionsResponse.fromJson(raw)
                continuation.resume(directionsResponse)
            }
        })
    }


    private fun fetchSuggestion(query: String?) {
        val client = OkHttpClient()
        val url = "https://photon.komoot.io/api/?q=$query&lon=${originPoint.longitude}&lat=${originPoint.latitude}&limit=10&lang=en"

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
                    val country = properties.optString("country")
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)


                    suggestions.add(Suggestion(name, country, lon, lat))
                }

                requireActivity().runOnUiThread {
                    adapter.updateData(suggestions)
                }


                val first = features.getJSONObject(0)

                val geometry = first.getJSONObject("geometry")

                val coords = geometry.getJSONArray("coordinates")

                val lon = coords.getDouble(0)
                val lat = coords.getDouble(1)

                Log.d(TAG,"$lon $lat")


                destinationPoint = Point(lon, lat)
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


    private fun drawRoute(style: Style, route: DirectionsRoute) {
        val routeLine = LineString(route.geometry, Constants.PRECISION_6)

        // The toJvm() extension converts the LineString to the deprecated Jvm one.
        val routeSource = GeoJsonSource(ROUTE_SOURCE_ID, routeLine.toJvm())
        style.addSource(routeSource)

        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
            .withProperties(
                lineWidth(5f),
                lineColor(Color.BLUE)
            )

        style.addLayer(routeLayer)
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