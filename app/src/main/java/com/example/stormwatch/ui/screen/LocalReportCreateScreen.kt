package com.example.stormwatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.stormwatch.t
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalReportCreateScreen(
    viewModel: MainViewModel,
    selectedTimestamp: Long,
    onBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WeatherParameter.RAIN) }
    var customTypeName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var durationSliderValue by remember { mutableFloatStateOf(3f) }
    val isSerbianNow = LocalIsSerbian.current

    val currentEndTime = remember(durationSliderValue) {
        selectedTimestamp + (durationSliderValue.toLong() * 3600000L)
    }

    val background = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))

    Box(Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.padding(24.dp).statusBarsPadding()) {

            Text(t(isSerbianNow,"Novi Izveštaj","New Report"), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${t(isSerbianNow,"Početak:","Start:")} ${formatTime(selectedTimestamp)}h", color = Color.White.copy(0.7f), fontSize = 14.sp)

            Spacer(Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text(t(isSerbianNow,"TIP POJAVE","Type of Report"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedType.name,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(0.5f),
                                unfocusedBorderColor = Color.White.copy(0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            WeatherParameter.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = { selectedType = type; expanded = false }
                                )
                            }
                        }
                    }

                    if (selectedType == WeatherParameter.OTHER) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customTypeName,
                            onValueChange = { customTypeName = it },
                            placeholder = { Text(t(isSerbianNow,"Šta ste uočili?","Description of the observation?"), color = Color.White.copy(0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Yellow.copy(0.7f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(isSerbianNow,"TRAJANJE","Duration"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f))
                        Spacer(Modifier.weight(1f))
                        Text("${durationSliderValue.toInt()}h", color = Color.Yellow, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = durationSliderValue,
                        onValueChange = { durationSliderValue = it },
                        valueRange = 1f..24f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Yellow,
                            inactiveTrackColor = Color.White.copy(0.2f)
                        )
                    )

                    Text(
                        "${t(isSerbianNow, "Ističe u:", "Ends in:")} ${formatTime(currentEndTime)}h",
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(t(isSerbianNow,"OPIS","Description"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(0.5f),
                            unfocusedBorderColor = Color.White.copy(0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.addLocalReport(
                        lat = viewModel.userLocation.value?.latitude ?: 0.0,
                        lon = viewModel.userLocation.value?.longitude ?: 0.0,
                        parameter = selectedType,
                        customName = customTypeName,
                        description = description,
                        endTime = currentEndTime,
                        clickedTimestamp = selectedTimestamp
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2193b0))
            ) {
                Text(t(isSerbianNow,"OBJAVI","PUBLISH"), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
fun formatTime(ts: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(ts))
}