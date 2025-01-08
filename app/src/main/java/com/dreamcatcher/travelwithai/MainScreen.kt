package com.dreamcatcher.travelwithai

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.demoapp.ui.theme.Blue500
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val mainViewModel: MainViewModel = viewModel(
        factory = BakingViewModelFactory(LocalContext.current)
    )

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (!locationPermissionState.status.isGranted) {
        LaunchedEffect(Unit) { locationPermissionState.launchPermissionRequest() }
    } else {
        val defaultPaddingHalf = 8.dp
        val defaultPaddingQuarter = 4.dp
        val defaultPadding = 16.dp
        val placeholderResult = stringResource(R.string.results_placeholder)
        var prompt by rememberSaveable { mutableStateOf("") }
        var result by rememberSaveable { mutableStateOf(placeholderResult) }
        val uiState by mainViewModel.uiState.collectAsState()

        // Image State for Captured Picture
        var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

        // Launch Camera Contract
        val takePictureLauncher = rememberLauncherForActivityResult (
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                imageBitmap = bitmap
                mainViewModel.sendPrompt(MessageType.PHOTO, null, bitmap)
            } else {
                // Todo
            }
        }


        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(defaultPadding)) {
                    Text(
                        text = stringResource(R.string.main_screen_title).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.8.sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 20.dp),
                    )
                }
            }

            item {
                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(mainViewModel.getAIGeneratedImages()) { index, image ->
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
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        start = defaultPadding,
                        end = defaultPadding,
                        top = defaultPadding,
                        bottom = defaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.action_start),
                        onClick = { mainViewModel.sendPrompt(MessageType.INITIAL) },
                        modifier = Modifier,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = defaultPadding,
                        vertical = defaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.history_of_this_place),
                        onClick = { mainViewModel.sendPrompt(MessageType.HISTORY) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = defaultPaddingQuarter),
                    )
                    ActionButton(
                        text = stringResource(R.string.restaurants_nearby),
                        onClick = { mainViewModel.sendPrompt(MessageType.RESTAURANTS) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = defaultPaddingQuarter),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = defaultPadding,
                        vertical = defaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.tourist_spots),
                        onClick = { mainViewModel.sendPrompt(MessageType.TOURIST_SPOTS) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = defaultPaddingQuarter),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = defaultPadding,
                        vertical = defaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.safety_rules),
                        onClick = { mainViewModel.sendPrompt(MessageType.SAFETY) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = defaultPaddingQuarter),
                        buttonColor = Color(0xFFD32F2F) // Safety red,
                    )
                }
            }

            item {
                imageBitmap?.let { bitmap ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(defaultPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = defaultPadding,
                        vertical = defaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.take_a_picture),
                        onClick = {
                            if (!cameraPermissionState.status.isGranted) {
                                cameraPermissionState.launchPermissionRequest()
                            } else {
                                takePictureLauncher.launch(null)
                            }
                        },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = defaultPaddingQuarter),
                        buttonColor = Blue500,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        start = defaultPadding,
                        end = defaultPadding,
                        top = defaultPadding,
                        bottom = defaultPaddingHalf,
                    )
                ) {
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

                    ActionButton(
                        text = stringResource(R.string.action_go),
                        onClick = { mainViewModel.sendPrompt(MessageType.CUSTOM, prompt) },
                        enabled = prompt.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .fillMaxWidth(0.225f)
                    )
                }
            }

            item {
                if (uiState is UiState.Loading) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(defaultPadding)) {
                        CircularProgressIndicator(
                            Modifier
                                .size(180.dp)
                                .align(Alignment.Center))
                    }
                } else {
                    val (textColor, result) = when (val state = uiState) {
                        is UiState.Error -> Pair(MaterialTheme.colorScheme.error, state.errorMessage)
                        is UiState.Success -> Pair(MaterialTheme.colorScheme.onSurface, state.outputText)
                        else -> Pair(MaterialTheme.colorScheme.onSurface, result)
                    }
                    Text(
                        text = result,
                        textAlign = TextAlign.Start,
                        color = textColor,
//                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(defaultPadding)
                            .heightIn(min = 0.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val buttonHeight = 54.dp
    val keyboardController = LocalSoftwareKeyboardController.current
    Button(
        onClick = {
            onClick()
            keyboardController?.hide()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        elevation = ButtonDefaults.elevatedButtonElevation()
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}