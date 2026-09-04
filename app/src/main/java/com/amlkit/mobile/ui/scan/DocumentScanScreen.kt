package com.amlkit.mobile.ui.scan

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.EmiratesIdScanResponse
import com.amlkit.mobile.data.dto.PassportScanResponse
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.File

/** Which document a [DocumentScanScreen] instance is scanning -- one screen
 * serves both types since camera/gallery capture, upload, and result
 * rendering are otherwise identical; only the API call and which fields
 * exist on the response differ. [routeArg] is also the nav route segment
 * (see Routes.documentScan). */
enum class DocType(val label: String, val routeArg: String) {
    PASSPORT("Passport", "passport"),
    EMIRATES_ID("Emirates ID", "emirates_id"),
    ;

    companion object {
        fun fromRouteArg(arg: String?): DocType = entries.firstOrNull { it.routeArg == arg } ?: PASSPORT
    }
}

/** Normalized view of either scan response -- [PassportScanResponse] and
 * [EmiratesIdScanResponse] carry a different field set (only passport has
 * `nationality`), but the screen renders both the same way. */
data class DocumentScanResult(
    val fullName: String?,
    val nationality: String?,
    val idNumber: String?,
    val birthDate: String?,
    val expiryDate: String?,
    val expiryFlags: List<String>,
    val qualityFlags: List<String>,
    val authenticityFlags: List<String>,
)

private fun JsonElement?.asFlags(): List<String> =
    ((this as? JsonObject)?.get("flags") as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?: emptyList()

private fun JsonElement?.boolField(key: String): Boolean? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.booleanOrNull

private fun PassportScanResponse.toScanResult() = DocumentScanResult(
    fullName = full_name,
    nationality = nationality,
    idNumber = id_number,
    birthDate = birth_date,
    expiryDate = expiry_date,
    expiryFlags = expiry_check.asFlags(),
    qualityFlags = image_quality.asFlags(),
    // authenticity is the MRZ checksum signal -- surfacing it here is the
    // whole point of Tier 1 IDV; a checksum failure otherwise scans clean.
    authenticityFlags = authenticity.asFlags(),
)

private fun EmiratesIdScanResponse.toScanResult() = DocumentScanResult(
    fullName = full_name,
    nationality = null,
    idNumber = id_number,
    birthDate = birth_date,
    expiryDate = expiry_date,
    expiryFlags = expiry_check.asFlags(),
    qualityFlags = image_quality.asFlags(),
    // id_validation.flags always includes an informational "check digit not
    // verified" disclaimer, unlike passport's authenticity.flags which is
    // only non-empty on an actual checksum failure -- only surface it here
    // when the ID number genuinely failed format validation, or a clean
    // scan would show a warning banner for no real problem.
    authenticityFlags = if (id_validation.boolField("valid_format") == false) id_validation.asFlags() else emptyList(),
)

data class DocumentScanUiState(
    val imageFile: File? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val result: DocumentScanResult? = null,
)

class DocumentScanViewModel(
    private val repository: AmlkitRepository,
    private val docType: DocType,
) : ViewModel() {
    private val _state = MutableStateFlow(DocumentScanUiState())
    val state: StateFlow<DocumentScanUiState> = _state

    fun onImageSelected(file: File) {
        val previous = _state.value.imageFile
        if (previous != null && previous != file) previous.delete()
        _state.value = _state.value.copy(imageFile = file, result = null, error = null)
    }

    fun onCameraPermissionDenied() {
        _state.value = _state.value.copy(error = "Camera permission was denied. Choose from gallery instead, or allow camera access in system settings.")
    }

    fun scan() {
        val file = _state.value.imageFile ?: return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val outcome: ApiResult<DocumentScanResult> = when (docType) {
                DocType.PASSPORT -> when (val r = repository.scanPassport(file)) {
                    is ApiResult.Success -> ApiResult.Success(r.data.toScanResult())
                    is ApiResult.Failure -> r
                }
                DocType.EMIRATES_ID -> when (val r = repository.scanEmiratesId(file)) {
                    is ApiResult.Success -> ApiResult.Success(r.data.toScanResult())
                    is ApiResult.Failure -> r
                }
            }
            when (outcome) {
                is ApiResult.Success -> _state.value = _state.value.copy(loading = false, result = outcome.data)
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = outcome.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.imageFile?.delete()
    }
}

private fun scanCacheDir(context: Context): File = File(context.cacheDir, "scans").apply { mkdirs() }

/** Creates the destination file+URI [ActivityResultContracts.TakePicture]
 * needs before it's launched -- that contract writes JPEG bytes straight
 * into the URI it's given rather than returning them. */
private fun createCameraTarget(context: Context, docType: DocType): Pair<File, Uri> {
    val file = File(scanCacheDir(context), "scan_${docType.routeArg}_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return file to uri
}

/** The system photo picker hands back a `content://` URI outside this app's
 * control; copy it into a local cache file so it can be read back for the
 * multipart upload the same way a camera capture is. */
private fun copyToScanCacheFile(context: Context, source: Uri, docType: DocType): File? = try {
    val file = File(scanCacheDir(context), "scan_${docType.routeArg}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    if (file.exists() && file.length() > 0) file else null
} catch (e: Exception) {
    null
}

@Composable
fun DocumentScanScreen(
    repository: AmlkitRepository,
    docType: DocType,
    onUseData: (DocumentScanResult) -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { DocumentScanViewModel(it, docType) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The path (not the File itself) survives process death while the
    // external camera app is in the foreground -- a well-documented risk
    // with TakePicture -- so the callback can still recover the target file
    // if this app's process was killed and restarted in between.
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingCameraPath
        if (success && path != null) viewModel.onImageSelected(File(path))
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (file, uri) = createCameraTarget(context, docType)
            pendingCameraPath = file.absolutePath
            cameraLauncher.launch(uri)
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { copyToScanCacheFile(context, uri, docType) }
                if (file != null) viewModel.onImageSelected(file)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(text = "Scan ${docType.label}") }

        state.error?.let { item { ErrorBanner(message = it) } }

        state.imageFile?.let { file ->
            item {
                AsyncImage(
                    model = file,
                    contentDescription = "${docType.label} preview",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    text = "Take photo",
                    tone = PillButtonTone.SECONDARY,
                    onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Choose from gallery",
                    tone = PillButtonTone.SECONDARY,
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.imageFile != null && state.result == null) {
            item {
                PillButton(
                    text = "Scan document",
                    onClick = viewModel::scan,
                    enabled = !state.loading,
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.result?.let { result ->
            item { ScanResultCard(result) }
            item {
                PillButton(
                    text = "Use this data",
                    onClick = { onUseData(result) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ScanResultCard(result: DocumentScanResult) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        result.fullName?.let { LabeledValue("Name", it) }
        result.nationality?.let { LabeledValue("Nationality", it) }
        result.idNumber?.let { LabeledValue("ID number", it) }
        result.birthDate?.let { LabeledValue("Date of birth", it) }
        result.expiryDate?.let { LabeledValue("Expiry date", it) }
        (result.authenticityFlags + result.expiryFlags + result.qualityFlags).forEach { flag -> ErrorBanner(message = flag) }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = AmlInk2)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
