package com.cadence.songs.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.songs.ui.AppViewModel
import com.cadence.songs.ui.theme.ChordColorNames
import com.cadence.songs.ui.theme.chordPalette
import com.cadence.songs.ui.theme.isDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val dark = isDarkTheme()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportBackup(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Sauvegarde enregistrée" else "Échec de la sauvegarde",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importBackup(uri) { n ->
                Toast.makeText(
                    context,
                    if (n >= 0) "$n morceau(x) restauré(s)" else "Fichier de sauvegarde invalide",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Réglages",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp)
        )

        SectionCard("Apparence") {
            val themes = listOf("Auto", "Clair", "Sombre", "Scène")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEachIndexed { i, label ->
                    FilterChip(
                        selected = settings.theme == i,
                        onClick = { vm.updateSettings { it.copy(theme = i) } },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                "Le mode Scène est un noir pur très contrasté, pensé pour jouer sur une scène sombre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Affichage des morceaux") {
            Text("Taille du texte : ${settings.fontSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.fontSize,
                onValueChange = { v -> vm.updateSettings { it.copy(fontSize = v) } },
                valueRange = 14f..44f
            )

            Text("Notation des accords", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.chordStyle == 0,
                    onClick = { vm.updateSettings { it.copy(chordStyle = 0) } },
                    label = { Text("Lettres (C, Am)") }
                )
                FilterChip(
                    selected = settings.chordStyle == 1,
                    onClick = { vm.updateSettings { it.copy(chordStyle = 1) } },
                    label = { Text("Nashville (1, 6m)") }
                )
            }

            Text("Colonnes (utile sur tablette)", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { n ->
                    FilterChip(
                        selected = settings.columns == n,
                        onClick = { vm.updateSettings { it.copy(columns = n) } },
                        label = { Text("$n") }
                    )
                }
            }

            Text("Couleur des accords", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                chordPalette(dark).forEachIndexed { i, color ->
                    ColorSwatch(
                        color = color,
                        selected = settings.chordColor == i,
                        description = ChordColorNames[i],
                        onClick = { vm.updateSettings { it.copy(chordColor = i) } }
                    )
                }
            }

            ToggleRow(
                "Afficher les accords",
                settings.showChords
            ) { on -> vm.updateSettings { it.copy(showChords = on) } }
            ToggleRow(
                "Garder l'écran allumé",
                settings.keepScreenOn
            ) { on -> vm.updateSettings { it.copy(keepScreenOn = on) } }
        }

        SectionCard("Transfert Wi-Fi (ordinateur)") {
            val wifi by vm.wifi.state.collectAsState()
            Text(
                "Ajoutez ou modifiez vos morceaux depuis un ordinateur connecté au même Wi-Fi, " +
                    "avec n'importe quel navigateur. Gardez l'appli ouverte pendant le transfert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToggleRow("Activer le transfert Wi-Fi", wifi.running) { on ->
                if (on) vm.wifi.start() else vm.wifi.stop()
            }
            if (wifi.error != null) {
                Text(wifi.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (wifi.running) {
                Text("Sur l'ordinateur, ouvrez cette adresse :", style = MaterialTheme.typography.bodyMedium)
                if (wifi.addresses.isEmpty()) {
                    Text(
                        "Aucune adresse trouvée : connectez l'appareil à un Wi-Fi.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                wifi.addresses.forEach { url ->
                    Text(url, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text("Puis saisissez le code :", style = MaterialTheme.typography.bodyMedium)
                Text(wifi.pin, style = MaterialTheme.typography.headlineMedium, letterSpacing = 6.sp)
                FilledTonalButton(onClick = { vm.wifi.newPin() }) { Text("Changer le code") }
            }
        }

        SectionCard("Sauvegarde") {
            Text(
                "Exportez tous vos morceaux et vos sets dans un fichier, ou restaurez-les sur un autre appareil. " +
                    "La restauration ajoute les morceaux sans rien écraser. Les partitions PDF ne sont pas incluses.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { exportLauncher.launch("cadence-sauvegarde.json") }) {
                    Icon(Icons.Rounded.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exporter")
                }
                FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                    Icon(Icons.Rounded.Restore, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restaurer")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ColorSwatch(color: Color, selected: Boolean, description: String, onClick: () -> Unit) {
    val ring = BorderStroke(3.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(ring, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = description, tint = Color.White)
    }
}
