package com.dreamcatcher.travelwithai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.dreamcatcher.travelwithai.ui.theme.UIConstants
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPadding
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingHalf
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingQuarter
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultRoundedCornerValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = BakingViewModelFactory(context))
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    updateAppOpeningsCounter(context)

    RequestPermission(Manifest.permission.ACCESS_FINE_LOCATION) {
        val uiState by viewModel.uiState.collectAsState()
        var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val lazyListState = rememberLazyListState()
        val takePictureLauncher = rememberLauncherForActivityResult (
            ActivityResultContracts.TakePicturePreview()
        ) {
            it?.let {
                imageBitmap = it
                viewModel.sendPrompt(MessageType.PHOTO, null, it)
            }
        }

        ReviewDialog()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { ScreenTitle() }
            item { ImageCarousel(viewModel) }
            item { ActionRow(listOf(R.string.action_start to { viewModel.sendPrompt(MessageType.INITIAL) })) }
            item { ActionRow(listOf(
                R.string.history_of_this_place to { viewModel.sendPrompt(MessageType.HISTORY) },
                R.string.restaurants_nearby to { viewModel.sendPrompt(MessageType.RESTAURANTS) },
            ))}
            item { ActionRow(listOf(R.string.tourist_spots to { viewModel.sendPrompt(MessageType.TOURIST_SPOTS) })) }
            item { ActionRow(
                listOf(R.string.safety_rules to { viewModel.sendPrompt(MessageType.SAFETY) }),
                buttonColor = Color(0xFFD32F2F), // Safety red,
            )}
            item { ImagePreview(imageBitmap) }
            item { ActionRow(
                listOf(R.string.take_a_picture to {
                    if (cameraPermissionState.status.isGranted) takePictureLauncher.launch(null)
                    else cameraPermissionState.launchPermissionRequest()
                }),
                buttonColor = Blue500,
            )}
            item { PromptInput(viewModel) }
            item { UiStateDisplay(uiState, lazyListState) }
        }
    }
}

@Composable
fun ScreenTitle() {
    Box(modifier = Modifier.fillMaxWidth().padding(DefaultPadding).padding(top = 20.dp)) {
        Text(
            text = stringResource(R.string.main_screen_title).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.8.sp),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun ImageCarousel(mainViewModel: MainViewModel) {
    LazyRow(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(mainViewModel.getAIGeneratedImages()) { _, image ->
            Card(
                modifier = Modifier.padding(DefaultPaddingHalf).requiredSize(130.dp),
                shape = RoundedCornerShape(UIConstants.DefaultRoundedCornerValue),
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

@Composable
fun ActionButton(
    text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Button(
        onClick = {
            onClick()
            keyboardController?.hide()
        },
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        elevation = ButtonDefaults.elevatedButtonElevation()
    ) {
        Text(text = stringResource(text), textAlign = TextAlign.Center)
    }
}

@Composable
fun ActionRow(
    buttons: List<Pair<Int, () -> Unit>>,
    modifier: Modifier = Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.padding(horizontal = DefaultPadding, vertical = DefaultPaddingHalf),
        horizontalArrangement = Arrangement.spacedBy(DefaultPaddingQuarter)
    ) {
        buttons.forEach { (text, action) ->
            ActionButton(
                text = text,
                onClick = action,
                modifier = Modifier.weight(1.0f).align(Alignment.CenterVertically),
                buttonColor = buttonColor
            )
        }
    }
}

@Composable
fun ImagePreview(imageBitmap: Bitmap?) {
    imageBitmap?.let { bitmap ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(DefaultPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(DefaultRoundedCornerValue)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun PromptInput(mainViewModel: MainViewModel) {
    var prompt by rememberSaveable { mutableStateOf("") }
    Row(modifier = Modifier.padding(DefaultPadding).padding(top = DefaultPaddingHalf)) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text(stringResource(R.string.prompt_placeholder)) },
            modifier = Modifier.weight(0.8f).padding(end = DefaultPadding).align(Alignment.CenterVertically),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
        )
        ActionButton(
            text = R.string.action_go,
            onClick = { mainViewModel.sendPrompt(MessageType.CUSTOM, prompt) },
            enabled = prompt.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterVertically).fillMaxWidth(0.225f),
        )
    }
}

@Composable
fun UiStateDisplay(uiState: UiState, lazyListState: LazyListState) {
    val scope = rememberCoroutineScope()
    val (text, color, isLoading) = when (uiState) {
        is UiState.Loading -> Triple(null, null, true)
        is UiState.Error -> Triple(uiState.errorMessage, MaterialTheme.colorScheme.error, false)
        is UiState.Success -> Triple(uiState.outputText, MaterialTheme.colorScheme.onSurface, false)
        else -> Triple(stringResource(R.string.results_placeholder), MaterialTheme.colorScheme.onSurface, false)
    }
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(DefaultPadding)) {
            CircularProgressIndicator(Modifier.size(180.dp).align(Alignment.Center))
        }
        val itemsCount = lazyListState.layoutInfo.totalItemsCount
        LaunchedEffect(Unit) { scope.launch { lazyListState.animateScrollToItem(itemsCount - 1) } }
    } else {
        text?.let { Text(it, Modifier.fillMaxWidth().padding(DefaultPadding), color!!) }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermission(
    permission: String,
    onPermissionGranted: @Composable () -> Unit
) {
    rememberPermissionState(permission).let {
        if (it.status.isGranted) onPermissionGranted()
        else LaunchedEffect(Unit) { it.launchPermissionRequest() }
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
    reviewManager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) reviewManager.launchReviewFlow(context as Activity, task.result)
    }
}

fun updateAppOpeningsCounter(context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    sharedPreferences
        .edit()
        .putInt("app_open_count", (sharedPreferences.getInt("app_open_count", 0)) + 1)
        .apply()
}
