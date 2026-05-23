package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.RentDatabase
import com.example.data.RentRepository
import com.example.ui.RentEaseApp
import com.example.ui.RentViewModel
import com.example.ui.RentViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Offline Room persistent storage
        val database = RentDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = RentRepository(database.rentDao)
        
        // Setup direct constructor dynamic injection for our ViewModel state controller
        val factory = RentViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[RentViewModel::class.java]

        setContent {
            MyApplicationTheme {
                RentEaseApp(viewModel = viewModel)
            }
        }
    }
}
