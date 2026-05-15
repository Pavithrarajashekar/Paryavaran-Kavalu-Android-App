package com.example.paryavarankavalu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import coil.compose.rememberAsyncImagePainter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

// THEME
@Composable
fun ParyavaranKavaluTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF66BB6A),
            tertiary = Color(0xFF1B5E20),
            background = Color(0xFFF9FBF9),
            surface = Color(0xFFFFFFFF)
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParyavaranKavaluTheme {
                ParyavaranApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParyavaranApp() {
    var activeTab by remember { mutableStateOf("Home") }
    var userXP by remember { mutableIntStateOf(1450) }
    val reports = remember {
        mutableStateListOf(
            WasteReport("1", "Plastic", "Piles of plastic bottles near the JSSATE entrance.", "Pending", 12.9030, 77.5035, reporterName = "Rahul S."),
            WasteReport("2", "Organic", "Food waste dumping site near cafeteria.", "Cleaned", 12.9015, 77.5025, reporterName = "Priya K.")
        )
    }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PARYAVARAN KAVALU",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20),
                        letterSpacing = 2.sp,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf("Home", "Map", "Report")
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Home" -> Icons.Default.Home
                                    "Map" -> Icons.Default.Place
                                    else -> Icons.Default.AddCircle
                                },
                                contentDescription = tab
                            )
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (activeTab) {
                "Home" -> HomeScreen(reports, userXP)
                "Map" -> MapScreen(reports) { reportToUpdate ->
                    val index = reports.indexOfFirst { it.id == reportToUpdate.id }
                    if (index != -1) {
                        reports[index] = reports[index].copy(status = "Cleaned")
                        userXP += 50
                        Toast.makeText(context, "Hotspot Cleaned! +50 XP", Toast.LENGTH_SHORT).show()
                    }
                }
                "Report" -> ReportScreen { newReport ->
                    reports.add(0, newReport)
                    userXP += 100
                    activeTab = "Home"
                    Toast.makeText(context, "Hotspot Reported! +100 XP", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun MapScreen(reports: List<WasteReport>, onStatusUpdate: (WasteReport) -> Unit) {
    val context = LocalContext.current
    val defaultLocation = LatLng(12.9022, 77.5029)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    var selectedReport by remember { mutableStateOf<WasteReport?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission)
        ) {
            reports.forEach { report ->
                Marker(
                    state = MarkerState(position = LatLng(report.lat, report.lng)),
                    title = "${report.type} Waste",
                    snippet = report.description,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (report.status == "Cleaned") BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                    ),
                    onClick = {
                        selectedReport = report
                        true
                    }
                )
            }
        }

        if (selectedReport != null) {
            ReportDetailScreen(
                report = selectedReport!!,
                onClose = { selectedReport = null },
                onMarkCleaned = {
                    onStatusUpdate(selectedReport!!)
                    selectedReport = null
                }
            )
        }
    }
}

@Composable
fun ReportDetailScreen(
    report: WasteReport,
    onClose: () -> Unit,
    onMarkCleaned: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                    Text("Report Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image Placeholder or real image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE8F5E9))
                    ) {
                        if (report.imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(report.imageUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).align(Alignment.Center),
                                tint = Color(0xFF2E7D32).copy(alpha = 0.3f)
                            )
                        }
                    }

                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFE1F5FE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    report.type.uppercase(Locale.getDefault()),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(report.timestamp)),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            report.description,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )

                        Spacer(Modifier.height(24.dp))

                        // Location
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                color = Color(0xFFE0F2F1),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF009688), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Location", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${String.format(Locale.getDefault(), "%.4f", report.lat)}° ${if (report.lat >= 0) "N" else "S"}, ${String.format(Locale.getDefault(), "%.4f", report.lng)}° ${if (report.lng >= 0) "E" else "W"}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Map, null, tint = Color(0xFF009688), modifier = Modifier.size(20.dp))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Reporter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Reported By", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(report.reporterName, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Action Button
                if (report.status != "Cleaned") {
                    Button(
                        onClick = onMarkCleaned,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Cleaned", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Already Cleaned", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(reports: List<WasteReport>, userXP: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF9FBF9)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Guardian Score", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                            Text("$userXP XP", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFF2E7D32))
                        }
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "WARRIOR LEVEL",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { (userXP % 1000) / 1000f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF2E7D32),
                        trackColor = Color(0xFFE8F5E9)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${1000 - (userXP % 1000)} points until next rank",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI INSIGHTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("SWITCH TO REUSABLES", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "Reduce plastic bottle accumulation at building entrances by carrying refillable containers and advocating for more water fountains.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("LAUNCH NEIGHBORHOOD COMPOSTING", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "Turn food waste dumping sites into community composting hubs to transform organic scraps into nutrient-rich soil for local parks.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VIEW DEEP ANALYSIS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatsCard(label = "Reports Resolved", value = "1", modifier = Modifier.weight(1f))
                StatsCard(label = "Active Hotspots", value = "2", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("LOCAL ACTIVITY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            }
        }

        items(reports) { report ->
            ActivityItem(report)
        }
    }
}

@Composable
fun StatsCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, lineHeight = 12.sp)
        }
    }
}

