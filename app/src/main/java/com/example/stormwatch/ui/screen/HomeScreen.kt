package com.example.stormwatch.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.floorToHour
import com.example.stormwatch.data.model.domain.DailyForecast
import com.example.stormwatch.data.model.domain.HourlyForecast
import com.example.stormwatch.data.model.domain.UserProfile
import com.example.stormwatch.util.LocationService
import com.example.stormwatch.util.isSameDay
import com.example.stormwatch.viewmodel.MainViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private fun isInNext24HoursByHour(ts: Long, nowTs: Long): Boolean {
    val start = floorToHour(nowTs)
    val endExclusive = start + 24L * 60L * 60L * 1000L
    return ts in start until endExclusive
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenReport: (String) -> Unit,
    onOpenMap: () -> Unit,
    onCreateReport: (selectedHourTimestamp: Long) -> Unit
) {
    val context = LocalContext.current

    val weather by viewModel.weather.collectAsState()
    val localReports by viewModel.localReports.collectAsState()

    var selectedDay by remember { mutableStateOf<DailyForecast?>(null) }
    var selectedHour by remember { mutableStateOf<HourlyForecast?>(null) }

    // CLOCK TICK (da se osvežava sat i “next 24h”)
    val timeNow by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    fun hasLocationPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var locationGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationGranted = fine || coarse
    }

    LaunchedEffect(Unit) {
        if (!locationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ======= LIVE LOCATION =======
    val locationService = remember { LocationService(context) }
    var myLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(locationGranted) {
        if (!locationGranted) return@LaunchedEffect
        locationService.locationUpdates().collect { loc ->
            myLocation = loc
            viewModel.setUserLocation(loc) // ✅ ovde ide
        }
    }

    // ======= BACKGROUND =======
    val backgroundGradient = when {
        weather?.current?.condition?.contains("Rain", true) == true ->
            Brush.verticalGradient(listOf(Color(0xFF203A43), Color(0xFF2C5364)))
        weather?.current?.condition?.contains("Snow", true) == true ->
            Brush.verticalGradient(listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)))
        else ->
            Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))
    }

    LaunchedEffect(weather, timeNow) {
        val data = weather ?: return@LaunchedEffect
        val nowTs = timeNow
        val nowHourTs = floorToHour(nowTs)

        selectedDay = data.daily.firstOrNull { isSameDay(it.date, nowTs) } ?: data.daily.firstOrNull()

        val day = selectedDay
        val hoursThatDay = data.hourly
            .filter { h -> day != null && isSameDay(h.timestamp, day.date) }
            .sortedBy { it.timestamp }
            .take(24)

        selectedHour = hoursThatDay.firstOrNull { floorToHour(it.timestamp) == nowHourTs }
            ?: hoursThatDay.firstOrNull()
    }

    val hoursForSelectedDay24 = remember(weather, selectedDay) {
        val data = weather ?: return@remember emptyList()
        val day = selectedDay ?: return@remember emptyList()
        data.hourly
            .filter { isSameDay(it.timestamp, day.date) }
            .sortedBy { it.timestamp }
            .take(24)
    }

    val hasHourlyForDay = hoursForSelectedDay24.isNotEmpty()


    val reportsForSelectedHour = remember(localReports, selectedHour, hasHourlyForDay, timeNow) {
        if (!hasHourlyForDay) return@remember emptyList()

        val selectedTs = selectedHour?.timestamp ?: return@remember emptyList()
        val selectedHourTs = floorToHour(selectedTs)

        if (!isInNext24HoursByHour(selectedHourTs, timeNow)) return@remember emptyList()

        localReports.filter { r ->
            val startHour = floorToHour(r.startTime)
            val endExclusive = startHour + (r.durationHours.toLong() * 60L * 60L * 1000L)
            selectedHourTs in startHour until endExclusive
        }
    }

    val canAddReport = remember(selectedHour, hasHourlyForDay, timeNow) {
        if (!hasHourlyForDay) return@remember false
        val ts = selectedHour?.timestamp ?: return@remember false
        isInNext24HoursByHour(floorToHour(ts), timeNow)
    }


    val bigMapCamera = rememberCameraPositionState()
    var mapCameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(myLocation) {
        val loc = myLocation ?: return@LaunchedEffect
        if (!mapCameraInitialized) {
            bigMapCamera.position = CameraPosition.fromLatLngZoom(
                LatLng(loc.latitude, loc.longitude),
                13f
            )
            mapCameraInitialized = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "StormWatch",
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenMap) {
                            Icon(Icons.Default.Map, contentDescription = "Mapa", tint = Color.White)
                        }
                        IconButton(onClick = { /* settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {


                weather?.let { data ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(data.current.city, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${data.current.temperature}°", fontSize = 60.sp, fontWeight = FontWeight.Light, color = Color.White)
                            Text(data.current.condition, fontSize = 18.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }

                    // ======= DAILY =======
                    item {
                        Text(
                            "Daily",
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    item {
                        DailyForecastRow(
                            days = data.daily,
                            selected = selectedDay,
                            onDayClick = { day ->
                                selectedDay = day

                                val hoursThatDay = data.hourly
                                    .filter { isSameDay(it.timestamp, day.date) }
                                    .sortedBy { it.timestamp }
                                    .take(24)

                                selectedHour = hoursThatDay.firstOrNull()
                            }
                        )
                    }

                    // ======= HOURLY + LOCAL REPORTS =======
                    if (hasHourlyForDay) {

                        item {
                            Text(
                                "Hourly",
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        item {
                            HourlyForecastRow(
                                hours = hoursForSelectedDay24,
                                selected = selectedHour,
                                onHourClick = { h -> selectedHour = h }
                            )
                        }

                        // LOCAL REPORTS SECTION
                        item {
                            Text(
                                "Local Reports",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            LocalReportsBox(
                                canAddReport = canAddReport,
                                onAdd = {
                                    val ts = selectedHour?.timestamp ?: return@LocalReportsBox
                                    onCreateReport(ts) // ✅ vodi na create screen
                                },
                                reportsForSelectedHour = reportsForSelectedHour,
                                onOpenReport = onOpenReport
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Mapa reportova",
                        modifier = Modifier.padding(start = 16.dp, bottom = 10.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ReportsPreviewMap(
                        localReports = localReports,
                        locationGranted = locationGranted,
                        cameraState = bigMapCamera,
                        onOpenMap = onOpenMap
                    )
                }
                item {
                    val users by viewModel.allUsers.collectAsState()
                    WeeklyLeaderboard(users = users)
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }
}

@Composable
private fun LocalReportsBox(
    canAddReport: Boolean,
    onAdd: () -> Unit,
    reportsForSelectedHour: List<LocalWeatherReport>,
    onOpenReport: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(250.dp)
            .fillMaxWidth(),
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.fillMaxSize()) {

            if (canAddReport) {
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj", tint = Color.White)
                }
            }

            if (reportsForSelectedHour.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nema reporta za ovaj sat",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reportsForSelectedHour) { report ->
                        ReportListItem(
                            report = report,
                            onClick = { onOpenReport(report.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsPreviewMap(
    localReports: List<LocalWeatherReport>,
    locationGranted: Boolean,
    cameraState: CameraPositionState,
    onOpenMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(260.dp)
            .clickable { onOpenMap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = locationGranted),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false
            )
        ) {
            localReports.forEach { r ->
                Marker(
                    state = MarkerState(LatLng(r.latitude, r.longitude)),
                    title = r.parametar.toString(),
                    snippet = r.description.take(60)
                )
            }
        }
    }
}

@Composable
fun DailyForecastRow(
    days: List<DailyForecast>,
    selected: DailyForecast?,
    onDayClick: (DailyForecast) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(days.take(5)) { day ->
            val isSelected = selected?.date == day.date

            Card(
                modifier = Modifier
                    .width(110.dp)
                    .clickable { onDayClick(day) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        day.dayName,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${day.minTemp}° / ${day.maxTemp}°",
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.condition,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.Gray else Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyForecastRow(
    hours: List<HourlyForecast>,
    selected: HourlyForecast?,
    onHourClick: (HourlyForecast) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(hours) { hour ->
            val isSelected = selected?.timestamp == hour.timestamp

            Card(
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onHourClick(hour) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(
                        imageVector = if (hour.isDay) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${hour.temp}°",
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${hour.hour}:00",
                        fontSize = 10.sp,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportListItem(
    report: LocalWeatherReport,
    onClick: () -> Unit
) {
    val prikazniParametar = if (report.parametar == WeatherParameter.OTHER && !report.customParameterName.isNullOrBlank()) {
        report.customParameterName
    } else {
        report.parametar.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tip: $prikazniParametar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = "Trajanje: ${report.durationHours}h",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    report.description,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(8.dp))


                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👍 ${report.likes}", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("👎 ${report.dislikes}", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }

            Spacer(Modifier.width(10.dp))


            MiniMap(
                lat = report.latitude,
                lon = report.longitude
            )
        }
    }
}

@Composable
fun MiniMap(lat: Double, lon: Double) {
    val cameraState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, lon), 14f)
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(90.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false
            )
        ) {
            Marker(state = MarkerState(LatLng(lat, lon)))
        }
    }
}

@Composable
fun WeeklyLeaderboard(users: List<UserProfile>) {
    val sortedUsers = users.sortedByDescending { it.weeklyScore }.take(10)

    Surface(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Weekly Leaderboard 🏆", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            sortedUsers.forEachIndexed { index, user ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Medalja ili Rank
                    val badge = when(index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    Text(badge, modifier = Modifier.width(30.dp), color = Color.White)

                    Text(user.username, color = Color.White, modifier = Modifier.weight(1f))

                    Text(
                        "${user.weeklyScore} pts",
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index < sortedUsers.size - 1) {
                    Divider(color = Color.White.copy(0.1f))
                }
            }
        }
    }
}