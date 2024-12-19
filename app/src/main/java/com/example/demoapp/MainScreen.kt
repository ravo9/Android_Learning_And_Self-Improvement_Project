package com.example.demoapp

import android.Manifest
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

val images = arrayOf(
    R.drawable.travel_1,
    R.drawable.travel_2,
    R.drawable.travel_3,
    R.drawable.travel_4,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BakingScreen() {
    val context = LocalContext.current
    val bakingViewModel: BakingViewModel = viewModel(
        factory = BakingViewModelFactory(context)
    )

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    if (!locationPermissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            locationPermissionState.launchPermissionRequest()
        }
    } else {
        val buttonHeight = 56.dp
        val placeholderResult = stringResource(R.string.results_placeholder)
        val selectedImage = remember { mutableIntStateOf(0) }
        var prompt by rememberSaveable { mutableStateOf("") }
        var result by rememberSaveable { mutableStateOf(placeholderResult) }
        val uiState by bakingViewModel.uiState.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.main_screen_title).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.8.sp),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
            )

            LazyRow(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(images) { index, image ->
                    val roundedCornersValue = 16.dp
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .requiredSize(130.dp),
                        shape = RoundedCornerShape(roundedCornersValue),
                        elevation = CardDefaults.cardElevation(50.dp),
                    ) {
                        Image(
                            painter = painterResource(image),
                            contentDescription = stringResource(R.string.ai_image_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(all = 16.dp)) {
                Button(
                    onClick = { bakingViewModel.sendLocationBasedPrompt() },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                        .height(buttonHeight),
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                ) {
                    Text(text = stringResource(R.string.action_start))
                }
            }

            Row(modifier = Modifier.padding(all = 16.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text(stringResource(R.string.prompt_placeholder)) },
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                )

                Button(
                    onClick = {
                        val bitmap = BitmapFactory.decodeResource(
                            context.resources,
                            images[selectedImage.intValue]
                        )
                        bakingViewModel.sendPrompt(bitmap, prompt)
                    },
                    enabled = prompt.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(buttonHeight),
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                ) {
                    Text(text = stringResource(R.string.action_go))
                }
            }

            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                var textColor = MaterialTheme.colorScheme.onSurface
                if (uiState is UiState.Error) {
                    textColor = MaterialTheme.colorScheme.error
                    result = (uiState as UiState.Error).errorMessage
                } else if (uiState is UiState.Success) {
                    textColor = MaterialTheme.colorScheme.onSurface
                    result = (uiState as UiState.Success).outputText
                }
                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    textAlign = TextAlign.Start,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }
        }
    }
}
