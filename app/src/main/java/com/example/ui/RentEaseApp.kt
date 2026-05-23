package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.CalculationsEngine
import com.example.data.MonthlyBill
import com.example.data.Tenant
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RentEaseApp(viewModel: RentViewModel) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val tenantsState by viewModel.tenants.collectAsState()
    val billsState by viewModel.bills.collectAsState()
    val totalCollected by viewModel.totalRentCollected.collectAsState()
    val pendingCollection by viewModel.pendingPayments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HomeWork,
                            contentDescription = "RentEase Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RentEase",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.initSettingsForm(settingsState)
                            viewModel.navigateTo("settings")
                        },
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (viewModel.currentScreen in listOf("dashboard", "tenants", "bills")) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "dashboard",
                        onClick = { viewModel.navigateTo("dashboard") },
                        label = { Text("Home") },
                        icon = {
                            Icon(
                                imageVector = if (viewModel.currentScreen == "dashboard") Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                             )
                        },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "tenants",
                        onClick = { viewModel.navigateTo("tenants") },
                        label = { Text("Tenants") },
                        icon = {
                            Icon(
                                imageVector = if (viewModel.currentScreen == "tenants") Icons.Default.People else Icons.Outlined.People,
                                contentDescription = "Tenants"
                            )
                        },
                        modifier = Modifier.testTag("nav_tenants")
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "bills",
                        onClick = { viewModel.navigateTo("bills") },
                        label = { Text("History") },
                        icon = {
                            Icon(
                                imageVector = if (viewModel.currentScreen == "bills") Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "History"
                            )
                        },
                        modifier = Modifier.testTag("nav_bills")
                    )
                }
            }
        },
        floatingActionButton = {
            when (viewModel.currentScreen) {
                "tenants" -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.initTenantForm(null)
                            viewModel.navigateTo("add_tenant")
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                        text = { Text("Add Tenant") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("add_tenant_fab")
                    )
                }
                "bills" -> {
                    if (tenantsState.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                viewModel.initBillForm(null)
                                viewModel.navigateTo("add_bill")
                            },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculate") },
                            text = { Text("New Bill") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("add_bill_fab")
                        )
                    }
                }
                "dashboard" -> {
                    if (tenantsState.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.initBillForm(null)
                                viewModel.navigateTo("add_bill")
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("dashboard_add_bill_fab")
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = "Quick Calculate Bill")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = viewModel.currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { screen ->
            when (screen) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    tenants = tenantsState,
                    bills = billsState,
                    totalCollected = totalCollected,
                    pendingCollection = pendingCollection,
                    settings = settingsState
                )
                "tenants" -> TenantsScreen(
                    viewModel = viewModel,
                    tenants = tenantsState,
                    settings = settingsState
                )
                "add_tenant" -> AddEditTenantScreen(viewModel = viewModel)
                "bills" -> BillsHistoryScreen(
                    viewModel = viewModel,
                    bills = billsState,
                    tenants = tenantsState,
                    settings = settingsState
                )
                "add_bill" -> AddEditBillScreen(
                    viewModel = viewModel,
                    tenants = tenantsState,
                    settings = settingsState
                )
                "bill_details" -> BillDetailsScreen(
                    viewModel = viewModel,
                    tenants = tenantsState,
                    settings = settingsState
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    currentSettings = settingsState
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 1. DASHBOARD SCREEN
// ---------------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: RentViewModel,
    tenants: List<Tenant>,
    bills: List<MonthlyBill>,
    totalCollected: Double,
    pendingCollection: Double,
    settings: AppSettings
) {
    val df = remember { DecimalFormat("#,##,##0.00") }
    val currency = settings.currencySymbol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        // Welcome and Landlord visual header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Namaste, ${settings.ownerName.ifBlank { "Owner" }}! 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage and split unified bills with submeter precision.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Financial KPIs Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rent Collected Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Collected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Collected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$currency${df.format(totalCollected)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }

            // Pending Payments Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$currency${df.format(pendingCollection)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Analytics Canvas Graph (Monthly Electricity Submeter trends)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Consumption",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Electricity Usage Trends (Units)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                val filterBills = bills.take(5).reversed()
                if (filterBills.size >= 1) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    val gridColor = MaterialTheme.colorScheme.outlineVariant

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val paddingLeft = 35.dp.toPx()
                        val paddingBottom = 25.dp.toPx()
                        val paddingTop = 10.dp.toPx()
                        val graphWidth = canvasWidth - paddingLeft
                        val graphHeight = canvasHeight - paddingBottom - paddingTop

                        // Find maximum units for scale
                        var maxUnits = 100.0
                        filterBills.forEach { b ->
                            val mu = b.mainMeterCurr - b.mainMeterPrev
                            if (mu > maxUnits) maxUnits = mu
                        }
                        // increment maxUnits to give ceiling breathing room
                        maxUnits *= 1.2

                        // Draw Grid lines
                        val steps = 3
                        for (i in 0..steps) {
                            val y = paddingTop + graphHeight * (1 - i.toFloat() / steps)
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            // Draw units labels
                            val valLabel = (maxUnits * i / steps).toInt().toString()
                            drawContext.canvas.nativeCanvas.drawText(
                                valLabel,
                                5.dp.toPx(),
                                y + 4.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = labelColor
                                    textSize = 10.sp.toPx()
                                }
                            )
                        }

                        // Plot Bars / Segments
                        val totalPoints = filterBills.size
                        val barSpacing = graphWidth / totalPoints
                        filterBills.forEachIndexed { idx, b ->
                            val mainUnitsCount = max(0.0, b.mainMeterCurr - b.mainMeterPrev)
                            val tenantUnitsCount = max(0.0, b.tenantSubmeterCurr - b.tenantSubmeterPrev)

                            val xCenter = paddingLeft + idx * barSpacing + barSpacing / 2

                            val mainBarHeight = (mainUnitsCount / maxUnits * graphHeight).toFloat()
                            val tenantBarHeight = (tenantUnitsCount / maxUnits * graphHeight).toFloat()

                            // Draw Main Meter units (Background bar)
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.25f),
                                topLeft = Offset(xCenter - 14.dp.toPx(), paddingTop + graphHeight - mainBarHeight),
                                size = androidx.compose.ui.geometry.Size(28.dp.toPx(), mainBarHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Draw Tenant units (Accent bar inside)
                            drawRoundRect(
                                color = secondaryColor,
                                topLeft = Offset(xCenter - 10.dp.toPx(), paddingTop + graphHeight - tenantBarHeight),
                                size = androidx.compose.ui.geometry.Size(20.dp.toPx(), tenantBarHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Draw Month label at bottom
                            val formattedMonth = b.billMonth.substringAfter("-") + "/" + b.billMonth.substring(2,4)
                            drawContext.canvas.nativeCanvas.drawText(
                                formattedMonth,
                                xCenter - 14.dp.toPx(),
                                canvasHeight - 5.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = labelColor
                                    textSize = 9.sp.toPx()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(primaryColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Main Meter Units", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(18.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(secondaryColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tenant Units", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Calculate bills to see electricity trends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions & Quick Launch Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Bills & Calculations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { viewModel.navigateTo("bills") }) {
                Text("See All")
            }
        }

        if (bills.isEmpty()) {
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Empty Bills",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No bills calculated yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add a tenant and calculate your first rent + submeter invoice.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (tenants.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.initBillForm(null)
                                viewModel.navigateTo("add_bill")
                            },
                            modifier = Modifier.testTag("empty_state_add_bill_btn")
                        ) {
                            Text("Calculate Now")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.initTenantForm(null)
                                viewModel.navigateTo("add_tenant")
                            },
                            modifier = Modifier.testTag("empty_state_add_tenant_btn")
                        ) {
                            Text("Add Tenant First")
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bills.take(3).forEach { bill ->
                    val tenantName = tenants.find { it.id == bill.tenantId }?.name ?: "Unknown Tenant"
                    val roomName = tenants.find { it.id == bill.tenantId }?.roomName ?: "Unit"
                    val isPaid = bill.paymentStatus == "PAID"

                    Card(
                        onClick = { viewModel.navigateTo("bill_details", bill.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPaid) Icons.Default.CurrencyRupee else Icons.Default.HourglassEmpty,
                                        contentDescription = "Status",
                                        tint = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tenantName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$roomName • ${bill.billMonth}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${df.format(bill.calculatedTotalAmount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bill.paymentStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 2. TENANTS MANAGEMENT SCREEN
// ---------------------------------------------------------------------------------
@Composable
fun TenantsScreen(
    viewModel: RentViewModel,
    tenants: List<Tenant>,
    settings: AppSettings
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Text(
            text = "Your Tenants Directory",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add, edit, or archive occupants and setup basic rent parameters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (tenants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "No Tenants",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tenant directory is empty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Add Tenant' in the bottom-right corner to begin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tenants) { tenant ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tenant.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tenant.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "🚪 ${tenant.roomName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.initTenantForm(tenant)
                                            viewModel.navigateTo("add_tenant")
                                        },
                                        modifier = Modifier.testTag("edit_tenant_${tenant.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteTenant(tenant)
                                            Toast.makeText(context, "${tenant.name} removed.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("delete_tenant_${tenant.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Archive",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Rent Amount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${settings.currencySymbol}${tenant.rentAmount}/mo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        "Deposit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${settings.currencySymbol}${tenant.depositAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        "Occupants",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${tenant.numberOfPersons} ${if (tenant.numberOfPersons == 1) "Person" else "Persons"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (tenant.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "🗒️ Note: ${tenant.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.initBillForm(null)
                                    viewModel.onTenantSelectedForBill(tenant.id)
                                    viewModel.navigateTo("add_bill")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_bill_tenant_${tenant.id}"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Utilities Split Bill")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 3. ADD / EDIT TENANT SCREEN
// ---------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTenantScreen(viewModel: RentViewModel) {
    val isEdit = viewModel.tenantFormId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { viewModel.navigateTo("tenants") },
                modifier = Modifier.testTag("cancel_add_tenant")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEdit) "Edit Tenant Details" else "Add New Tenant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.tenantFormName,
            onValueChange = { viewModel.tenantFormName = it },
            label = { Text("Full Name") },
            placeholder = { Text("Enter tenant first and last name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("tenant_name_input")
        )

        OutlinedTextField(
            value = viewModel.tenantFormPhone,
            onValueChange = { viewModel.tenantFormPhone = it },
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. +91 98765 43210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("tenant_phone_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.tenantFormRoom,
                onValueChange = { viewModel.tenantFormRoom = it },
                label = { Text("Room / Unit") },
                placeholder = { Text("e.g. Unit 2B") },
                leadingIcon = { Icon(Icons.Default.Room, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("tenant_room_input")
            )

            OutlinedTextField(
                value = viewModel.tenantFormPersons,
                onValueChange = { viewModel.tenantFormPersons = it },
                label = { Text("Occupants count") },
                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("tenant_persons_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.tenantFormRent,
                onValueChange = { viewModel.tenantFormRent = it },
                label = { Text("Rent Amount (monthly)") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("tenant_rent_input")
            )

            OutlinedTextField(
                value = viewModel.tenantFormDeposit,
                onValueChange = { viewModel.tenantFormDeposit = it },
                label = { Text("Security Deposit") },
                leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("tenant_deposit_input")
            )
        }

        OutlinedTextField(
            value = viewModel.tenantFormStartDate,
            onValueChange = { viewModel.tenantFormStartDate = it },
            label = { Text("Occupancy Start Date") },
            placeholder = { Text("dd/mm/yyyy") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("tenant_start_date_input")
        )

        OutlinedTextField(
            value = viewModel.tenantFormNotes,
            onValueChange = { viewModel.tenantFormNotes = it },
            label = { Text("Landlord Notes (Optional)") },
            placeholder = { Text("Enter agreement highlights or notes...") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("tenant_notes_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveTenant() },
            enabled = viewModel.tenantFormName.isNotBlank() && viewModel.tenantFormRent.toDoubleOrNull() != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_tenant_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEdit) "Save Edits" else "Confirm Active Tenant")
        }
    }
}

// ---------------------------------------------------------------------------------
// 4. BILLS & HISTORY SCREEN
// ---------------------------------------------------------------------------------
@Composable
fun BillsHistoryScreen(
    viewModel: RentViewModel,
    bills: List<MonthlyBill>,
    tenants: List<Tenant>,
    settings: AppSettings
) {
    val df = remember { DecimalFormat("#,##,##0.00") }
    var filterPendingOnly by remember { mutableStateOf(false) }

    val filteredBills = remember(bills, filterPendingOnly) {
        if (filterPendingOnly) {
            bills.filter { it.paymentStatus == "PENDING" }
        } else {
            bills
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History Ledger",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pending Only", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = filterPendingOnly,
                    onCheckedChange = { filterPendingOnly = it },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("filter_pending_switch")
                )
            }
        }
        
        Text(
            text = "Track historical utility computations and payment updates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History screen is empty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (filterPendingOnly) "All calculated rent splits are paid! 🎉" else "Tap 'New Bill' to calculate rent share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBills) { bill ->
                    val tenant = tenants.find { it.id == bill.tenantId }
                    val isPaid = bill.paymentStatus == "PAID"

                    Card(
                        onClick = { viewModel.navigateTo("bill_details", bill.id) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tenant?.name ?: "Unknown Tenant",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "🚪 ${tenant?.roomName ?: "Unit"} • 📅 ${bill.billMonth}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                SuggestionChip(
                                    onClick = {
                                        val nextStatus = if (isPaid) "PENDING" else "PAID"
                                        viewModel.updatePaymentStatus(bill, nextStatus, 0.0)
                                    },
                                    label = { Text(bill.paymentStatus) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        labelColor = if (isPaid) MaterialTheme.colorScheme.primary else if (bill.paymentStatus == "PARTIAL") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                        containerColor = if (isPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else if (bill.paymentStatus == "PARTIAL") MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ElectricBolt,
                                        contentDescription = "Power",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val consumed = max(0.0, bill.tenantSubmeterCurr - bill.tenantSubmeterPrev)
                                    Text(
                                        "${consumed.toInt()} Units (${settings.currencySymbol}${bill.calculatedTenantElectricity.toInt()})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (bill.waterBillAmount > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = "Water",
                                            tint = Color(0xFF0288D1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${settings.currencySymbol}${bill.calculatedTenantWater.toInt()}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Text(
                                    text = "${settings.currencySymbol}${df.format(bill.calculatedTotalAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 5. ADD / EDIT MONTHLY SPLIT BILL SCREEN (THE CALCULATION ENGINE FORM)
// ---------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillScreen(
    viewModel: RentViewModel,
    tenants: List<Tenant>,
    settings: AppSettings
) {
    val liveElecBreakdown by viewModel.liveElectricityBreakdown.collectAsState(
        CalculationsEngine.ElectricityBreakdown(0.0,0.0,0.0,0.0,0.0,0.0,0.0)
    )
    val liveWaterBreakdown by viewModel.liveWaterBreakdown.collectAsState(
        CalculationsEngine.WaterBreakdown("EQUAL",0.0,0,0,0.0)
    )

    val selectedTenant = tenants.find { it.id == viewModel.billFormTenantId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { viewModel.navigateTo("bills") },
                modifier = Modifier.testTag("cancel_add_bill")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (viewModel.billFormId != null) "Edit Split Bill Ledger" else "New Utilities Split calculation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Select Tenant
        Text(
            "1. Tenant & Month Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tenant Select Card
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Select Tenant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .testTag("bill_tenant_dropdown_trigger"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTenant?.name ?: "Tap to choose",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        tenants.forEach { tenant ->
                            DropdownMenuItem(
                                text = { Text("${tenant.name} (${tenant.roomName})") },
                                onClick = {
                                    expanded = false
                                    viewModel.onTenantSelectedForBill(tenant.id)
                                },
                                modifier = Modifier.testTag("bill_tenant_opt_${tenant.id}")
                            )
                        }
                    }
                }
            }

            // Month Select Box (yyyy-MM)
            OutlinedTextField(
                value = viewModel.billFormMonth,
                onValueChange = { viewModel.billFormMonth = it },
                label = { Text("Bill Month") },
                placeholder = { Text("yyyy-MM") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bill_month_input")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Main Electric Connection & Tenant Submeter Reading Data
        Text(
            "2. Electric Meter Readings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Enter current readings. Previous readings will auto-fill from last month's data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Meter Container
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "🔌 MAIN METER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = viewModel.billFormMainPrev,
                    onValueChange = { viewModel.billFormMainPrev = it },
                    label = { Text("Previous") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_meter_prev")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = viewModel.billFormMainCurr,
                    onValueChange = { viewModel.billFormMainCurr = it },
                    label = { Text("Current") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_meter_curr")
                )
            }

            // Tenant Submeter Container
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "🔌 TENANT SUBMETER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.billFormTenantPrev,
                    onValueChange = { viewModel.billFormTenantPrev = it },
                    label = { Text("Previous") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tenant_submeter_prev")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = viewModel.billFormTenantCurr,
                    onValueChange = { viewModel.billFormTenantCurr = it },
                    label = { Text("Current") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tenant_submeter_curr")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Electricity Board Bill Details (Invoiced values)
        Text(
            "3. Electricity Board Main Invoice Items",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.billFormBoardBill,
                onValueChange = { viewModel.billFormBoardBill = it },
                label = { Text("Total Bill Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("board_bill_amount")
            )

            OutlinedTextField(
                value = viewModel.billFormFixedCharges,
                onValueChange = { viewModel.billFormFixedCharges = it },
                label = { Text("Fixed Charges") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("board_fixed_charges")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.billFormTaxAmount,
                onValueChange = { viewModel.billFormTaxAmount = it },
                label = { Text("Tax @ 9% on Bill") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("board_tax_amount")
            )

            OutlinedTextField(
                value = viewModel.billFormOtherAdjustments,
                onValueChange = { viewModel.billFormOtherAdjustments = it },
                label = { Text("Other Surcharges") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("board_surcharges")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Split Modes
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Calculation Division Modes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        selected = viewModel.billFormCalcMode == "PROPORTIONAL",
                        onClick = { viewModel.billFormCalcMode = "PROPORTIONAL" },
                        text = "Proportional Split"
                    )
                    ChoiceChip(
                        selected = viewModel.billFormCalcMode == "CUSTOM",
                        onClick = { viewModel.billFormCalcMode = "CUSTOM" },
                        text = "Static Rate (Custom)"
                    )
                }

                if (viewModel.billFormCalcMode == "CUSTOM") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.billFormCustomElectricityRate,
                        onValueChange = { viewModel.billFormCustomElectricityRate = it },
                        label = { Text("Fixed Rate per Unit (e.g., 8.00)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_elec_rate")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Custom options triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Include tax in division", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Add taxes proportionately into the final share.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.billFormIncludeTax,
                        onCheckedChange = { viewModel.billFormIncludeTax = it },
                        modifier = Modifier.scale(0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Exclude fixed charges", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Deduct landlord's base fixed charges from split.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.billFormExcludeFixed,
                        onCheckedChange = { viewModel.billFormExcludeFixed = it },
                        modifier = Modifier.scale(0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.billFormCalcMode == "PROPORTIONAL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Divide only basic energy charges", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Exclude surcharges, adjustments and fixed charges.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = viewModel.billFormDivideOnlyEnergy,
                            onCheckedChange = { viewModel.billFormDivideOnlyEnergy = it },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = viewModel.billFormManualOverrideElec,
                    onValueChange = { viewModel.billFormManualOverrideElec = it },
                    label = { Text("Landlord Manual override amount (0 to disable)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("elec_manual_override")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 4: Water Bill division Details
        Text(
            "4. Water Bill details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.billFormWaterAmount,
                onValueChange = { viewModel.billFormWaterAmount = it },
                label = { Text("Total Water Bill Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("water_bill_amount_input")
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .padding(top = 8.dp)
            ) {
                var wExpanded by remember { mutableStateOf(false) }
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { wExpanded = true }
                        .testTag("water_rule_trigger")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Division Rule", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when (viewModel.billFormWaterSplitRule) {
                                    "EQUAL" -> "Equal split"
                                    "PER_PERSON" -> "By headcount"
                                    else -> "Manual split"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }

                DropdownMenu(expanded = wExpanded, onDismissRequest = { wExpanded = false }) {
                    DropdownMenuItem(text = { Text("Equal split among unit slots") }, onClick = { viewModel.billFormWaterSplitRule = "EQUAL"; wExpanded = false })
                    DropdownMenuItem(text = { Text("By occupants headcount") }, onClick = { viewModel.billFormWaterSplitRule = "PER_PERSON"; wExpanded = false })
                    DropdownMenuItem(text = { Text("Custom fixed amount") }, onClick = { viewModel.billFormWaterSplitRule = "MANUAL"; wExpanded = false })
                }
            }
        }

        if (viewModel.billFormWaterSplitRule == "MANUAL") {
            OutlinedTextField(
                value = viewModel.billFormTenantWaterCustom,
                onValueChange = { viewModel.billFormTenantWaterCustom = it },
                label = { Text("Tenant Water Share Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("tenant_water_custom_amt")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 5: Rent & Maintenance Adjustments
        Text(
            "5. Rent, Repair and Maintenance Charges",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.billFormRent,
                onValueChange = { viewModel.billFormRent = it },
                label = { Text("Rent amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bill_rent_input_override")
            )

            OutlinedTextField(
                value = viewModel.billFormMaintenance,
                onValueChange = { viewModel.billFormMaintenance = it },
                label = { Text("Maintenance cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bill_maintenance_input")
            )

            OutlinedTextField(
                value = viewModel.billFormOtherCharges,
                onValueChange = { viewModel.billFormOtherCharges = it },
                label = { Text("Other fees") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bill_other_charges_input")
            )
        }

        OutlinedTextField(
            value = viewModel.billFormNotes,
            onValueChange = { viewModel.billFormNotes = it },
            label = { Text("Discrepancies / Notes for invoice print") },
            placeholder = { Text("e.g., Deduction for plumbing repairs...") },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        val finalTotal = (viewModel.billFormRent.toDoubleOrNull() ?: 0.0) +
                liveElecBreakdown.finalElectricityAmount +
                liveWaterBreakdown.finalWaterAmount +
                (viewModel.billFormMaintenance.toDoubleOrNull() ?: 0.0) +
                (viewModel.billFormOtherCharges.toDoubleOrNull() ?: 0.0)

        // LIVE CALCULATION TRANSPARENCIES ENGINE BREAKDOWN
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Live Splitting Invoice Calculation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected Tenant name:", style = MaterialTheme.typography.bodySmall)
                    Text(selectedTenant?.name ?: "—", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Board Main Consumed:", style = MaterialTheme.typography.bodySmall)
                    Text("${liveElecBreakdown.mainUnits.toInt()} Units", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tenant Submeter Consumed:", style = MaterialTheme.typography.bodySmall)
                    Text("${liveElecBreakdown.tenantUnits.toInt()} Units", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated energy rate/unit:", style = MaterialTheme.typography.bodySmall)
                    Text("${settings.currencySymbol}${String.format("%.2f", liveElecBreakdown.perUnitRate)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))

                // Breakdown list
                BreakdownRow(label = "Base Monthly Tenant Rent", value = viewModel.billFormRent.toDoubleOrNull() ?: 0.0, symbol = settings.currencySymbol)
                BreakdownRow(
                    label = "Tenant Electricity Cost Share (${liveElecBreakdown.tenantUnits.toInt()} units)",
                    value = liveElecBreakdown.finalElectricityAmount,
                    symbol = settings.currencySymbol,
                    highlight = true
                )
                BreakdownRow(label = "Tenant Water split cost", value = liveWaterBreakdown.finalWaterAmount, symbol = settings.currencySymbol)
                BreakdownRow(label = "Maintenance charge", value = viewModel.billFormMaintenance.toDoubleOrNull() ?: 0.0, symbol = settings.currencySymbol)
                BreakdownRow(label = "Other adjustments/charges", value = viewModel.billFormOtherCharges.toDoubleOrNull() ?: 0.0, symbol = settings.currencySymbol)

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PREVIEW TOTAL PAYABLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${settings.currencySymbol}${String.format("%.2f", finalTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val prevPending = viewModel.getPreviousPendingDues(viewModel.billFormTenantId, viewModel.billFormMonth)
        val totalOutstanding = finalTotal + prevPending

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "LEDGER BALANCE DETAILS (CARRY-FORWARD)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Month Charges:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${settings.currencySymbol}${String.format("%.2f", finalTotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Previous Pending Due (+):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (prevPending > 0) "${settings.currencySymbol}${String.format("%.2f", prevPending)}" else "Nil / No dues",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (prevPending > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Outstanding Balance:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${settings.currencySymbol}${String.format("%.2f", totalOutstanding)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "5. Record Tenant Payment Received",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.billFormPaidAmount,
            onValueChange = { 
                viewModel.billFormPaidAmount = it
                // Auto calculate status from text input
                val paid = it.toDoubleOrNull() ?: 0.0
                viewModel.billFormPaymentStatus = when {
                    paid >= totalOutstanding -> "PAID"
                    paid > 0.0 -> "PARTIAL"
                    else -> "PENDING"
                }
            },
            label = { Text("Amount Paid (${settings.currencySymbol})") },
            placeholder = { Text("0.00") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bill_paid_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick action payment chip helpers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputChip(
                selected = (viewModel.billFormPaidAmount.toDoubleOrNull() ?: 0.0) == 0.0,
                onClick = {
                    viewModel.billFormPaidAmount = "0"
                    viewModel.billFormPaymentStatus = "PENDING"
                },
                label = { Text("No Pay (₹0)") }
            )

            InputChip(
                selected = (viewModel.billFormPaidAmount.toDoubleOrNull() ?: 0.0) == finalTotal,
                onClick = {
                    viewModel.billFormPaidAmount = String.format(Locale.US, "%.2f", finalTotal)
                    viewModel.billFormPaymentStatus = if (finalTotal >= totalOutstanding) "PAID" else "PARTIAL"
                },
                label = { Text("Current month only") }
            )

            if (prevPending > 0) {
                InputChip(
                    selected = (viewModel.billFormPaidAmount.toDoubleOrNull() ?: 0.0) == totalOutstanding,
                    onClick = {
                        viewModel.billFormPaidAmount = String.format(Locale.US, "%.2f", totalOutstanding)
                        viewModel.billFormPaymentStatus = "PAID"
                    },
                    label = { Text("Outstanding (Full)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Payment Mode Received:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf("Cash", "UPI", "Bank Transfer", "Other")
            modes.forEach { mode ->
                FilterChip(
                    selected = viewModel.billFormPaymentMethod == mode,
                    onClick = { viewModel.billFormPaymentMethod = mode },
                    label = { Text(mode) },
                    modifier = Modifier.testTag("mode_${mode.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Payment info banner
        val currentPaid = viewModel.billFormPaidAmount.toDoubleOrNull() ?: 0.0
        val remainingDue = if (totalOutstanding > currentPaid) totalOutstanding - currentPaid else 0.0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        remainingDue == 0.0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        currentPaid > 0.0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    }
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        remainingDue == 0.0 -> Icons.Default.CheckCircle
                        currentPaid > 0.0 -> Icons.Default.Info
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = when {
                        remainingDue == 0.0 -> MaterialTheme.colorScheme.primary
                        currentPaid > 0.0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            remainingDue == 0.0 -> "FULLY PAID"
                            currentPaid > 0.0 -> "PARTIALLY PAID"
                            else -> "UNPAID BILL"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            remainingDue == 0.0 -> MaterialTheme.colorScheme.primary
                            currentPaid > 0.0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = when {
                            remainingDue == 0.0 -> "Full outstanding balance cleared! Status is PAID."
                            currentPaid > 0.0 -> "Remaining balance ${settings.currencySymbol}${String.format("%.2f", remainingDue)} will be automatically carried forward to next month's bill."
                            else -> "Full amount of ${settings.currencySymbol}${String.format("%.2f", remainingDue)} will be carried forward as pending dues."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveBill() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_bill_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Bill To Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: Double, symbol: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "$symbol${String.format("%.2f", value)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ChoiceChip(selected: Boolean, onClick: () -> Unit, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------------
// 6. BILL DETAILS AND RECEIPTS PAGE (COMPLETE BREAKDOWN AND TRANSPARENCY SHARER)
// ---------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    viewModel: RentViewModel,
    tenants: List<Tenant>,
    settings: AppSettings
) {
    val context = LocalContext.current
    val bill = viewModel.bills.value.find { it.id == viewModel.selectedBillIdForDetails }
    val tenant = tenants.find { it.id == bill?.tenantId }
    val currency = settings.currencySymbol

    if (bill == null || tenant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bill invoice records not found.")
        }
        return
    }

    val isPaid = bill.paymentStatus == "PAID"
    val df = remember { DecimalFormat("#,##,##0.00") }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var dialogAmountPaid by remember { mutableStateOf("") }
    var dialogPaymentMethod by remember { mutableStateOf("Cash") }
    var dialogNotes by remember { mutableStateOf("") }

    val prevPending = viewModel.getPreviousPendingDues(bill.tenantId, bill.billMonth)
    val totalOutstanding = bill.calculatedTotalAmount + prevPending

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("bills") },
                modifier = Modifier.testTag("back_from_receipt")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "Rent Invoice Receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Status chip (taps to log/modify the payment directly)
            SuggestionChip(
                onClick = {
                    dialogAmountPaid = if (bill.paidAmount > 0.0) String.format(Locale.US, "%.2f", bill.paidAmount) else ""
                    dialogPaymentMethod = bill.paymentMethod
                    dialogNotes = bill.notes
                    showPaymentDialog = true
                },
                label = { Text(bill.paymentStatus) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = if (isPaid) MaterialTheme.colorScheme.primary else if (bill.paymentStatus == "PARTIAL") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    containerColor = if (isPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else if (bill.paymentStatus == "PARTIAL") MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("receipt_status_tog")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Receipt styling container
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "RentEase Splitting Invoice",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ROOM / SEC: ${tenant.roomName.uppercase()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Tenant: ${tenant.name}  |  Month: ${bill.billMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section: Financial elements breakdown list
                Text(
                    "BILL BREAKDOWN DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ItemizedRow(title = "Monthly House Rent", amount = "$currency${df.format(bill.rentAmount)}")
                ItemizedRow(
                    title = "Electricity share cost",
                    amount = "$currency${df.format(bill.calculatedTenantElectricity)}",
                    detail = "${(bill.tenantSubmeterCurr - bill.tenantSubmeterPrev).toInt()} units @ $currency${String.format("%.2f", CalculationsEngine.calculateElectricity(bill, tenant.numberOfPersons).perUnitRate)} / unit"
                )
                if (bill.waterBillAmount > 0) {
                    ItemizedRow(
                        title = "Water supply split cost",
                        amount = "$currency${df.format(bill.calculatedTenantWater)}",
                        detail = "Split via Mode: ${bill.waterSplitRule.replace("_", " ")}"
                    )
                }
                if (bill.maintenanceCharges > 0) {
                    ItemizedRow(title = "Maintenance base charge", amount = "$currency${df.format(bill.maintenanceCharges)}")
                }
                if (bill.otherCharges > 0) {
                    ItemizedRow(title = "Landlord other additions", amount = "$currency${df.format(bill.otherCharges)}")
                }

                // Segmented Proportion Progress Bar representing Rent, Electric and Others
                Spacer(modifier = Modifier.height(16.dp))
                val totalForBar = bill.calculatedTotalAmount
                if (totalForBar > 0) {
                    val rentWeight = ((bill.rentAmount / totalForBar).toFloat()).coerceIn(0.01f, 1f)
                    val elecWeight = ((bill.calculatedTenantElectricity / totalForBar).toFloat()).coerceIn(0.01f, 1f)
                    val otherSum = bill.calculatedTenantWater + bill.maintenanceCharges + bill.otherCharges
                    val othersWeight = ((otherSum / totalForBar).toFloat()).coerceIn(0.01f, 1f)
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "BILL RATIO PROPORTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.weight(rentWeight).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                            Box(modifier = Modifier.weight(elecWeight).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                            if (otherSum > 0) {
                                Box(modifier = Modifier.weight(othersWeight).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rent", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Electricity", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (otherSum > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Others", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section: Calculation breakdown details (Electric meter auditing)
                Text(
                    "⚡ ELECTRIC SUBMETER AUDITING TRANSPARENCY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                AuditField(label = "Board Main connection consumed (Units)", value = "${(bill.mainMeterCurr - bill.mainMeterPrev).toInt()}")
                AuditField(label = "├─ Present reading", value = bill.mainMeterCurr.toString())
                AuditField(label = "└─ Previous reading", value = bill.mainMeterPrev.toString())

                Spacer(modifier = Modifier.height(6.dp))
                AuditField(label = "Tenant submeter connection consumed (Units)", value = "${(bill.tenantSubmeterCurr - bill.tenantSubmeterPrev).toInt()}", highlight = true)
                AuditField(label = "├─ Present submeter", value = bill.tenantSubmeterCurr.toString())
                AuditField(label = "└─ Previous submeter", value = bill.tenantSubmeterPrev.toString())

                Spacer(modifier = Modifier.height(6.dp))
                AuditField(label = "Main board electric bill total amount", value = "$currency${df.format(bill.electricityBoardBillAmount)}")
                AuditField(label = "├─ Fixed board connection charges", value = "$currency${df.format(bill.fixedChargesAmount)}")
                AuditField(label = "├─ Tax portion (9%)", value = "$currency${df.format(bill.taxAmount)}")
                AuditField(label = "└─ Surcharges / adjustments", value = "$currency${df.format(bill.otherAdjustmentsAmount)}")

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                val paidValue = bill.paidAmount
                val remainingDue = if (totalOutstanding > paidValue) totalOutstanding - paidValue else 0.0

                Text(
                    "BALANCE & PAYMENT SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    DetailSettlementRow("Current Month Charges", bill.calculatedTotalAmount, currency, df)
                    DetailSettlementRow("Previous Pending Carry-over (+)", prevPending, currency, df, highlightColor = if (prevPending > 0) MaterialTheme.colorScheme.error else null)
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    DetailSettlementRow("Total Outstanding Balance", totalOutstanding, currency, df, isBold = true)
                    DetailSettlementRow("Total Paid Amount (-)", paidValue, currency, df, highlightColor = if (paidValue > 0) MaterialTheme.colorScheme.primary else null)

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NET REMAINING DUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = if (remainingDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (remainingDue == 0.0) "Cleared (${bill.paymentMethod})" else "Status: ${bill.paymentStatus} (${bill.paymentMethod})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$currency${df.format(remainingDue)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (bill.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🗒️ Notes: ${bill.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Update Payment Log", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Record how much the tenant has paid towards the total outstanding (${currency}${df.format(totalOutstanding)}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = dialogAmountPaid,
                            onValueChange = { dialogAmountPaid = it },
                            label = { Text("Amount Paid ($currency)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_paid_input")
                        )
                        
                        Text("Payment Mode:", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf("Cash", "UPI", "Bank Transfer", "Other")
                            modes.forEach { mode ->
                                FilterChip(
                                    selected = dialogPaymentMethod == mode,
                                    onClick = { dialogPaymentMethod = mode },
                                    label = { Text(mode) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = dialogNotes,
                            onValueChange = { dialogNotes = it },
                            label = { Text("Notes") },
                            placeholder = { Text("e.g. UPI ref #8294") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val paidAmt = dialogAmountPaid.toDoubleOrNull() ?: 0.0
                            val updatedStatus = when {
                                paidAmt >= totalOutstanding -> "PAID"
                                paidAmt > 0.0 -> "PARTIAL"
                                else -> "PENDING"
                            }
                            
                            viewModel.updateBillPaymentDetails(
                                bill = bill,
                                status = updatedStatus,
                                paidAmt = paidAmt,
                                method = dialogPaymentMethod,
                                notes = dialogNotes
                            )
                            showPaymentDialog = false
                        },
                        modifier = Modifier.testTag("dialog_paid_confirm")
                    ) {
                        Text("Save Ledger Record")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sharers and Actions panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share invoice button
            Button(
                onClick = {
                    val shareStr = viewModel.getShareableBillText(bill, tenant, settings)
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, shareStr)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Send invoice via")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_whatsapp_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp brand green
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("WhatsApp Share", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Delete History button
            OutlinedButton(
                onClick = {
                    viewModel.deleteBill(bill)
                    viewModel.navigateTo("bills")
                    Toast.makeText(context, "Bill deleted from ledger.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("delete_bill_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Bill")
            }
        }
    }
}

@Composable
fun ItemizedRow(title: String, amount: String, detail: String? = null) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (detail != null) {
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AuditField(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------------
// 7. LANDLORD SETTINGS SCREEN
// ---------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RentViewModel,
    currentSettings: AppSettings
) {
    var ownerName by remember { mutableStateOf(viewModel.settingsFormOwnerName) }
    var ownerPhone by remember { mutableStateOf(viewModel.settingsFormOwnerPhone) }
    var taxPercent by remember { mutableStateOf(viewModel.settingsFormTaxPercent) }
    var fixedCharges by remember { mutableStateOf(viewModel.settingsFormFixedCharges) }
    var currencySymbol by remember { mutableStateOf(viewModel.settingsFormCurrency) }
    var waterRate by remember { mutableStateOf(viewModel.settingsFormWaterRate) }

    // Synchronize local edit states with viewmodel forms on entry
    LaunchedEffect(currentSettings) {
        ownerName = currentSettings.ownerName
        ownerPhone = currentSettings.ownerPhone
        taxPercent = currentSettings.defaultElectricityTaxPercent.toString()
        fixedCharges = currentSettings.defaultFixedCharges.toString()
        currencySymbol = currentSettings.currencySymbol
        waterRate = currentSettings.defaultWaterRatePerPerson.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { viewModel.navigateTo("dashboard") },
                modifier = Modifier.testTag("cancel_settings_edit")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Preferences & Configurations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subsection: Profile Details
        Text(
            "Landlord Profile (Included on Shared text messages)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it; viewModel.settingsFormOwnerName = it },
            label = { Text("Owner Name / Sender") },
            placeholder = { Text("e.g. KM Salian") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("owner_name_input")
        )

        OutlinedTextField(
            value = ownerPhone,
            onValueChange = { ownerPhone = it; viewModel.settingsFormOwnerPhone = it },
            label = { Text("Linked Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("owner_phone_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subsection: Local calculation standards
        Text(
            "State Billing Standard Preferences",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = taxPercent,
            onValueChange = { taxPercent = it; viewModel.settingsFormTaxPercent = it },
            label = { Text("Standard Electricity State Tax Percentage (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("settings_tax_input")
        )

        OutlinedTextField(
            value = fixedCharges,
            onValueChange = { fixedCharges = it; viewModel.settingsFormFixedCharges = it },
            label = { Text("Standard Fixed Connection Charge amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("settings_fixed_input")
        )

        OutlinedTextField(
            value = waterRate,
            onValueChange = { waterRate = it; viewModel.settingsFormWaterRate = it },
            label = { Text("Water Split default index rate per headcount ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("settings_water_rate_input")
        )

        OutlinedTextField(
            value = currencySymbol,
            onValueChange = { currencySymbol = it; viewModel.settingsFormCurrency = it },
            label = { Text("Currency Indicator Symbol (e.g. ₹)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("settings_currency_symbol_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Water Split Rule Choice
        Text("Default Water splittings rules standard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                selected = viewModel.settingsFormWaterSplit == "EQUAL",
                onClick = { viewModel.settingsFormWaterSplit = "EQUAL" },
                text = "Equal Splitting"
            )
            ChoiceChip(
                selected = viewModel.settingsFormWaterSplit == "PER_PERSON",
                onClick = { viewModel.settingsFormWaterSplit = "PER_PERSON" },
                text = "Headcount Split"
            )
            ChoiceChip(
                selected = viewModel.settingsFormWaterSplit == "MANUAL",
                onClick = { viewModel.settingsFormWaterSplit = "MANUAL" },
                text = "Manual Override"
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm System Rules")
        }
    }
}

@Composable
fun DetailSettlementRow(
    label: String,
    amount: Double,
    currency: String,
    df: java.text.DecimalFormat,
    isBold: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$currency${df.format(amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold || highlightColor != null) FontWeight.Bold else FontWeight.SemiBold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
