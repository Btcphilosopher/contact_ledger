package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.ContactRepository
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.ContactDetailsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LocationMapScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ContactLedgerViewModel
import com.example.ui.viewmodel.ContactLedgerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Offline Local Room Engine bootstrap
        val database = AppDatabase.getDatabase(this)
        val repository = ContactRepository(database.contactDao())
        val factory = ContactLedgerViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[ContactLedgerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Context mapping to conditionally anchor Bottom Bar navigation
                val showBottomBar = currentRoute in listOf("dashboard", "projects", "location_map")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                                    label = { Text("Contacts") },
                                    selected = currentRoute == "dashboard",
                                    onClick = {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                                    label = { Text("Projects") },
                                    selected = currentRoute == "projects",
                                    onClick = {
                                        navController.navigate("projects") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Map, contentDescription = "Radar Map") },
                                    label = { Text("Radar Map") },
                                    selected = currentRoute == "location_map",
                                    onClick = {
                                        navController.navigate("location_map") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onContactClick = { id -> navController.navigate("contact_details/$id") },
                                onAddContactClick = { navController.navigate("add_edit_contact/0") }
                            )
                        }

                        composable(
                            route = "contact_details/{contactId}",
                            arguments = listOf(navArgument("contactId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                            ContactDetailsScreen(
                                contactId = contactId,
                                viewModel = viewModel,
                                onEditClick = { id -> navController.navigate("add_edit_contact/$id") },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "add_edit_contact/{contactId}",
                            arguments = listOf(navArgument("contactId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                            AddEditContactScreen(
                                contactId = contactId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("projects") {
                            ProjectsScreen(
                                viewModel = viewModel,
                                onContactClick = { id -> navController.navigate("contact_details/$id") }
                            )
                        }

                        composable("location_map") {
                            LocationMapScreen(
                                viewModel = viewModel,
                                onContactClick = { id -> navController.navigate("contact_details/$id") }
                            )
                        }
                    }
                }
            }
        }
    }
}
