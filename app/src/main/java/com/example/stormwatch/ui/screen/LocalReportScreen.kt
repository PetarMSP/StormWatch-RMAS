package com.example.stormwatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.stormwatch.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalReportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.selectedReport.collectAsState()
    val owner by viewModel.selectedReportOwner.collectAsState()
    val myUid = viewModel.currentUserId()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (report == null) {
                CircularProgressIndicator()
                return@Column
            }

            Text("Owner: ${owner?.username ?: report!!.userID}")
            Spacer(Modifier.height(12.dp))

            Text("Parametar: ${report!!.parametar}")
            Text("Trajanje: ${report!!.durationHours}h")
            Text(report!!.description)

            Spacer(Modifier.height(16.dp))

            val isMine = (report!!.userID == myUid)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isMine) {
                    Button(onClick = { viewModel.likeReport(report!!.id) }) { Text("Like") }
                    Button(onClick = { viewModel.dislikeReport(report!!.id) }) { Text("Dislike") }
                } else {
                    Button(onClick = { viewModel.deleteReport(report!!.id); onBack() }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}