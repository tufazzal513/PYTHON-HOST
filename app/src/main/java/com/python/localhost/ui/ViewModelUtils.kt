package com.python.localhost.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.python.localhost.di.AppContainer

/**
 * Creates a ViewModel bound to the app's AppContainer (composition root).
 */
@Composable
inline fun <reified VM : ViewModel> provideVm(
    container: AppContainer,
    crossinline factory: (AppContainer) -> VM,
): VM = viewModel(
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = factory(container) as T
    }
)
