package com.fakeprofilescan.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fakeprofilescan.app.AppViewModel
import com.fakeprofilescan.app.SubmitState
import com.fakeprofilescan.app.data.FormState

@Composable
fun HomeScreen(vm: AppViewModel, onStart: () -> Unit, onOpen: (String) -> Unit) {
    val history = vm.history.state()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Check a profile before you trust it", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Enter what you can see on a public profile. Fake Profile Scan returns a risk assessment with the signals behind it, " +
                    "so you can decide what to do next.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(onClick = onStart, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Start assessment") }

        SurfaceCard {
            Text("What this is, and is not", style = MaterialTheme.typography.titleMedium)
            Text("An estimate from visible signals such as follower ratios and username patterns.", style = MaterialTheme.typography.bodyMedium)
            Text("Not proof that an account is fake, and not a judgement about the person behind it.", style = MaterialTheme.typography.bodyMedium)
            Text("It never contacts the platform and never reports anyone on your behalf.", style = MaterialTheme.typography.bodyMedium)
        }

        if (history.isNotEmpty()) SurfaceCard {
            Text("Your saved checks", style = MaterialTheme.typography.titleMedium)
            Text("${history.size} saved · ${history.count { it.band == com.fakeprofilescan.app.data.Band.ELEVATED }} elevated · " +
                "${history.count { it.band == com.fakeprofilescan.app.data.Band.REVIEW }} worth a closer look · ${history.count { it.decision.isEmpty() }} without a decision",
                style = MaterialTheme.typography.bodyMedium)
        }

        if (history.isNotEmpty()) Section("Recent") {
            history.take(3).forEach { a ->
                SurfaceCard(Modifier.clickable { onOpen(a.id) }) {
                    Text(a.band.title(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(a.handle?.let { "@$it" }, formatDate(a.assessedAt)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun FormScreen(vm: AppViewModel) {
    val f = vm.form.state()
    val show = vm.showErrors.state()
    val server = vm.serverErrors.state()
    val submit = vm.submit.state()
    val errors = if (show) f.errors() + server else server
    val busy = submit is SubmitState.Loading

    @Composable
    fun field(key: String, label: String, value: String, help: String, numeric: Boolean = false, last: Boolean = false,
              lines: Int = 1, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(if (numeric) it.filter { c -> c.isDigit() || c in ".,KkMmBb" }.take(12) else it) },
            label = { Text(label) },
            isError = key in errors,
            supportingText = { Text(errors[key] ?: help) },
            enabled = !busy,
            singleLine = lines == 1,
            minLines = lines,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Ascii else KeyboardType.Text,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
            ),
        )
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Everything here is visible on the public profile. Nothing is posted anywhere.", style = MaterialTheme.typography.bodyMedium)

        Section("Profile") {
            field("username", "Username", f.username, "Without the @, no spaces. Example: anna_travels") { v -> vm.edit { it.copy(username = v) } }
            field("fullName", "Display name (optional)", f.fullName, "As shown under the photo. Leave empty if none.") { v -> vm.edit { it.copy(fullName = v) } }
            field("bio", "Bio (optional)", f.bio, "${f.bio.length}/150. Only its length is used; the text is not sent.", lines = 3) { v -> vm.edit { it.copy(bio = v.take(150)) } }
            ToggleRow("Has a profile picture", "Any photo other than the default avatar", f.hasPicture, !busy) { v -> vm.edit { it.copy(hasPicture = v) } }
            ToggleRow("Link in bio", "An external website is listed", f.hasLink, !busy) { v -> vm.edit { it.copy(hasLink = v) } }
            ToggleRow("Private account", "Posts are hidden from non-followers", f.isPrivate, !busy) { v -> vm.edit { it.copy(isPrivate = v) } }
        }

        Section("Activity and audience") {
            field("posts", "Posts", f.posts, "Total number of posts. Enter 0 if none.", numeric = true) { v -> vm.edit { it.copy(posts = v) } }
            field("followers", "Followers", f.followers, "As shown on the profile: 850, 12,400, 1.2K or 3M.", numeric = true) { v -> vm.edit { it.copy(followers = v) } }
            field("following", "Following", f.following, "Accounts this profile follows.", numeric = true, last = true) { v -> vm.edit { it.copy(following = v) } }
        }

        Section("Behaviour you noticed (optional)") {
            Text("Tick anything this profile did. Your answers can raise the assessment, never lower it.", style = MaterialTheme.typography.bodyMedium)
            FormState.FLAG_CATALOG.forEach { (id, label) ->
                CheckRow(label, id in f.flags, !busy) { on -> vm.edit { it.copy(flags = if (on) it.flags + id else it.flags - id) } }
            }
        }

        Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (submit) {
                SubmitState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Assessing…")
                    }
                    OutlinedButton(onClick = vm::cancel, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Cancel") }
                }
                is SubmitState.Failed -> {
                    SurfaceCard {
                        Text("Assessment not completed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                        Text(submit.message, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = vm::submit, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (submit.retryable) "Try again" else "Assess") }
                }
                SubmitState.Idle -> Button(onClick = vm::submit, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Assess profile") }
            }
        }
    }
}
