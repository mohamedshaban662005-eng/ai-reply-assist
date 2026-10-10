package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.WeeklyStats

@Composable
fun StatsView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    com.example.ui.StatsView(viewModel = viewModel, modifier = modifier)
}

@Composable
fun StatsView(
    stats: WeeklyStats,
    modifier: Modifier = Modifier
) {
    com.example.ui.StatsView(stats = stats, modifier = modifier)
}
