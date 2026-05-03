package com.example.tampraktikum

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.tampraktikum.model.Food
import com.example.tampraktikum.network.RetrofitClient
import com.example.tampraktikum.ui.theme.TAMPraktikumTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TAMPraktikumTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    var foods by remember { mutableStateOf<List<Food>>(emptyList()) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            DaftarMakananScreen(
                navController = navController,
                onFoodsLoaded = { foods = it }
            )
        }

        composable("detail/{nama}") { backStackEntry ->
            val nama = backStackEntry.arguments?.getString("nama")
            val food = foods.find { it.nama == nama }

            if (food != null) {
                DetailScreen(food, navController, true)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Data tidak ditemukan", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun DaftarMakananScreen(
    navController: NavController,
    onFoodsLoaded: (List<Food>) -> Unit = {}
) {
    var foods by remember { mutableStateOf<List<Food>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            foods = RetrofitClient.instance.getFoods()
            onFoodsLoaded(foods)
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            isError = true
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        isError || foods.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Gagal Memuat Data", color = Color.Red)
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Rekomendasi Populer",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(foods) { food ->
                            FoodRowItem(food, navController)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Daftar Menu Lengkap",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(foods) { food ->
                    DetailScreen(food, navController, false)
                }
            }
        }
    }
}

@Composable
fun FoodRowItem(food: Food, navController: NavController) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable {
                navController.navigate("detail/${Uri.encode(food.nama)}")
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = food.imageUrl,
                contentDescription = food.nama,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )

            Column(Modifier.padding(8.dp)) {
                Text(food.nama, fontWeight = FontWeight.Bold)
                Text("Rp ${food.harga}", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DetailScreen(
    food: Food,
    navController: NavController,
    isFullScreen: Boolean
) {
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = food.nama,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(8.dp))

                Text(food.nama, fontWeight = FontWeight.Bold)
                Text(food.deskripsi)
                Text("Rp ${food.harga}", color = MaterialTheme.colorScheme.primary)

                Spacer(Modifier.height(12.dp))

                if (isFullScreen) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                delay(2000)
                                snackbar.showSnackbar("Pesanan berhasil!")
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Pesan")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { navController.popBackStack() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}