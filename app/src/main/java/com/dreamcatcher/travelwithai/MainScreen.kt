package com.dreamcatcher.travelwithai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamcatcher.travelwithai.ui.theme.Blue500
import com.dreamcatcher.travelwithai.ui.theme.FirebrickRed
import com.dreamcatcher.travelwithai.ui.theme.UIConstants
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPadding
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingHalf
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultPaddingQuarter
import com.dreamcatcher.travelwithai.ui.theme.UIConstants.DefaultRoundedCornerValue
import com.dreamcatcher.travelwithai.ui.theme.VeryLightGrey
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = BakingViewModelFactory(context))
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val locationState by viewModel.location.collectAsState()
    var locationInputState by remember { mutableStateOf<String>("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scrollState = rememberScrollState()
    val toastTextNoLocationPermissions = "Please enable location permissions in app settings or provide the location manually."
    val toastTextNoCameraPermissions = "Camera permission is necessary for this feature."
    val fakeImageBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.london) // For screenshots
    val takePictureLauncher = rememberLauncherForActivityResult (
        ActivityResultContracts.TakePicturePreview()
    ) {
        it?.let {
            imageBitmap = it
            viewModel.sendPrompt(MessageType.PHOTO, null, it)
//            viewModel.sendPrompt(MessageType.PHOTO, null, fakeImageBitmap)
        }
    }

    updateAppOpeningsCounter(context)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ReviewDialog()
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            ScreenTitle()
            ImageCarousel(viewModel)

            RequestPermission(
                locationPermissionState,
                { viewModel.userAgreedLocation() },
                { viewModel.userDeniedLocation() },
            )
            Location(locationState, locationInputState.isNotEmpty())
            LocationInput(
                locationInputState = locationInputState,
                onLocationChange = { newLocation ->
                    locationInputState = newLocation
                }
            )

            ActionRow(listOf(R.string.action_start to {
                if (locationInputState.isNotEmpty()) {
                    viewModel.sendPrompt(MessageType.INITIAL, manualLocation = locationInputState)
                } else {
                    RequestPermission(
                        locationPermissionState,
                        { viewModel.sendPrompt(MessageType.INITIAL) },
                        { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                    )
                }
            }))
            ActionRow(listOf(
                R.string.history_of_this_place to {
                    if (locationInputState.isNotEmpty()) {
                        viewModel.sendPrompt(MessageType.HISTORY, manualLocation = locationInputState)
                    } else {
                        RequestPermission(
                            locationPermissionState,
                            { viewModel.sendPrompt(MessageType.HISTORY) },
                            { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                        )
                    }
                },
                R.string.restaurants_nearby to {
                    if (locationInputState.isNotEmpty()) {
                        viewModel.sendPrompt(MessageType.RESTAURANTS, manualLocation = locationInputState)
                    } else {
                        RequestPermission(
                            locationPermissionState,
                            { viewModel.sendPrompt(MessageType.RESTAURANTS) },
                            { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                        )
                    }
                },
            ))
            ActionRow(listOf(R.string.tourist_spots to {
                if (locationInputState.isNotEmpty()) {
                    viewModel.sendPrompt(MessageType.TOURIST_SPOTS, manualLocation = locationInputState)
                } else {
                    RequestPermission(
                        locationPermissionState,
                        { viewModel.sendPrompt(MessageType.TOURIST_SPOTS) },
                        { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                    )
                }
            }))
            ActionRow(
                listOf(R.string.safety_rules to {
                    if (locationInputState.isNotEmpty()) {
                        viewModel.sendPrompt(MessageType.SAFETY, manualLocation = locationInputState)
                    } else {
                        RequestPermission(
                            locationPermissionState,
                            { viewModel.sendPrompt(MessageType.SAFETY) },
                            { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                        )
                    }
                }),
                buttonColor = FirebrickRed
            )

            ImagePreview(imageBitmap)
            ActionRow(
                listOf(R.string.take_a_picture to {
                    var triggerAction by remember { mutableStateOf(false) }
                    if (locationInputState.isNotEmpty()) {
                        triggerAction = true
                    } else {
                        RequestPermission(
                            locationPermissionState,
                            { triggerAction = true },
                            { Toast.makeText(context, toastTextNoLocationPermissions, Toast.LENGTH_SHORT,).show() },
                        )
                    }
                    LaunchedEffect(cameraPermissionState.status, triggerAction) {
                        if (triggerAction) {
                            if (cameraPermissionState.status.isGranted) takePictureLauncher.launch(null)
                            else {
                                Toast.makeText(context, toastTextNoCameraPermissions, Toast.LENGTH_SHORT,).show()
                                cameraPermissionState.launchPermissionRequest()
                            }
                            triggerAction = false
                        }
                    }
                }),
                buttonColor = Blue500,
            )

            PromptInput(viewModel)
            UiStateDisplay(uiState, scrollState)
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
                shape = RoundedCornerShape(DefaultRoundedCornerValue),
                elevation = CardDefaults.cardElevation(10.dp),
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
fun Location(location: String, locationInputStateIsPresent: Boolean) {
    val textColour = if (locationInputStateIsPresent) Color.LightGray else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DefaultPadding)
            .padding(top = DefaultPadding)
    ) {
        Text(
            text = "Your Location:",
            style = MaterialTheme.typography.bodyLarge.copy(color = textColour),
            modifier = Modifier.padding(bottom = 5.dp)
        )
        Text(
            text = "$location",
            style = MaterialTheme.typography.bodyMedium.copy(color = textColour),
        )
    }
}

@Composable
fun LocationInput(
    locationInputState: String,
    onLocationChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = locationInputState,
            onValueChange = onLocationChange,
            placeholder = {
                Text(
                    text = "e.g. 'Rome, Italy' or 'London, Piccadilly'",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            },
            label = {
                Text(
                    text = "You can also provide another location",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultPadding)
                .clip(RoundedCornerShape(4.dp)),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
                disabledBorderColor = Color.Gray
            ),
            trailingIcon = {
                if (locationInputState.isNotEmpty()) {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Close Keyboard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun ActionButton(
    text: Int,
    onClick: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var triggerAction by remember { mutableStateOf(false) }
    Button(
        onClick = {
            triggerAction = true
            keyboardController?.hide()
        },
        modifier = modifier.fillMaxWidth().height(UIConstants.ButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        elevation = ButtonDefaults.elevatedButtonElevation()
    ) {
        Text(text = stringResource(text), textAlign = TextAlign.Center)
    }
    if (triggerAction) {
        onClick()
        triggerAction = false
    }
}

@Composable
fun ActionRow(
    buttons: List<Pair<Int, @Composable () -> Unit>>,
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
    val fakeImagePainterResource = painterResource(R.drawable.london) // For screenshots
    imageBitmap?.let { bitmap ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(DefaultPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
//                painter = fakeImagePainterResource,
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                disabledBorderColor = Color.Gray,
            )
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
fun UiStateDisplay(uiState: UiState, scrollState: ScrollState) {
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
        LaunchedEffect(Unit) { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
    } else {
        text?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DefaultPadding)
                    .background(VeryLightGrey, RoundedCornerShape(12.dp))
                    .padding(DefaultPadding)
            ) {
                Text(it, modifier = Modifier.fillMaxWidth(), color = color!!)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermission(
    permissionState: PermissionState,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    LaunchedEffect(permissionState.status) { when {
        permissionState.status.isGranted -> onPermissionGranted()
        !permissionState.status.isGranted -> {
            permissionState.launchPermissionRequest()
            onPermissionDenied()
        }
    }}
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
// 417