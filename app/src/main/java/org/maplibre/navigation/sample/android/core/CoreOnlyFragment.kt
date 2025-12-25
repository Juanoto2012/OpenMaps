// Archivo restaurado a su estado anterior a las dos últimas ediciones.

// Este archivo ha sido restaurado a su versión anterior antes de la última edición.

package org.maplibre.navigation.sample.android.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
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
import org.maplibre.geojson.turf.TurfMeasurement
import org.maplibre.geojson.turf.TurfMisc
import org.maplibre.geojson.turf.TurfUnit
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
import org.maplibre.navigation.sample.android.viewModel.NavigationViewModel
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
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
        private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

        private const val VALHALLA_URL = "https://valhalla1.openstreetmap.de/route"

        private const val OSRM_URL = "https://router.project-osrm.org/route/v1/driving/"


        private const val VALHALLA = "valhalla"

        private const val PACKAGE = "package"

        private const val TIME_PATTERN = "h:mm a"
    }

    private lateinit var binding: FragmentCoreOnlyBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var originPoint: Point
    private lateinit var destinationPoint : Point
    private lateinit var adapter: SuggestionAdapter

    private var currentClickListener: MapLibreMap.OnMapClickListener? = null

    private var selectedRoute: DirectionsRoute? = null

    private var selectedRouteIndex: Int? = null

    private var textToSpeech: TextToSpeech? = null

    private var lastSpokenInstruction: String? = null

    private val viewModel: NavigationViewModel by viewModels()

    private var mlNavigation: AndroidMapLibreNavigation? = null
    private var currentMap: MapLibreMap? = null
    private var currentStyle: Style? = null

    private var isNavigating: Boolean = false

    private var job: Job? = null

    private var isCameraTrackingUser: Boolean = true


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
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels


        textToSpeech = TextToSpeech(requireContext(), this)

        val bottomSheet = BottomSheetBehavior.from(binding.routeOptionsSheet)

        bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED

        val navBottomSheet = BottomSheetBehavior.from(binding.navRouteSheet)

        navBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        navBottomSheet.isFitToContents = false
        navBottomSheet.expandedOffset = screenHeight / 2

        binding.map.getMapAsync { map ->
            currentMap = map
            map.setStyle(
                Style.Builder()
                    .fromUri(MAP_STYLE_URL)
            ) { style ->
                currentStyle = style
                // Comprobación de permisos antes de inicializar ubicación y mapa
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    initializeLocationAndMap(map, style)
                }
                // Listener para saber si el usuario mueve el mapa manualmente
                map.addOnCameraMoveListener {
                    if (isCameraTrackingUser) {
                        isCameraTrackingUser = false
                        binding.recenterButton.visibility = View.VISIBLE
                    }
                }
                ViewCompat.setOnApplyWindowInsetsListener(binding.map) {_, insets ->
                    map.uiSettings.setCompassMargins(
                        0,
                        binding.instructionBar.height + 250,
                        20,
                        0
                    )
                    insets
                }
            }
        }

        requestLocationPermission()

        adapter = SuggestionAdapter(emptyList()){selectedItem ->

            destinationPoint = Point(selectedItem.lon, selectedItem.lat)
            fetchDestination()
        }


        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val searchEditText = binding.searchBar.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.setHintTextColor(Color.BLACK)

        binding.searchBar.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if(hasFocus) {
                binding.routeOptionsSheet.visibility = View.GONE
                binding.cancelButton.visibility  = View.VISIBLE
                binding.blankView.visibility = View.VISIBLE
            }else {
                binding.routeOptionsSheet.visibility = View.VISIBLE
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

        binding.recenterButton.setOnClickListener {
            isCameraTrackingUser = true
            binding.map.getMapAsync { map ->
                if (map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.setCameraMode(
                        CameraMode.TRACKING_GPS,
                        object : OnLocationCameraTransitionListener {
                            override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                                map.locationComponent.zoomWhileTracking(17.0)
                                map.locationComponent.tiltWhileTracking(60.0)
                                binding.recenterButton.visibility = View.GONE
                            }
                            override fun onLocationCameraTransitionCanceled(cameraMode: Int) {}
                        }
                    )
                }
            }
        }


        binding.searchBar.setOnQueryTextListener(object :SearchView.OnQueryTextListener {
           override fun onQueryTextSubmit(query: String?): Boolean {
               fetchDestination()
               return true
           }

           override fun onQueryTextChange(newText: String?): Boolean {
               job?.cancel()
               if (!newText.isNullOrEmpty()) {
                    job = CoroutineScope(Dispatchers.Main).launch {
                       delay(200)
                       fetchSuggestion(newText)
                   }

               } else {
                   adapter.updateData(emptyList())
               }
               return true
           }

       })
        binding.map.onCreate(savedInstanceState)

        viewModel.routeOptions.observe(viewLifecycleOwner) { options ->

            binding.routeOptions.switchTolls.isChecked = options.avoidToll
            binding.routeOptions.switchFerries.isChecked = options.avoidFerries
            binding.routeOptions.switchMotorways.isChecked = options.avoidHighways
            binding.routeOptions.routeTypeGroup.check(
                if (options.useDistance == 0) R.id.fastest else R.id.shortest
            )

            binding.bottomSheet.etaRouteOptions.switchTolls.isChecked = options.avoidToll
            binding.bottomSheet.etaRouteOptions.switchFerries.isChecked = options.avoidFerries
            binding.bottomSheet.etaRouteOptions.switchMotorways.isChecked = options.avoidHighways
            binding.bottomSheet.etaRouteOptions.routeTypeGroup.check(
                if (options.useDistance == 0) R.id.fastest else R.id.shortest
            )
        }

        binding.routeOptions.switchTolls.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidToll(checked)
        }
        binding.routeOptions.switchFerries.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidFerry(checked)
        }
        binding.routeOptions.switchMotorways.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidHighway(checked)
        }
        binding.routeOptions.routeTypeGroup.setOnCheckedChangeListener { group, checkedId ->
                val useDistance = if (checkedId == R.id.fastest) 0 else 1
                viewModel.updateUseDistance(useDistance)
        }

        binding.bottomSheet.etaRouteOptions.switchTolls.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidToll(checked)
            updateRouteOptionsDuringNavigation()
        }
        binding.bottomSheet.etaRouteOptions.switchFerries.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidFerry(checked)
            updateRouteOptionsDuringNavigation()
        }
        binding.bottomSheet.etaRouteOptions.switchMotorways.setOnCheckedChangeListener { btn, checked ->
            if (btn.isPressed) viewModel.updateAvoidHighway(checked)
            updateRouteOptionsDuringNavigation()
        }
        binding.bottomSheet.etaRouteOptions.routeTypeGroup.setOnCheckedChangeListener { group, checkedId ->
                val useDistance = if (checkedId == R.id.fastest) 0 else 1
                viewModel.updateUseDistance(useDistance)
                updateRouteOptionsDuringNavigation()
        }

        // Botón para descargar mapas offline
        binding.routeOptions.downloadMapsButton.setOnClickListener {
            Toast.makeText(requireContext(), "La caché automática está activa. Los mapas que visites se guardarán para uso offline.", Toast.LENGTH_LONG).show()
        }

        // Botón para borrar caché de mapas
        binding.routeOptions.clearCacheButton.setOnClickListener {
            borrarCacheMapas()
        }
    }

    // --- Cambios para indicaciones de voz en español ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Idioma no soportado")
            } else {
                Log.d(TAG, "Inicialización exitosa")
            }
        } else {
            Log.d(TAG, "Fallo en la inicialización")
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
        // Comprobación de permisos antes de obtener la ubicación
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        getUserLocation { location ->
            originPoint = location
            enableLocationComponent(location, map, style)
            map.locationComponent.forceLocationUpdate(
                Location("").apply {
                    latitude = location.latitude
                    longitude = location.longitude
                    altitude = 0.0
                }
            )
            // Seguir la ubicación del usuario siempre
            map.locationComponent.addOnLocationClickListener {
                if (!isCameraTrackingUser) {
                    isCameraTrackingUser = true
                    map.locationComponent.setCameraMode(CameraMode.TRACKING_GPS, null)
                    binding.recenterButton.visibility = View.GONE
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("SetTextI18n")
    private fun loadRoute(map: MapLibreMap, style: Style, destinationPoint: Point) {

        mlNavigation?.stopNavigation()

        selectedRoute = null

        requireActivity().runOnUiThread {
            binding.searchBar.visibility = View.INVISIBLE
            binding.tvManuever.text = context?.getString(R.string.loading)
        }

        getUserLocation{userLocation ->
            lifecycleScope.launch {
                val directionsResponse = withContext(Dispatchers.IO) {
                    fetchRoute(userLocation, destinationPoint)
                }
                Log.d(TAG, "${directionsResponse.routes.size}")

                if (directionsResponse.routes.isEmpty()) {
                    requireActivity().runOnUiThread {
                        binding.searchBar.visibility = View.VISIBLE
                        binding.routeOptionsSheet.visibility = View.VISIBLE
                        binding.navRouteSheet.visibility = View.GONE
                        binding.bottomSheet.bottomLayout.visibility = View.GONE
                        binding.startNavButton.visibility = View.GONE

                        binding.searchBar.requestLayout()
                        Toast.makeText(requireContext(), R.string.no_route_found, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

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
                            language = "es-ES",
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

                showRoutesOverview(map, routes)

                handleRouteClick(routes, map, style)


//                val locationEngine = ReplayRouteLocationEngine()
//                val locationEngine = GoogleLocationEngine(
//                    requireContext(),
//                    looper = Looper.getMainLooper(),
//                )
                mlNavigation = AndroidMapLibreNavigation(
                    context = requireContext(),
                    options = MapLibreNavigationOptions(
                        defaultMilestonesEnabled = true
                    )
                )

                // --- Mantener el icono de posición del usuario en la ubicación real durante la navegación ---
                // En el listener de progreso de navegación, actualizar la posición del usuario en el mapa y recalcular si se desvía
                mlNavigation?.addProgressChangeListener { location, routeProgress ->

                    map.locationComponent.forceLocationUpdate(location.toAndroidLocation())
                    if (isCameraTrackingUser) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
                    }
                    val style = map.style

                    val distanceRemaining = routeProgress.distanceRemaining
                    val durationRemaining = routeProgress.durationRemaining

                    updateEta(durationRemaining, distanceRemaining)


                    selectedRouteIndex = routes.indexOf(selectedRoute)

                    val source = style?.getSourceAs<GeoJsonSource>("$ROUTE_SOURCE_ID-$selectedRouteIndex")

                    val fullLine = LineString(routeProgress.directionsRoute.geometry, Constants.PRECISION_6)

                    val currentPoint = Point(location.longitude, location.latitude)

                    val routePoints = fullLine.coordinates

                    val nearestPoint = TurfMisc.nearestPointOnLine(currentPoint, routePoints,
                        TurfUnit.KILOMETERS)

                    val nearestPointGeometry = nearestPoint.geometry as Point

                    val distanceToRoute = TurfMeasurement.distance(currentPoint, nearestPointGeometry, TurfUnit.METERS)
                    if(distanceToRoute > 40) {
                        mlNavigation?.stopNavigation()

                        Log.d(TAG, "Te has desviado de la ruta")
                        Toast.makeText(requireContext(), "Te has desviado de la ruta",
                            Toast.LENGTH_SHORT).show()

                        routes.forEachIndexed { index, route ->
                            style?.removeLayer("$ROUTE_LAYER_ID-$index")
                            style?.removeSource("$ROUTE_SOURCE_ID-$index")
                        }


                        lifecycleScope.launch {
                            val newDirectionRes = withContext(Dispatchers.IO) {
                                fetchRoute(currentPoint, destinationPoint)
                            }
                            val newRoute = newDirectionRes.routes.first().copy(
                                routeOptions = RouteOptions(
                                    baseUrl = "https://valhalla.routing",
                                    profile = "valhalla",
                                    user = "valhalla",
                                    accessToken = "valhalla",
                                    voiceInstructions = true,
                                    bannerInstructions = true,
                                    language = "es-ES",
                                    coordinates = listOf(
                                        currentPoint,
                                        destinationPoint
                                    ),
                                    requestUuid = "0000-0000-0000-0000"
                                )
                            )

                            drawRoute(map, style!!, listOf(newRoute))

                            mlNavigation?.startNavigation(newRoute)
                            // Recentrar cámara si estaba siguiendo
                            if (isCameraTrackingUser) {
                                map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(currentPoint.latitude, currentPoint.longitude)))
                            }
                        }

                    }

                    val endPoint = fullLine.coordinates.last()
                    val remaining = TurfMisc.lineSlice(currentPoint, endPoint, fullLine)

                    source?.setGeoJson(remaining.toJvm())

                    if(distanceRemaining < 30) {
                        mlNavigation?.stopNavigation()
                        isNavigating = false
                        resetCameraState(userLocation, map)

                        routes.forEachIndexed { index, route ->
                            style?.removeLayer("$ROUTE_LAYER_ID-$index")
                            style?.removeSource("$ROUTE_SOURCE_ID-$index")
                        }
                        binding.instructionBar.visibility = View.GONE
                        binding.bottomSheet.bottomLayout.visibility = View.GONE
                        binding.navRouteSheet.visibility = View.GONE
                        binding.searchBar.visibility = View.VISIBLE
                        binding.routeOptionsSheet.visibility = View.VISIBLE

                    }

                    val voiceInstruction =  routeProgress.currentLegProgress.currentStep.voiceInstructions

                    val remainingStepDistanceMeters =
                        routeProgress.currentLegProgress.currentStepProgress.distanceRemaining
                    routeProgress.currentLegProgress.currentStep.bannerInstructions?.first()
                        ?.let { bannerInstruction: BannerInstructions ->

                            binding.tvManuever.text = getString(
                                R.string.after,
                                bannerInstruction.primary.text.removeSuffix("."),
                                remainingStepDistanceMeters.roundToInt()
                            )

                            val iconImage = IconMapper.getIconImage(bannerInstruction.primary.type, bannerInstruction.primary.modifier)

                            binding.imvManuever.setImageResource(iconImage)
                        }

                    voiceInstruction?.lastOrNull { remainingStepDistanceMeters <= it.distanceAlongGeometry }?.let { instruction ->
                        if (lastSpokenInstruction != instruction.announcement) {
                            speak(instruction.announcement)
                            lastSpokenInstruction = instruction.announcement
                        }
                    }

                }

                if(selectedRoute == null && routes.isNotEmpty()) {
                    selectedRoute = routes.first()
                }

                selectedRoute?.let {
                    showEta(selectedRoute!!)
                }

                binding.startNavButton.setOnClickListener {
                    selectedRoute?.let {route ->
                        mlNavigation?.startNavigation(route)
                        isNavigating = true
                        binding.instructionBar.visibility = View.VISIBLE
                        binding.recenterButton.visibility = View.VISIBLE
                        binding.routeOptionsSheet.visibility = View.GONE
                        binding.startNavButton.visibility = View.GONE
                        // Notificación inicial
                        val firstInstruction = route.legs.firstOrNull()?.steps?.firstOrNull()?.bannerInstructions?.firstOrNull()?.primary?.text ?: "Navegando..."
                        val firstIcon = route.legs.firstOrNull()?.steps?.firstOrNull()?.bannerInstructions?.firstOrNull()?.let { IconMapper.getIconImage(it.primary.type, it.primary.modifier) } ?: R.drawable.ic_navigation
                        startNavigationNotification(firstInstruction, firstIcon)
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
                    mlNavigation?.stopNavigation()
                    selectedRoute = null
                    isNavigating = false
                    isCameraTrackingUser = false
                    binding.bottomSheet.bottomLayout.visibility = View.GONE
                    binding.navRouteSheet.visibility = View.GONE
                    binding.instructionBar.visibility = View.GONE
                    binding.startNavButton.visibility = View.GONE
                    binding.recenterButton.visibility = View.VISIBLE
                    binding.searchBar.visibility = View.VISIBLE
                    binding.routeOptionsSheet.visibility = View.VISIBLE
                    binding.searchBar.setQuery("",false)

                    routes.forEachIndexed { index, _ ->
                        style.removeLayer("$ROUTE_LAYER_ID-$index")
                        style.removeSource("$ROUTE_SOURCE_ID-$index")
                    }

                    style.removeLayer(DESTINATION_LAYER_ID)
                    style.removeSource(DESTINATION_SOURCE_ID)

                    resetCameraState(userLocation ,map)

                    stopNavigationNotification()
                }
            }

        }

    }

    private fun formatTime(seconds: Double): String {
        val totalSeconds = seconds.toLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}hr ${minutes}min"
            hours > 0 -> "$hours hr"
            minutes > 0 -> "$minutes min"
            else -> "$minutes min"
        }

    }

    fun formatDistance(meters: Double): String {
        val km = meters / 1000.0
        return if (km >= 1) {
            String.format(Locale.getDefault(), context?.getString(R.string.distance_km) ?: "", km)
        } else {
            String.format(Locale.getDefault(), context?.getString(R.string.distance_m) ?: "", meters)
        }
    }

    fun showEta(route: DirectionsRoute) {

        updateEta(route.duration, route.distance)

    }

    fun updateEta(durationRemaining: Double, distanceRemaining: Double) {

        val durationMinutes =  formatTime(durationRemaining)
        val distanceKm = formatDistance(distanceRemaining)

        val currentTime = System.currentTimeMillis()
        val arrivingTimeMilli = currentTime + (durationRemaining * 1000).toLong()

        val dateFormat = SimpleDateFormat(TIME_PATTERN, Locale.getDefault())
        val arrivingTime = dateFormat.format(Date(arrivingTimeMilli))

        binding.bottomSheet.time.text = durationMinutes
        binding.bottomSheet.distance.text = distanceKm
        binding.bottomSheet.arrivingTime.text = arrivingTime
    }


    private fun showRoutesOverview(map: MapLibreMap, routes: List<DirectionsRoute>) {
        val builder = LatLngBounds.Builder()
        routes.forEach { route ->
            val routeLine = LineString(route.geometry, Constants.PRECISION_6)
            routeLine.coordinates.forEach { point ->
                builder.include(LatLng(point.latitude, point.longitude))
            }
        }

        try {
            val bounds = builder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
        }
    }




    private fun handleRouteClick(routes: List<DirectionsRoute>, map: MapLibreMap, style: Style) {

        currentClickListener?.let {
            map.removeOnMapClickListener(it)
        }


        val newListener = MapLibreMap.OnMapClickListener { point ->
            val screenPoint = map.projection.toScreenLocation(point)
            val features = map.queryRenderedFeatures(
                screenPoint,
                *routes.indices.map { "$ROUTE_LAYER_ID-$it" }.toTypedArray()
            )

            if (features.isNotEmpty()) {
                val routeIndex = features[0].getNumberProperty(ROUTE_INDEX)?.toInt()
                if(routeIndex != null) {
                    selectedRoute = routes[routeIndex]
                    routes.forEachIndexed { i, _ ->
                        val color = if (i == routeIndex) Color.BLUE else Color.LTGRAY
                        style.getLayer("$ROUTE_LAYER_ID-$i")?.setProperties(lineColor(color))
                    }

                  showEta(selectedRoute!!)

                }
                val selectedLayerId = "$ROUTE_LAYER_ID-$routeIndex"
                val selectedLayer = style.getLayer(selectedLayerId)
                if (selectedLayer != null) {
                    style.removeLayer(selectedLayer)
                    style.addLayer(selectedLayer)
                }
            }
            true
        }

        currentClickListener = newListener
        map.addOnMapClickListener(newListener)
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

        val options = viewModel.routeOptions.value ?: org.maplibre.navigation.sample.android.model.RouteOptions()

        val avoidToll = options.avoidToll
        val avoidFerry = options.avoidFerries
        val avoidHighway = options.avoidHighways
        val useDistance = options.useDistance

        val requestBody = mapOf(
            "format" to "osrm",
            "costing" to "auto",
            "banner_instructions" to true,
            "voice_instructions" to true,
            "language" to "es-ES",
            "alternates" to 3,
            "directions_options" to mapOf(
                "units" to "kilometers"
            ),
            "costing_options" to mapOf(
                "auto" to mapOf(
                    "top_speed" to 130,
                    "use_ferry" to if(avoidFerry) 0 else 1,
                    "use_highways" to if(avoidHighway) 0 else 1,
                    "use_tolls" to if(avoidToll) 0 else 1,
                    "use_distance" to useDistance
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

        val requestBodyJson = Gson().toJson(requestBody)
        val client = OkHttpClient()
        val url = VALHALLA_URL

        val request = Request.Builder()
            .header("User-Agent", "ML Nav - Android Sample App")
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "${e.message}")
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val raw = response.body?.string()
                    if (response.isSuccessful && raw != null) {
                        Log.d(TAG, raw)
                        val directionsResponse = DirectionsResponse.fromJson(raw)
                        continuation.resume(directionsResponse)
                    } else {
                        continuation.resume(
                            DirectionsResponse(
                                routes = emptyList(),
                                code = "NoRoute",
                                message = "Failed to fetch route",
                                waypoints = emptyList(),
                                uuid = ""
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "${e.message}")
                    continuation.resumeWithException(e)
                }
            }
        })
    }


    // 1. Volver a usar Photon para sugerencias
    private fun fetchSuggestion(query: String?) {
        val client = OkHttpClient()
        query?.length?.let {
            if (it > 2) {
                val url = "https://photon.komoot.io/api/?q=$query&limit=10&lang=es"
                val request = Request.Builder().url(url).build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d(TAG, "${e.message}")
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val responseStr = response.body?.string() ?: return
                        Log.d(TAG, responseStr)
                        val results = org.json.JSONObject(responseStr)
                            .optJSONArray("features") ?: return
                        val suggestions = mutableListOf<Suggestion>()
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val properties = item.optJSONObject("properties")
                            val geometry = item.optJSONObject("geometry")
                            val coordinates = geometry?.optJSONArray("coordinates")
                            val lon = coordinates?.optDouble(0) ?: 0.0
                            val lat = coordinates?.optDouble(1) ?: 0.0
                            val name = properties?.optString("name") ?: ""
                            val city = properties?.optString("city") ?: ""
                            val state = properties?.optString("state") ?: ""
                            val country = properties?.optString("country") ?: ""
                            suggestions.add(Suggestion(name, city, state, country, lon, lat))
                        }
                        requireActivity().runOnUiThread {
                            adapter.updateData(suggestions)
                        }
                        if (results.length() > 0) {
                            val first = results.getJSONObject(0)
                            val geometry = first.optJSONObject("geometry")
                            val coordinates = geometry?.optJSONArray("coordinates")
                            val lon = coordinates?.optDouble(0) ?: 0.0
                            val lat = coordinates?.optDouble(1) ?: 0.0
                            destinationPoint = Point(lon, lat)
                        }
                    }
                })
            }
        }
    }

    // 2. Función para borrar la caché de mapas
    private fun borrarCacheMapas() {
        try {
            val cacheDir = requireContext().cacheDir
            cacheDir?.let {
                it.deleteRecursively()
                Toast.makeText(requireContext(), "Caché de mapas borrada.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al borrar la caché: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDestination() {

        binding.map.getMapAsync { map ->
            map.getStyle { style ->
                // Comprobación de permisos antes de cargar la ruta
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    loadRoute(map, style, destinationPoint)
                }
            }
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun updateRouteOptionsDuringNavigation() {
        val map = currentMap ?: return
        val style = currentStyle ?: return
        val navigation = mlNavigation ?: return
        val userLocation = map.locationComponent.lastKnownLocation?.let {
            Point(it.longitude, it.latitude)
        } ?: return

        if(!isNavigating) return

        mlNavigation?.stopNavigation()

        lifecycleScope.launch {
            try {
                val directionsResponse = withContext(Dispatchers.IO) {
                    fetchRoute(userLocation, destinationPoint)
                }

                if (directionsResponse.routes.isNotEmpty()) {
                    val newRoute = directionsResponse.routes.first().copy(
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
                            language = "es-ES",
                            coordinates = listOf(
                                userLocation,
                                destinationPoint
                            ),
                            requestUuid = "0000-0000-0000-0000"
                        )
                    )
                    Log.d("Route", "$selectedRoute")

                    style.removeLayer("$ROUTE_LAYER_ID-$selectedRouteIndex" )
                    style.removeSource("$ROUTE_SOURCE_ID-$selectedRouteIndex")

                    drawRoute(map, style, listOf(newRoute))

                    selectedRoute = newRoute
                    navigation.startNavigation(newRoute)

                    binding.startNavButton.visibility = View.GONE

                    mlNavigation?.addProgressChangeListener { location, routeProgress ->
                        val selectedRouteIndex = 0

                        val source = style.getSourceAs<GeoJsonSource>("$ROUTE_SOURCE_ID-$selectedRouteIndex")

                        val fullLine = LineString(routeProgress.directionsRoute.geometry, Constants.PRECISION_6)

                        val currentPoint = Point(location.longitude, location.latitude)

                        val endPoint = fullLine.coordinates.last()
                        val remaining = TurfMisc.lineSlice(currentPoint, endPoint, fullLine)

                        source?.setGeoJson(remaining.toJvm())
                    }

                    showEta(newRoute)
                }
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }
        }
    }


    private fun drawRoute(map: MapLibreMap ,style: Style, routes: List<DirectionsRoute>) {
        routes.forEachIndexed { index, route ->
            style.removeLayer("$ROUTE_LAYER_ID-$index")
            style.removeSource("$ROUTE_SOURCE_ID-$index")
        }

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
            binding.bottomSheet.bottomLayout.visibility = View.VISIBLE
            binding.routeOptionsSheet.visibility = View.GONE
            binding.navRouteSheet.visibility = View.VISIBLE

        }
    }


    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(location: Point, map: MapLibreMap, style: Style) {
        map.locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), style)
                .useDefaultLocationEngine(false)
                .useSpecializedLocationLayer(true)
                .build()
        )

        map.locationComponent.isLocationComponentEnabled = true
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                17.0
            ),
            1500
        )


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

    private fun resetCameraState(location: Point, map: MapLibreMap) {
        if (map.locationComponent.isLocationComponentActivated) {

            map.locationComponent.cameraMode = CameraMode.NONE
            map.locationComponent.renderMode = RenderMode.NORMAL

            val latLng = LatLng(location.latitude, location.longitude)

            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(latLng)
                        .zoom(14.0)
                        .tilt(0.0)
                        .build()
                ),
                1000
            )
        }
    }

    private fun startNavigationNotification(instruction: String, iconRes: Int) {
        val context = requireContext().applicationContext
        val intent = Intent(context, NavigationNotificationService::class.java)
        intent.putExtra(NavigationNotificationService.EXTRA_INSTRUCTION, instruction)
        intent.putExtra(NavigationNotificationService.EXTRA_ICON_RES, iconRes)
        context.startForegroundService(intent)
    }

    private fun updateNavigationNotification(instruction: String, iconRes: Int) {
        val context = requireContext().applicationContext
        val intent = Intent(context, NavigationNotificationService::class.java)
        intent.putExtra(NavigationNotificationService.EXTRA_INSTRUCTION, instruction)
        intent.putExtra(NavigationNotificationService.EXTRA_ICON_RES, iconRes)
        context.startService(intent)
    }

    private fun stopNavigationNotification() {
        val context = requireContext().applicationContext
        val intent = Intent(context, NavigationNotificationService::class.java)
        context.stopService(intent)
    }

    /**
     * Descarga manual de regiones offline NO soportada en esta versión de MapLibre.
     * La caché automática está activa por defecto.
     */
    // private fun descargarRegionOffline() { ... }
}