@Composable
fun ActivityItem(report: WasteReport) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (report.type) {
                    "Plastic" -> Icons.Default.Inventory
                    "Organic" -> Icons.Default.Eco
                    else -> Icons.Default.Delete
                },
                null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(report.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Reported by ${report.reporterName}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ReportScreen(onSubmit: (WasteReport) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var latLng by remember { mutableStateOf(0.0 to 0.0) }
    var category by remember { mutableStateOf("Pending AI Analysis") }
    var note by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun updateLocationOnce() {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    location?.let {
                        latLng = it.latitude to it.longitude
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isAnalyzing = true
            updateLocationOnce()
            scope.launch {
                category = classifyWasteWithAI(bitmap) ?: "Mixed"
                isAnalyzing = false
                try {
                    val file = java.io.File(context.cacheDir, "captured_waste_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    capturedImageUri = Uri.fromFile(file)
                } catch (_: Exception) {
                    // Ignore error during file save
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            capturedImageUri = it
            try {
                val exifLocation = getLocationFromUri(context, it)
                if (exifLocation != null) {
                    latLng = exifLocation
                } else {
                    updateLocationOnce()
                }

                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                capturedImage = bitmap
                isAnalyzing = true
                scope.launch {
                    category = classifyWasteWithAI(bitmap) ?: "Mixed"
                    isAnalyzing = false
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Load failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.CAMERA] == true) {
            cameraLauncher.launch()
        }
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            updateLocationOnce()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            updateLocationOnce()
        }
    }

    // Continuous location tracking for "real time" feel
    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    latLng = it.latitude to it.longitude
                }
            }
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Report Waste Hotspot", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))

        Box(
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF1F8E9))
                .clickable { permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)) },
            contentAlignment = Alignment.Center
        ) {
            if (capturedImage != null) {
                Image(capturedImage!!.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(48.dp), tint = Color(0xFF2E7D32))
                    Text("Capture or Scan", fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Upload from Gallery")
        }

        Text("GPS COORDINATES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White)
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Current Location Captured", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (latLng.first != 0.0) {
                            "${String.format(Locale.getDefault(), "%.4f", latLng.first.absoluteValue)}° ${if (latLng.first >= 0) "N" else "S"}, " +
                                    "${String.format(Locale.getDefault(), "%.4f", latLng.second.absoluteValue)}° ${if (latLng.second >= 0) "E" else "W"}"
                        } else "Fetching...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = { updateLocationOnce() }) {
                    Icon(Icons.Default.Refresh, null, tint = Color(0xFF2E7D32))
                }
            }
        }

        Text("AI CLASSIFICATION", fontSize = 10.sp, color = Color.Gray)
        Text("Select or Scan Category", fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Plastic", "Organic", "E-Waste", "Mixed").forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E7D32),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isAnalyzing) CircularProgressIndicator(Modifier.size(20.dp))
                else Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF2E7D32))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("AI Analysis", fontSize = 10.sp, color = Color.Gray)
                    Text(category, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSubmit(
                    WasteReport(
                        id = System.currentTimeMillis().toString(),
                        type = category,
                        description = note,
                        status = "Pending",
                        lat = latLng.first,
                        lng = latLng.second,
                        imageUri = capturedImageUri
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = capturedImage != null && !isAnalyzing
        ) {
            Text("SUBMIT REPORT")
        }
    }
}

suspend fun classifyWasteWithAI(bitmap: Bitmap): String? {
    return try {
        val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = "APIKEY")
        val response = generativeModel.generateContent(content { image(bitmap); text("Is this Plastic, Organic, or E-Waste? Answer in 1 word.") })
        when (val result = response.text?.trim()?.uppercase(Locale.getDefault())) {
            "PLASTIC", "ORGANIC", "E-WASTE" -> result.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
            else -> "Mixed"
        }
    } catch (_: Exception) { null }
}

fun getLocationFromUri(context: Context, uri: Uri): Pair<Double, Double>? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            exif.latLong?.let {
                it[0] to it[1]
            }
        }
    } catch (_: Exception) { null }
}
