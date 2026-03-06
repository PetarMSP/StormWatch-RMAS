package com.example.stormwatch.ui.screen

import android.graphics.*
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.stormwatch.LocalIsSerbian
import com.example.stormwatch.t
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.label
import com.example.stormwatch.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlin.math.cos
import kotlin.math.roundToInt


fun distanceMeters(a: LatLng, b: LatLng): Float {
    val res = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res)
    return res[0]
}

private fun boundsForRadius(center: LatLng, radiusMeters: Double): LatLngBounds {
    val latDelta = radiusMeters / 111_000.0
    val lngDelta = radiusMeters / (111_000.0 * cos(Math.toRadians(center.latitude)).coerceAtLeast(0.000001))
    val sw = LatLng(center.latitude - latDelta, center.longitude - lngDelta)
    val ne = LatLng(center.latitude + latDelta, center.longitude + lngDelta)
    return LatLngBounds(sw, ne)
}

private suspend fun loadMarkerIcon(
    context: android.content.Context,
    url: String,
    parameter: WeatherParameter,
    userName: String
): BitmapDescriptor {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
    val result = (loader.execute(request) as? SuccessResult)?.drawable
    val originalBitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap

    val size = 150
    val imageSize = 110
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply { isAntiAlias = true }

    val borderColor = when (parameter) {
        WeatherParameter.RAIN -> android.graphics.Color.parseColor("#1976D2")
        WeatherParameter.SNOW -> android.graphics.Color.parseColor("#B3E5FC")
        WeatherParameter.TEMPERATURE -> android.graphics.Color.parseColor("#FF5722")
        WeatherParameter.WIND -> android.graphics.Color.parseColor("#607D8B")
        else -> android.graphics.Color.RED
    }

    paint.color = borderColor
    canvas.drawCircle(size / 2f, size / 2f, (imageSize / 2f) + 6, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, (imageSize / 2f) + 2, paint)

    val path = Path().apply { addCircle(size / 2f, size / 2f, imageSize / 2f, Path.Direction.CCW) }
    canvas.save()
    canvas.clipPath(path)

    if (originalBitmap != null) {
        canvas.drawBitmap(Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, false), (size - imageSize) / 2f, (size - imageSize) / 2f, null)
    } else {
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawCircle(size / 2f, size / 2f, imageSize / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(userName.take(1).uppercase(), size / 2f, (size / 2f) + 20f, paint)
    }
    canvas.restore()

    val emoji = when (parameter) {
        WeatherParameter.RAIN -> "💧"
        WeatherParameter.SNOW -> "❄️"
        WeatherParameter.TEMPERATURE -> "🔥"
        WeatherParameter.WIND -> "🌬️"
        WeatherParameter.FOG -> "🌫️"
        else -> "⚠️"
    }

    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    canvas.drawCircle(size * 0.82f, size * 0.82f, 28f, paint)
    val textPaint = Paint().apply { textSize = 40f; textAlign = Paint.Align.CENTER }
    canvas.drawText(emoji, size * 0.82f, size * 0.87f, textPaint)

    return BitmapDescriptorFactory.fromBitmap(output)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onOpenReport: (String) -> Unit,
    onBack: () -> Unit
) {
    val isSerbian = LocalIsSerbian.current
    val context = LocalContext.current
    val reports by viewModel.localReports.collectAsState()
    val myLoc by viewModel.userLocation.collectAsState()
    val myLatLng = myLoc?.let { LatLng(it.latitude, it.longitude) }

    val markerIcons = remember { mutableStateMapOf<String, BitmapDescriptor>() }

    var authorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var typeFilter by rememberSaveable { mutableStateOf<WeatherParameter?>(null) }
    var radiusKm by rememberSaveable { mutableFloatStateOf(3.0f) }
    var radiusDraft by rememberSaveable { mutableStateOf(radiusKm) }
    var radiusDragging by remember { mutableStateOf(false) }
    var timeWindowHours by rememberSaveable { mutableIntStateOf(24) }

    LaunchedEffect(radiusKm) { if (!radiusDragging) radiusDraft = radiusKm }

    val backgroundGradient = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))

    val filtered = remember(reports, authorFilter, typeFilter, timeWindowHours, myLatLng, radiusKm) {
        val now = System.currentTimeMillis()
        val radiusMeters = radiusKm * 1000f
        val timeLimitMs = timeWindowHours.toLong() * 3600000L

        reports.filter { r ->
            val matchesAuthor = authorFilter == null || r.userName == authorFilter
            val matchesType = typeFilter == null || r.parametar == typeFilter
            val diffToStart = r.startTime - now
            val hasStartedAndActive = diffToStart <= 0 && (r.startTime + r.durationHours * 3600000L) > now
            val startsInFutureWithinWindow = diffToStart in 1..timeLimitMs
            val isTimeMatch = hasStartedAndActive || startsInFutureWithinWindow
            val isNear = myLatLng?.let { distanceMeters(it, LatLng(r.latitude, r.longitude)) <= radiusMeters } ?: true

            matchesAuthor && matchesType && isTimeMatch && isNear
        }
    }

    LaunchedEffect(filtered) {
        filtered.forEach { r ->
            if (!markerIcons.containsKey(r.id)) {
                markerIcons[r.id] = loadMarkerIcon(context, r.authorPhotoUrl, r.parametar, r.userName)
            }
        }
    }

    val cameraState = rememberCameraPositionState()
    var cameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(myLatLng) {
        val me = myLatLng ?: return@LaunchedEffect
        if (!cameraInitialized) {
            cameraState.position = CameraPosition.fromLatLngZoom(me, 13f)
            cameraInitialized = true
        }
    }

    suspend fun zoomToRadius(center: LatLng, km: Float) {
        val bounds = boundsForRadius(center, km.toDouble() * 1000.0)
        cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t(isSerbian, "Mapa izveštaja", "Report Map"), fontWeight = FontWeight.Black, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            myLatLng?.let { cameraState.position = CameraPosition.fromLatLngZoom(it, 13f) }
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = t(isSerbian, "Moja lokacija", "My Location"), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                FilterPanel(
                    isSerbian = isSerbian,
                    reports = reports,
                    authorFilter = authorFilter,
                    onAuthorChange = { authorFilter = it },
                    typeFilter = typeFilter,
                    onTypeChange = { typeFilter = it },
                    radiusKm = radiusDraft,
                    onRadiusKmChange = { v -> radiusDragging = true; radiusDraft = v },
                    onRadiusFinish = { radiusDragging = false; radiusKm = radiusDraft },
                    timeWindowHours = timeWindowHours,
                    onTimeWindowChange = { timeWindowHours = it },
                    resultsCount = filtered.size
                )

                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        properties = MapProperties(isMyLocationEnabled = true),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        myLatLng?.let { me ->
                            Circle(center = me, radius = radiusKm.toDouble() * 1000.0, strokeColor = Color.White, fillColor = Color.White.copy(0.1f))
                        }
                        filtered.forEach { r ->
                            Marker(
                                state = rememberMarkerState(position = LatLng(r.latitude, r.longitude)),
                                title = r.userName,
                                snippet = r.parametar.label(isSerbian),
                                icon = markerIcons[r.id] ?: BitmapDescriptorFactory.defaultMarker(),
                                onClick = { onOpenReport(r.id); true }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(radiusKm, myLatLng) {
        val me = myLatLng ?: return@LaunchedEffect
        zoomToRadius(me, radiusKm)
    }
}

@Composable
private fun FilterPanel(
    isSerbian: Boolean,
    reports: List<LocalWeatherReport>,
    authorFilter: String?,
    onAuthorChange: (String?) -> Unit,
    typeFilter: WeatherParameter?,
    onTypeChange: (WeatherParameter?) -> Unit,
    radiusKm: Float,
    onRadiusKmChange: (Float) -> Unit,
    onRadiusFinish: () -> Unit,
    timeWindowHours: Int,
    onTimeWindowChange: (Int) -> Unit,
    resultsCount: Int
) {
    val authors = remember(reports) { reports.map { it.userName }.distinct().filter { it.isNotBlank() }.sorted() }
    val allLabel = t(isSerbian, "Svi", "All")

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(isSerbian, "Filteri", "Filters"), fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("${t(isSerbian, "Rezultati", "Results")}: $resultsCount", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownFilter(
                    label = t(isSerbian, "Autor", "Author"),
                    value = authorFilter ?: allLabel,
                    items = listOf(allLabel) + authors,
                    onSelect = { v -> onAuthorChange(if (v == allLabel) null else v) },
                    modifier = Modifier.weight(1f)
                )

                val typeItems = listOf(allLabel) + WeatherParameter.entries.map { it.label(isSerbian) }
                DropdownFilter(
                    label = t(isSerbian, "Tip", "Type"),
                    value = typeFilter?.label(isSerbian) ?: allLabel,
                    items = typeItems,
                    onSelect = { v ->
                        val selected = WeatherParameter.entries.find { it.label(isSerbian) == v }
                        onTypeChange(selected)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadiusSlider(
                    label = t(isSerbian, "Radijus", "Radius"),
                    value = radiusKm,
                    onValueChange = onRadiusKmChange,
                    onValueChangeFinished = onRadiusFinish,
                    valueRange = 0f..10f,
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                HoursSlider(
                    label = t(isSerbian, "Prikaži narednih", "Show next"),
                    value = timeWindowHours,
                    onValueChange = onTimeWindowChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RadiusSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        val displayValue = if (value == 0f) "0" else "%.1f".format(value)
        Text("$label: $displayValue $unit", color = Color.White, fontSize = 10.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )
    }
}

@Composable
private fun HoursSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text("$label: $value h", color = Color.White, fontSize = 10.sp)
        Slider(
            value = value.toFloat(),
            onValueChange = { f -> onValueChange(f.roundToInt().coerceIn(1, 24)) },
            valueRange = 1f..24f,
            steps = 22,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownFilter(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.White.copy(0.7f)) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(0.5f),
                unfocusedBorderColor = Color.White.copy(0.3f)
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item); expanded = false })
            }
        }
    }
}