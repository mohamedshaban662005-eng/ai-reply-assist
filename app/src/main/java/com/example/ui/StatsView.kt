package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.DayStats
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.WeeklyStats
import org.json.JSONArray
import org.json.JSONObject

enum class ChartEngine {
    RECHARTS,
    NATIVE
}

/**
 * StatsView component to visualize notifications handled vs. total AI responses generated over the last 7 days.
 * Supports interactive Recharts (via web engine) as well as native Jetpack Compose chart rendering.
 */
@Composable
fun StatsView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.last7DaysStats.collectAsState()
    StatsView(stats = stats, modifier = modifier)
}

@Composable
fun StatsView(
    stats: WeeklyStats,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var selectedEngine by remember { mutableStateOf(ChartEngine.RECHARTS) }
    var selectedDayIndex by remember { mutableIntStateOf(stats.dailyStats.lastIndex.coerceAtLeast(0)) }

    // Keep selectedDayIndex in bounds if stats change
    LaunchedEffect(stats.dailyStats.size) {
        if (selectedDayIndex !in stats.dailyStats.indices && stats.dailyStats.isNotEmpty()) {
            selectedDayIndex = stats.dailyStats.lastIndex
        }
    }

    val selectedDay = stats.dailyStats.getOrNull(selectedDayIndex)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_view"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Title and Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.stats_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stats_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "Handled",
                    value = "${stats.totalNotifications}",
                    icon = Icons.Default.Notifications,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f).testTag("metric_handled_7d")
                )
                MetricChip(
                    title = "AI Replies",
                    value = "${stats.totalAiResponses}",
                    icon = Icons.Default.AutoAwesome,
                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    contentColor = Color(0xFF059669),
                    modifier = Modifier.weight(1f).testTag("metric_ai_7d")
                )
                MetricChip(
                    title = "Rate",
                    value = "${stats.responseRatePercentage}%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f).testTag("metric_rate_7d")
                )
            }

            // Engine Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last 7 Days (Daily Traffic)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedEngine == ChartEngine.RECHARTS,
                        onClick = { selectedEngine = ChartEngine.RECHARTS },
                        label = { Text(stringResource(R.string.stats_view_recharts), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chip_engine_recharts")
                    )
                    FilterChip(
                        selected = selectedEngine == ChartEngine.NATIVE,
                        onClick = { selectedEngine = ChartEngine.NATIVE },
                        label = { Text(stringResource(R.string.stats_view_native), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chip_engine_native")
                    )
                }
            }

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6366F1))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.stats_notifications_handled),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.stats_ai_responses),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chart Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    )
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("stats_chart_container")
            ) {
                when (selectedEngine) {
                    ChartEngine.RECHARTS -> {
                        RechartsWebView(
                            dailyStats = stats.dailyStats,
                            isDark = isDark,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("recharts_webview")
                        )
                    }
                    ChartEngine.NATIVE -> {
                        NativeBarChart(
                            dailyStats = stats.dailyStats,
                            selectedIndex = selectedDayIndex,
                            onDaySelected = { selectedDayIndex = it },
                            isDark = isDark,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("native_bar_chart")
                        )
                    }
                }
            }

            // Interactive Day Detail Card
            if (selectedDay != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_day_detail")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedDay.dayLabel} (${selectedDay.fullDateLabel})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.stats_tap_to_inspect),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selectedDay.notificationsHandled}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    text = "Handled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selectedDay.aiResponses}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "AI Replies",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Recharts WebView component. Loads HTML with React 18 & Recharts 2 UMD, with an inline SVG fallback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebView(
    dailyStats: List<DayStats>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val htmlContent = remember(dailyStats, isDark) {
        buildRechartsHtml(dailyStats, isDark)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0) // Transparent background
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = false
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://recharts.org", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

/**
 * Native Jetpack Compose Bar Chart visualization with Recharts aesthetic styling.
 */
@Composable
fun NativeBarChart(
    dailyStats: List<DayStats>,
    selectedIndex: Int,
    onDaySelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (dailyStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.stats_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = remember(dailyStats) {
        val maxInList = dailyStats.maxOfOrNull { maxOf(it.notificationsHandled, it.aiResponses) } ?: 10
        ((maxInList + 4) / 5) * 5 // Round up to nearest multiple of 5
    }.coerceAtLeast(5)

    val gridColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(modifier = modifier) {
        // Chart Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Background horizontal grid lines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(gridColor)
                    )
                }
            }

            // Columns row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEachIndexed { index, day ->
                    val isSelected = index == selectedIndex
                    val handledFraction = (day.notificationsHandled.toFloat() / maxVal).coerceIn(0.04f, 1f)
                    val aiFraction = (day.aiResponses.toFloat() / maxVal).coerceIn(0.04f, 1f)

                    val animHandled by animateFloatAsState(
                        targetValue = handledFraction,
                        animationSpec = tween(durationMillis = 600),
                        label = "handled_$index"
                    )
                    val animAi by animateFloatAsState(
                        targetValue = aiFraction,
                        animationSpec = tween(durationMillis = 600),
                        label = "ai_$index"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { onDaySelected(index) }
                            .padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label if selected
                        if (isSelected) {
                            Text(
                                text = "${day.notificationsHandled}/${day.aiResponses}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Dual Bars Row
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        ) {
                            // Notifications Handled Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(animHandled)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFF6366F1))
                            )
                            // AI Responses Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(animAi)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dailyStats.forEachIndexed { index, day ->
                val isSelected = index == selectedIndex
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Builds an HTML payload that instantiates Recharts via React 18, with an SVG fallback.
 */
private fun buildRechartsHtml(dailyStats: List<DayStats>, isDark: Boolean): String {
    val jsonArray = JSONArray()
    for (stat in dailyStats) {
        val obj = JSONObject().apply {
            put("day", stat.dayLabel)
            put("date", stat.fullDateLabel)
            put("notificationsHandled", stat.notificationsHandled)
            put("aiResponses", stat.aiResponses)
        }
        jsonArray.put(obj)
    }

    val bgColor = if (isDark) "#0F172A" else "#F8FAFC"
    val textColor = if (isDark) "#F8FAFC" else "#0F172A"
    val subColor = if (isDark) "#94A3B8" else "#64748B"
    val gridColor = if (isDark) "#1E293B" else "#E2E8F0"
    val tooltipBg = if (isDark) "#1E293B" else "#FFFFFF"
    val tooltipBorder = if (isDark) "#334155" else "#CBD5E1"

    val maxVal = dailyStats.maxOfOrNull { maxOf(it.notificationsHandled, it.aiResponses) }?.coerceAtLeast(5) ?: 10

    // Build SVG fallback bars
    val svgBars = StringBuilder()
    val count = dailyStats.size.coerceAtLeast(1)
    val svgWidth = 320
    val svgHeight = 170
    val colWidth = svgWidth / count
    val barWidth = (colWidth * 0.32).toInt().coerceAtLeast(6)

    dailyStats.forEachIndexed { idx, item ->
        val xCenter = idx * colWidth + (colWidth / 2)
        val hHandled = ((item.notificationsHandled.toDouble() / maxVal) * (svgHeight - 40)).toInt().coerceAtLeast(4)
        val hAi = ((item.aiResponses.toDouble() / maxVal) * (svgHeight - 40)).toInt().coerceAtLeast(4)

        val xHandled = xCenter - barWidth - 1
        val yHandled = svgHeight - 25 - hHandled

        val xAi = xCenter + 1
        val yAi = svgHeight - 25 - hAi

        svgBars.append(
            """
            <rect x="$xHandled" y="$yHandled" width="$barWidth" height="$hHandled" rx="3" fill="#6366F1" />
            <rect x="$xAi" y="$yAi" width="$barWidth" height="$hAi" rx="3" fill="#10B981" />
            <text x="$xCenter" y="${svgHeight - 8}" text-anchor="middle" font-size="10" fill="$subColor">${item.dayLabel}</text>
            """.trimIndent()
        )
    }

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Recharts StatsView</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: $bgColor;
      color: $textColor;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      overflow: hidden;
      width: 100vw;
      height: 100vh;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }
    #recharts-root {
      width: 100%;
      height: 100%;
      display: none;
    }
    #fallback-root {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }
    .grid-line { stroke: $gridColor; stroke-dasharray: 3 3; }
  </style>
  <script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
</head>
<body>

  <!-- SVG Fallback Chart shown immediately -->
  <div id="fallback-root">
    <svg viewBox="0 0 320 180" style="width: 100%; height: 100%; max-height: 240px; padding: 4px;">
      <line x1="10" y1="25" x2="310" y2="25" class="grid-line" stroke-width="1" />
      <line x1="10" y1="65" x2="310" y2="65" class="grid-line" stroke-width="1" />
      <line x1="10" y1="105" x2="310" y2="105" class="grid-line" stroke-width="1" />
      <line x1="10" y1="145" x2="310" y2="145" class="grid-line" stroke-width="1" />
      $svgBars
    </svg>
  </div>

  <!-- Recharts Root -->
  <div id="recharts-root"></div>

  <script>
    const data = $jsonArray;

    function renderRecharts() {
      if (window.Recharts && window.React && window.ReactDOM) {
        try {
          const { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } = window.Recharts;
          const e = window.React.createElement;

          const ChartApp = () => {
            return e(ResponsiveContainer, { width: '100%', height: '100%' },
              e(BarChart, { data: data, margin: { top: 12, right: 12, left: -22, bottom: 4 } },
                e(CartesianGrid, { strokeDasharray: '3 3', stroke: '$gridColor', vertical: false }),
                e(XAxis, { dataKey: 'day', stroke: '$subColor', fontSize: 11, tickLine: false }),
                e(YAxis, { stroke: '$subColor', fontSize: 11, tickLine: false, allowDecimals: false }),
                e(Tooltip, {
                  contentStyle: {
                    backgroundColor: '$tooltipBg',
                    borderColor: '$tooltipBorder',
                    borderRadius: '8px',
                    color: '$textColor',
                    fontSize: '11px',
                    boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)'
                  },
                  labelStyle: { fontWeight: 'bold', marginBottom: '2px' }
                }),
                e(Bar, { dataKey: 'notificationsHandled', name: 'Handled', fill: '#6366F1', radius: [3, 3, 0, 0] }),
                e(Bar, { dataKey: 'aiResponses', name: 'AI Replies', fill: '#10B981', radius: [3, 3, 0, 0] })
              )
            );
          };

          const rootElem = document.getElementById('recharts-root');
          const fallbackElem = document.getElementById('fallback-root');
          if (rootElem && fallbackElem) {
            const root = window.ReactDOM.createRoot(rootElem);
            root.render(e(ChartApp));
            fallbackElem.style.display = 'none';
            rootElem.style.display = 'block';
          }
        } catch (err) {
          console.error("Recharts render error", err);
        }
      }
    }

    // Try rendering Recharts once scripts load
    if (window.Recharts) {
      renderRecharts();
    } else {
      window.addEventListener('load', renderRecharts);
      setTimeout(renderRecharts, 400);
      setTimeout(renderRecharts, 1200);
    }
  </script>
</body>
</html>
    """.trimIndent()
}
