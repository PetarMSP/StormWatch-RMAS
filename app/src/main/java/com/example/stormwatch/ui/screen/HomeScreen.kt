package com.example.stormwatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.domain.DailyForecast
import com.example.stormwatch.data.model.domain.HourlyForecast
import com.example.stormwatch.util.isSameDay
import com.example.stormwatch.viewmodel.MainViewModel
import java.util.Calendar

private fun floorToHour(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun isInNext24HoursByHour(ts: Long, nowTs: Long): Boolean {
    val start = floorToHour(nowTs)
    val endExclusive = start + 24L * 60L * 60L * 1000L
    return ts in start until endExclusive
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenReport: (String) -> Unit
) {
    val weather by viewModel.weather.collectAsState()
    val localReports by viewModel.localReports.collectAsState()

    var selectedDay by remember { mutableStateOf<DailyForecast?>(null) }
    var selectedHour by remember { mutableStateOf<HourlyForecast?>(null) }

    val backgroundGradient = when {
        weather?.current?.condition?.contains("Rain", true) == true ->
            Brush.verticalGradient(listOf(Color(0xFF203A43), Color(0xFF2C5364)))
        weather?.current?.condition?.contains("Snow", true) == true ->
            Brush.verticalGradient(listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)))
        else ->
            Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))
    }
    val timeNow by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            value = System.currentTimeMillis()
        }
    }

    // Default: danas + trenutni sat
    LaunchedEffect(weather) {
        val data = weather ?: return@LaunchedEffect
        val nowTs = timeNow
        val nowHourTs = floorToHour(nowTs)

        selectedDay = data.daily.firstOrNull { isSameDay(it.date, nowTs) } ?: data.daily.firstOrNull()

        val today = selectedDay
        val hoursThatDay = data.hourly
            .filter { h -> today != null && isSameDay(h.timestamp, today.date) }
            .sortedBy { it.timestamp }
            .take(24)

        selectedHour = hoursThatDay.firstOrNull { floorToHour(it.timestamp) == nowHourTs }
            ?: hoursThatDay.firstOrNull()
    }

    // 24h za selektovani dan
    val hoursForSelectedDay24 = remember(weather, selectedDay) {
        val data = weather ?: return@remember emptyList()
        val day = selectedDay ?: return@remember emptyList()

        data.hourly
            .filter { isSameDay(it.timestamp, day.date) }
            .sortedBy { it.timestamp }
            .take(24)
    }

    val hasHourlyForDay = hoursForSelectedDay24.isNotEmpty()

    // reportovi samo ako izabrani sat upada u "sada..+24h" i samo za taj sat
    val reportsForSelectedHour = remember(localReports, selectedHour, hasHourlyForDay) {
        if (!hasHourlyForDay) return@remember emptyList()

        val nowTs = timeNow
        val selectedTs = selectedHour?.timestamp ?: return@remember emptyList()
        val selectedHourTs = floorToHour(selectedTs)

        if (!isInNext24HoursByHour(selectedHourTs, nowTs)) return@remember emptyList()

        localReports.filter { r ->
            val startHour = floorToHour(r.startTime)
            val endExclusive = startHour + (r.durationHours.toLong() * 60L * 60L * 1000L)
            selectedHourTs in startHour until endExclusive
        }
    }

    val canAddReport = remember(selectedHour, hasHourlyForDay) {
        if (!hasHourlyForDay) return@remember false
        val nowTs = timeNow
        val ts = selectedHour?.timestamp ?: return@remember false
        isInNext24HoursByHour(floorToHour(ts), nowTs)
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
                        IconButton(onClick = { /* otvori settings meni */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                if (canAddReport) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.addLocalReport(
                                lat = 43.23,
                                lon = 21.59,
                                parameter = WeatherParameter.RAIN,
                                duration = 3,
                                description = "Report za ${selectedHour?.hour ?: "?"}:00"
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Report")
                    }
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {

                // Header (grad/temp/condition) - levo
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

                    // DAILY
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

                    // HOURLY + LOCAL REPORTS samo ako ima hourly za taj dan
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

                        if (canAddReport) {
                            item {
                                Text(
                                    "Local Reports",
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .height(250.dp)
                                        .fillMaxWidth(),
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (reportsForSelectedHour.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Nema reporta za ovaj sat",
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    } else {
                                        val scrollState = rememberScrollState()

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(scrollState)
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            reportsForSelectedHour.forEach { report ->
                                                ReportListItem(report = report, onClick = {
                                                   onOpenReport(report.id)
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(70.dp)) }
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
            // LEVO: text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Parametar: ${report.parametar}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Trajanje: ${report.durationHours}h", color = Color.White.copy(alpha = 0.85f))
                Text(
                    report.description,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text("👍 ${report.likes}   👎 ${report.dislikes}", color = Color.White.copy(alpha = 0.9f))
            }

            Spacer(Modifier.width(10.dp))

            // DESNO: mini mapa (za sada placeholder)
            MiniMapPlaceholder(
                lat = report.latitude,
                lon = report.longitude
            )
        }
    }
}

@Composable
fun MiniMapPlaceholder(lat: Double, lon: Double) {
    Surface(
        modifier = Modifier.size(width = 90.dp, height = 100.dp),
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("MAP", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}