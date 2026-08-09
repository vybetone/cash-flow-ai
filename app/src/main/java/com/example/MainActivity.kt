package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CashFlowApp
import com.example.ui.CashFlowViewModel
import com.example.ui.theme.CashFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashFlowTheme {
                val viewModel: CashFlowViewModel = viewModel()
                CashFlowApp(viewModel = viewModel)
            }
        }
    }
}
