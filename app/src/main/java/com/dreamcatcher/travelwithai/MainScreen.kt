package com.dreamcatcher.travelwithai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dreamcatcher.travelwithai.ui.theme.Blue500
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPadding
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingHalf
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingQuarter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    val mainViewModel: MainViewModel = viewModel(factory = BakingViewModelFactory(context))

    var appOpenCount = sharedPreferences.getInt("app_open_count", 0)
    sharedPreferences.edit().putInt("app_open_count", appOpenCount + 1).apply()

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (!locationPermissionState.status.isGranted) {
        LaunchedEffect(Unit) { locationPermissionState.launchPermissionRequest() }
    } else {
        val placeholderResult = stringResource(R.string.results_placeholder)
        var prompt by rememberSaveable { mutableStateOf("") }
        var result by rememberSaveable { mutableStateOf(placeholderResult) }
        val uiState by mainViewModel.uiState.collectAsState()

        // Loading bar animation
        val scope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()

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

        ReviewDialog()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(DefaultPadding)) {
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
                        start = DefaultPadding,
                        end = DefaultPadding,
                        top = DefaultPadding,
                        bottom = DefaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.action_start),
                        onClick = { mainViewModel.sendPrompt(MessageType.INITIAL) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = DefaultPadding,
                        vertical = DefaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.history_of_this_place),
                        onClick = { mainViewModel.sendPrompt(MessageType.HISTORY) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = DefaultPaddingQuarter),
                    )
                    ActionButton(
                        text = stringResource(R.string.restaurants_nearby),
                        onClick = { mainViewModel.sendPrompt(MessageType.RESTAURANTS) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = DefaultPaddingQuarter),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = DefaultPadding,
                        vertical = DefaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.tourist_spots),
                        onClick = { mainViewModel.sendPrompt(MessageType.TOURIST_SPOTS) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = DefaultPaddingQuarter),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = DefaultPadding,
                        vertical = DefaultPaddingHalf,
                    )
                ) {
                    ActionButton(
                        text = stringResource(R.string.safety_rules),
                        onClick = { mainViewModel.sendPrompt(MessageType.SAFETY) },
                        modifier = Modifier
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                            .padding(end = DefaultPaddingQuarter),
                        buttonColor = Color(0xFFD32F2F) // Safety red,
                    )
                }
            }

            item {
                imageBitmap?.let { bitmap ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DefaultPadding),
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
                        horizontal = DefaultPadding,
                        vertical = DefaultPaddingHalf,
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
                            .padding(end = DefaultPaddingQuarter),
                        buttonColor = Blue500,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(
                        start = DefaultPadding,
                        end = DefaultPadding,
                        top = DefaultPadding,
                        bottom = DefaultPaddingHalf,
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
                        .padding(DefaultPadding)) {
                        CircularProgressIndicator(
                            Modifier
                                .size(180.dp)
                                .align(Alignment.Center))
                    }
                    scope.launch {
                        lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DefaultPadding)
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
    modifier: Modifier = Modifier,
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

@Composable
fun ReviewDialog() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    val appOpenCount = sharedPreferences.getInt("app_open_count", 0)
    val hasReviewed = sharedPreferences.getBoolean("has_reviewed", false)
    val shouldShowDialog = (appOpenCount == 2 || appOpenCount == 4) && !hasReviewed
    var showDialog by remember { mutableStateOf(shouldShowDialog) }

    val confirmButtonClick = {
        requestReview(context, sharedPreferences)
        showDialog = false
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Would you like to leave a review?") },
            text = { Text("I'm Rafal, sole developer of the app. It would be my honour if you'd like to leave me a review. If you have any idea about how I could improve the app, please share in the comment. Thank you for using my app!") },
            confirmButton = { Button(onClick = confirmButtonClick) { Text("Yes") } },
            dismissButton = { Button(onClick = { showDialog = false }) { Text("No") } }
        )
    }
}

fun requestReview(context: Context, sharedPreferences: SharedPreferences) {
    sharedPreferences.edit().putBoolean("has_reviewed", true).apply()
    val reviewManager = ReviewManagerFactory.create(context)
    val reviewInfoTask: Task<ReviewInfo> = reviewManager.requestReviewFlow()
    reviewInfoTask.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            val reviewFlow = reviewManager.launchReviewFlow(context as Activity, reviewInfo)
            reviewFlow.addOnCompleteListener { }
        } else { } // Todo
    }
}
