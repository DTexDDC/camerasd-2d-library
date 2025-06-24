package com.example.datdt.scanningsdk2D

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.datdt.scanningsdk2D.ui.theme._2DScanningSDKAndroidTheme
import com.example.datdt.scanningsdk2D.CameraScreen
import com.example.datdt.scanningsdk2D.models.ModelType
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "camera"
            ) {
//                composable("demo") {
//                    DemoScreen(navController = navController)
//                }
                composable(
                    route ="camera",
                ) {
                    val modelType = it.savedStateHandle.get<ModelType>("type") ?: ModelType.OUTPUT_FLOAT32
                    CameraScreen(modifier = Modifier.fillMaxSize() , modelType = modelType)
                }
            }
        }
    }
}

//@Composable
//fun DemoScreen(navController: NavController) {
//    Scaffold(modifier = Modifier.fillMaxSize()) {
//        Column(
//            modifier = Modifier.fillMaxSize().padding(it),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            ModelType.entries.map {
//                Button(
//                    onClick = {
//                        navController.navigate("camera")
//                    }
//                ) {
//                    Text(it.name)
//                }
//            }
//        }
//    }
//}