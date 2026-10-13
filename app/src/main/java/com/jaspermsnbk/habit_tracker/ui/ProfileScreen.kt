package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.PetUi
import com.jaspermsnbk.habit_tracker.ui.pet.Outfit
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas

/**
 * You: your pet at a glance, then the trends as cards to scroll through. Tapping the pet opens the Pet tab.
 */
@Composable
fun ProfileScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenPet: () -> Unit = {},
    shop: ShopViewModel? = null,
) {
    val pet by viewModel.pet.collectAsState()
    val balance by (shop?.balance ?: MutableStateFlow(0)).collectAsState()
    val outfit by (shop?.outfit ?: MutableStateFlow(Outfit.None)).collectAsState()
    var showShop by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            HabitTopBar("Profile", onOpenSettings, extraActions = {
                if (shop != null) {
                    IconButton(onClick = { showShop = true }) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = "Shop")
                    }
                }
            })
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(pet, outfit, balance.takeIf { shop != null }, onOpenPet)
            TrendsWidgets(viewModel)
        }
    }

    if (showShop && shop != null) ShopSheet(shop, pet, onDismiss = { showShop = false })
}

@Composable
private fun ProfileHeader(pet: PetUi?, outfit: Outfit, gems: Int?, onOpenPet: () -> Unit) {
    Card(
        onClick = onOpenPet,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pet == null) {
                Column {
                    Text("No pet yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose one on the Pet tab and your habits will feed it.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                PetCanvas(pet.look(outfit), Modifier.size(72.dp))
                Column(Modifier.weight(1f)) {
                    Text(pet.name, style = MaterialTheme.typography.titleLarge)
                    if (pet.state.alive) {
                        Text("Level ${pet.state.level} · ${pet.stageName()}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Needs a new owner", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (gems != null) GemBalance(gems)
        }
    }
}
