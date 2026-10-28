package com.fakeprofilescan.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fakeprofilescan.app.AppViewModel
import com.fakeprofilescan.app.BuildConfig
import com.fakeprofilescan.app.data.Band

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val all = vm.history.state()
    var filter by remember { mutableStateOf<Band?>(null) }
    var query by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf<String?>(null) }
    val shown = all.filter { (filter == null || it.band == filter) && (decision == null || it.decision == decision) && (query.isBlank() || it.handle?.contains(query.trim(), true) == true || it.note.contains(query.trim(), true)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search handle or notes") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filter == null, { filter = null }, label = { Text("All") })
                    Band.entries.forEach { b -> FilterChip(filter == b, { filter = if (filter == b) null else b }, label = { Text(b.title()) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DECISIONS.forEach { d -> FilterChip(decision == d, { decision = if (decision == d) null else d }, label = { Text(d) }) }
                }
            }
        }
        if (shown.isEmpty()) item {
            SurfaceCard {
                Text(if (all.isEmpty()) "No saved assessments" else "Nothing matches", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (all.isEmpty()) "Results you save appear here. They stay on this device and are removed automatically after your retention period (see Settings)."
                    else "Try a different search or filter.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(shown, key = { it.id }) { a ->
            SurfaceCard(Modifier.clickable { onOpen(a.id) }) {
                Text(a.band.title(), style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(a.handle?.let { "@$it" }, formatDate(a.assessedAt), a.decision.ifEmpty { null }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                if (a.note.isNotBlank()) Text(a.note, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LearnScreen() {
    val items = listOf(
        "Common signs of scam or spam profiles" to "Very new or empty profiles with many followed accounts, usernames that are mostly digits, " +
            "copied or stock photos, urgent requests for money or codes, and links to unrelated sites.",
        "Before you follow, message or pay" to "Look at posting history and comments, search the name elsewhere, and confirm through a channel you already trust. " +
            "Never share one-time codes, passwords or card details.",
        "If someone is impersonating you" to "Collect screenshots and the profile link, then use the platform's own impersonation report form. " +
            "Tell friends so they do not engage.",
        "Reporting" to "Fake Profile Scan does not report accounts. Use the report option inside the platform, which is the only route that can lead to action.",
        "Limits of this tool" to "Scores come from a small public dataset and a simple statistical model. Real accounts can score high and fake ones can score low. " +
            "Use the result as one input, never as a verdict.",
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (t, b) -> SurfaceCard { Text(t, style = MaterialTheme.typography.titleMedium); Text(b, style = MaterialTheme.typography.bodyMedium) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = vm.settings.state()
    val ctx = LocalContext.current
    var url by remember(s.apiUrl) { mutableStateOf(s.apiUrl) }
    var confirmClear by remember { mutableStateOf(false) }
    val urlOk = url.isBlank() || url.startsWith("https://") || (BuildConfig.DEBUG && url.startsWith("http://"))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Section("Server") {
            OutlinedTextField(
                url, { url = it }, label = { Text("Assessment server address") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = !urlOk, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = { Text(if (urlOk) "HTTPS address of your Fake Profile Scan API." else "Must start with https://") },
            )
            OutlinedButton({ vm.updateSettings(s.copy(apiUrl = url.trim())) }, enabled = urlOk && url.trim() != s.apiUrl,
                modifier = Modifier.heightIn(min = 48.dp)) { Text("Save address") }
        }

        Section("Privacy") {
            ToggleRow("Keep username with saved results", "Off by default. When off, history shows only date and risk level.", s.storeHandle) { vm.updateSettings(s.copy(storeHandle = it)) }
            Text("Keep history for", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 days", 30 to "30 days", 90 to "90 days", 0 to "Until I delete").forEach { (d, l) ->
                    FilterChip(s.retentionDays == d, { vm.updateSettings(s.copy(retentionDays = d)) }, label = { Text(l) })
                }
            }
            Text("The bio text and other profile details you type are sent to the server only to compute the score and are not stored by it.", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton({
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, vm.exportJson()), null))
            }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Export history") }
            TextButton({ confirmClear = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Delete all history") }
        }

        Section("About") {
            Text("Fake Profile Scan ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
            Text("Fake Profile Scan gives risk estimates from visible profile signals. It does not identify people, prove intent, access any social platform, or report accounts. " +
                "Not affiliated with Instagram or Meta.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Delete all history?") }, text = { Text("This permanently removes every saved assessment and note from this device.") },
        confirmButton = { TextButton({ confirmClear = false; vm.clearHistory() }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } },
    )
}
