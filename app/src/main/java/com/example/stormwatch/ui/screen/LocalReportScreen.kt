package com.example.stormwatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stormwatch.LocalIsSerbian
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.label
import com.example.stormwatch.t
import com.example.stormwatch.viewmodel.MainViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalReportScreen(
    viewModel: MainViewModel,
    reportId: String,
    onBack: () -> Unit
) {
    val report by viewModel.selectedReport.collectAsState()
    val owner by viewModel.selectedReportOwner.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()

    val isSerbian = LocalIsSerbian.current

    LaunchedEffect(reportId) {
        viewModel.openReport(reportId)
    }

    val background = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))

    Box(Modifier.fillMaxSize().background(background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t(isSerbian, "Detalji Izveštaja", "Report Details"), color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            val r = report
            if (r == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                color = Color.White.copy(0.2f)
                            ) {
                                coil.compose.AsyncImage(
                                    model = r.authorPhotoUrl,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (r.authorPhotoUrl.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(r.userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(text = r.userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("${t(isSerbian, "Nedeljni rezultat:", "Weekly score:")} ${owner?.weeklyScore ?: 0}", color = Color.White.copy(0.7f), fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TrophyItem("🥇", owner?.goldTrophies ?: 0)
                                TrophyItem("🥈", owner?.silverTrophies ?: 0)
                                TrophyItem("🥉", owner?.bronzeTrophies ?: 0)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val pos = remember(r.latitude, r.longitude) { LatLng(r.latitude, r.longitude) }
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(pos, 15f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState,
                            properties = MapProperties(isMyLocationEnabled = false),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false)
                        ) {
                            Marker(
                                state = rememberMarkerState(position = pos),
                                title = r.parametar.label(isSerbian)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            val prikazniParametar = if (r.parametar == WeatherParameter.OTHER && !r.customParameterName.isNullOrBlank()) {
                                r.customParameterName
                            } else {
                                r.parametar.label(isSerbian)
                            }

                            Text(
                                text = prikazniParametar,
                                color = Color.Yellow, fontSize = 22.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(r.description, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp)

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "${t(isSerbian, "Trajanje:", "Duration:")} ${r.durationHours}h",
                                color = Color.White.copy(0.7f), fontSize = 14.sp
                            )

                            Spacer(Modifier.height(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👍 ${r.likes}", color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(16.dp))
                                Text("👎 ${r.dislikes}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    val isOwner = r.userName == currentUsername

                    if (isOwner) {
                        Button(
                            onClick = {
                                viewModel.deleteReport(r.id)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B2B).copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(t(isSerbian, "OBRIŠI IZVEŠTAJ", "DELETE REPORT"), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionButton(
                                text = t(isSerbian, "Korisno", "Helpful"),
                                icon = "👍",
                                color = Color(0xFF43A047),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.likeReport(r.id) }

                            ActionButton(
                                text = t(isSerbian, "Netačno", "Inaccurate"),
                                icon = "👎",
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.weight(1f)
                            ) { viewModel.dislikeReport(r.id) }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun TrophyItem(emoji: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(emoji, fontSize = 18.sp)
        Text(count.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionButton(text: String, icon: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("$icon $text", color = Color.White, fontWeight = FontWeight.Bold)
    }
}