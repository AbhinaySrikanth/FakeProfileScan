package com.fakeprofilescan.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fakeprofilescan.app.AppViewModel
import com.fakeprofilescan.app.data.Assessment
import com.fakeprofilescan.app.data.Band

private const val PLATFORM_HELP = "https://help.instagram.com/"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(vm: AppViewModel, id: String, onNew: () -> Unit, onGone: () -> Unit) {
    val history = vm.history.state()
    val current = vm.current.state()
    val a = history.firstOrNull { it.id == id } ?: current?.takeIf { it.id == id }
    val saved = history.any { it.id == id }
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    // Result removed (deleted, or expired by retention): leave rather than show a blank page.
    LaunchedEffect(a == null) { if (a == null) onGone() }
    if (a == null) return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SurfaceCard {
            Text(a.handle?.let { "@$it" } ?: "Profile assessed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(a.band.title(), style = MaterialTheme.typography.headlineMedium,
                color = if (a.band == Band.ELEVATED) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
            Text(summary(a.band), style = MaterialTheme.typography.bodyLarge)
            a.escalation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
            vm.previousFor(a)?.let { p ->
                val d = a.score - p.score
                Text("Previously ${p.band.title().lowercase()} (${formatDate(p.assessedAt)}): " +
                    if (Math.abs(d) < 0.05) "little change." else if (d > 0) "model score is up ${"%.2f".format(d)}." else "model score is down ${"%.2f".format(-d)}.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (a.lowConfidence) Text(
                "Lower confidence: " + if (a.unusualInputs.isNotEmpty()) "${a.unusualInputs.joinToString()} fall outside what the model was trained on." else "the signals point in mixed directions.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary,
            )
            Text("Model score ${"%.2f".format(a.score)}${if (a.modelBand != a.band) " (model alone: ${a.modelBand.title().lowercase()})" else ""} · all 11 signals provided", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("What we observed") {
            a.signals.forEach { s ->
                Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(s.name, style = MaterialTheme.typography.bodyLarge)
                    Text(if (s.raises) "Pushed the assessment toward higher risk" else "Pushed the assessment toward lower risk",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (a.flags.isNotEmpty()) Section("Behaviour you reported") {
            a.flags.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }

        Section("What this does not mean") {
            Text("This is not proof that the account is fake or that its owner intends harm. Real people can look unusual, " +
                "and careful bad actors can look ordinary. Treat it as one input to your own judgement.", style = MaterialTheme.typography.bodyMedium)
        }

        Section("What you can do") {
            summaryActions(a.band).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }

        OutlinedTextField(
            value = a.note, onValueChange = { vm.updateNote(a.id, it.take(500)) },
            label = { Text("Your notes") }, supportingText = { Text("Stored on this device only.") },
            minLines = 3, modifier = Modifier.fillMaxWidth(),
        )

        if (saved) Section("What did you decide?") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DECISIONS.forEach { d -> FilterChip(a.decision == d, { vm.setDecision(a.id, if (a.decision == d) "" else d) }, label = { Text(d) }) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (saved) Text("Saved to history", style = MaterialTheme.typography.labelLarge)
            else Button({ vm.save(a) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save to history") }
            OutlinedButton({
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, report(a)), null))
            }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Share summary") }
            OutlinedButton({
                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLATFORM_HELP))) } catch (_: ActivityNotFoundException) {}
            }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Open the platform's safety and reporting help") }
            OutlinedButton(onNew, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Assess another profile") }
            if (saved) TextButton({ confirmDelete = true }, Modifier.fillMaxWidth()) { Text("Delete this assessment") }
        }

        Text("Model ${a.modelVersion} · ${formatDate(a.assessedAt)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete assessment?") }, text = { Text("This removes it and your notes from this device.") },
        confirmButton = { TextButton({ confirmDelete = false; vm.delete(a.id) }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
    )
}

private fun summary(b: Band) = when (b) {
    Band.LOW -> "Few of the signals we check stood out for this profile."
    Band.REVIEW -> "Some signals are commonly seen on low-authenticity accounts. A closer manual look is sensible."
    Band.ELEVATED -> "Several signals commonly associated with spam or impersonation are present."
}

private fun summaryActions(b: Band) = when (b) {
    Band.LOW -> listOf("Still avoid sharing money or personal details with someone you only know online.")
    Band.REVIEW -> listOf("Check whether the account's posts, comments and tagged photos look consistent over time.",
        "Search for the person elsewhere to see if the details match.")
    Band.ELEVATED -> listOf("Do not send money, codes or personal information.",
        "Check for copied photos and look-alike usernames of someone you know.",
        "If it is abusive or impersonating someone, use the platform's own report tool.")
}

private fun report(a: Assessment) = buildString {
    appendLine("Fake Profile Scan assessment: ${a.band.title()}")
    a.handle?.let { appendLine("Profile: @$it") }
    appendLine("Model score: ${"%.2f".format(a.score)}${if (a.lowConfidence) " (lower confidence)" else ""}")
    a.escalation?.let { appendLine(it) }
    a.flags.forEach { appendLine("Reported: $it") }
    a.signals.forEach { appendLine("- ${it.name}: ${if (it.raises) "raises" else "lowers"} risk") }
    appendLine("Not proof of fraud or identity. Model ${a.modelVersion}, ${a.assessedAt}.")
}
