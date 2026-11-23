package dev.ether.app.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.ether.app.domain.HackathonResult
import dev.ether.app.domain.PastHackathon
import dev.ether.app.domain.Project
import dev.ether.app.domain.TrackRecord
import dev.ether.app.domain.TrackRecordRules
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.TagRow
import dev.ether.app.ui.components.color
import dev.ether.app.ui.components.label
import dev.ether.app.ui.components.hackathonCountLabel
import dev.ether.app.ui.components.projectCountLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.SkySoft

/** "Track record" on the user's own profile: past hackathons and projects, each editable. */
@Composable
fun TrackRecordSection(
    record: TrackRecord,
    onAddHackathon: () -> Unit,
    onEditHackathon: (PastHackathon) -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Arcade.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            "Track record",
            trailing = "${hackathonCountLabel(record.hackathons.size)} · ${projectCountLabel(record.projects.size)}",
        )
        if (record.hackathons.isEmpty() && record.projects.isEmpty()) {
            Text(
                "Add past hackathons and projects. Teammates see your best result and project count on your card.",
                style = MaterialTheme.typography.bodyMedium, color = colors.muted,
            )
        }

        CapsLabel("Hackathons", Modifier.padding(top = 4.dp))
        record.hackathons.forEach { HackathonItem(it) { onEditHackathon(it) } }
        AddButton("Add hackathon", record.canAddHackathon, onAddHackathon)

        CapsLabel("Projects", Modifier.padding(top = 8.dp))
        record.projects.forEach { ProjectItem(it) { onEditProject(it) } }
        AddButton("Add project", record.canAddProject, onAddProject)
    }
}

@Composable
private fun AddButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    LedgeButton(
        if (enabled) text else "Limit of ${TrackRecordRules.MAX_ENTRIES} reached", onClick, Modifier.fillMaxWidth(),
        color = Color.Transparent, contentColor = Arcade.colors.text, icon = ArcadeIcons.Plus, enabled = enabled, height = 48.dp,
    )
}

@Composable
private fun HackathonItem(h: PastHackathon, onEdit: () -> Unit) {
    val colors = Arcade.colors
    val context = LocalContext.current
    LedgeCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, ledge = 4.dp) {
        Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        h.name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium,
                        color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(h.year.toString(), style = NumberStyle, color = colors.muted)
                }
                Chip(h.result.label, color = h.result.color, icon = ArcadeIcons.Trophy)
                h.projectName?.let {
                    Text("Built $it", style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            h.url?.let { url -> LedgeIconButton(ArcadeIcons.Link, "Open link for ${h.name}", { openContact(context, url) }, size = 40.dp) }
            Spacer(Modifier.width(8.dp))
            LedgeIconButton(ArcadeIcons.Edit, "Edit ${h.name}", onEdit, size = 40.dp)
        }
    }
}

@Composable
private fun ProjectItem(p: Project, onEdit: () -> Unit) {
    val colors = Arcade.colors
    val context = LocalContext.current
    LedgeCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, ledge = 4.dp) {
        Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(p.title, style = MaterialTheme.typography.titleMedium, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.blurb, style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.tags.isNotEmpty()) TagRow(p.tags, SkySoft, maxLines = 1)
            }
            p.url?.let { url -> LedgeIconButton(ArcadeIcons.Link, "Open link for ${p.title}", { openContact(context, url) }, size = 40.dp) }
            Spacer(Modifier.width(8.dp))
            LedgeIconButton(ArcadeIcons.Edit, "Edit ${p.title}", onEdit, size = 40.dp)
        }
    }
}

/** Add or edit one item. Validation mirrors the database; its errors still show if a save is rejected. */
@Composable
fun TrackRecordEditorDialog(
    editor: TrackRecordEditor,
    currentYear: Int,
    canSave: Boolean,
    onHackathonChange: (HackathonForm) -> Unit,
    onProjectChange: (ProjectForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val reducedMotion = Arcade.reducedMotion
        val pop = remember { Animatable(if (reducedMotion) 1f else 0.85f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, Motion.pop()) }
        LedgeCard(Modifier.imePadding().graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val editing = when (editor) {
                    is TrackRecordEditor.Hackathon -> editor.editing != null
                    is TrackRecordEditor.Project -> editor.editing != null
                }
                val noun = if (editor is TrackRecordEditor.Hackathon) "hackathon" else "project"
                Text(
                    if (editing) "Edit $noun" else "Add a $noun",
                    style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text,
                )
                when (editor) {
                    is TrackRecordEditor.Hackathon -> HackathonFields(editor.form, currentYear, onHackathonChange)
                    is TrackRecordEditor.Project -> ProjectFields(editor.form, onProjectChange)
                }
                editor.error?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgeButton("Cancel", onDismiss, Modifier.weight(1f), color = Color.Transparent, contentColor = Arcade.colors.text, enabled = !editor.saving)
                    LedgeButton("Save", onSave, Modifier.weight(1f), enabled = canSave, busy = editor.saving)
                }
                if (editing) {
                    LedgeButton(
                        if (editor.confirmingDelete) "Tap again to delete" else "Delete $noun",
                        onDelete, Modifier.fillMaxWidth(),
                        color = if (editor.confirmingDelete) CoralSoft else Color.Transparent,
                        contentColor = Arcade.colors.danger, icon = ArcadeIcons.Close, enabled = !editor.saving, height = 48.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HackathonFields(form: HackathonForm, currentYear: Int, onChange: (HackathonForm) -> Unit) {
    val muted = Arcade.colors.muted
    ArcadeTextField(form.name, { onChange(form.copy(name = it)) }, "Hackathon name", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
    if (form.nameInvalid) ErrorText("At most ${TrackRecordRules.NAME_MAX} characters.")
    ArcadeTextField(
        form.year, { v -> onChange(form.copy(year = v.filter(Char::isDigit).take(4))) }, "Year",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
    if (form.yearInvalid(currentYear)) ErrorText("Pick a year from ${TrackRecordRules.MIN_YEAR} to ${TrackRecordRules.maxYear(currentYear)}.")
    CapsLabel("Result")
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Best first, the way people tell it.
        HackathonResult.entries.reversed().forEach { r ->
            val selected = form.result == r
            Chip(
                r.label,
                Modifier.clip(CircleShape).selectable(selected, role = Role.RadioButton) { onChange(form.copy(result = r)) },
                // Participant's own color is white, which wouldn't read as selected.
                color = if (!selected) Arcade.colors.surface else if (r == HackathonResult.PARTICIPANT) CoralSoft else r.color,
                icon = if (selected) ArcadeIcons.Check else null,
                contentColor = if (selected) Ink else Arcade.colors.text,
            )
        }
    }
    ArcadeTextField(form.projectName, { onChange(form.copy(projectName = it)) }, "Project name (optional)", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
    if (form.projectNameInvalid) ErrorText("At most ${TrackRecordRules.NAME_MAX} characters.")
    ArcadeTextField(
        form.url, { onChange(form.copy(url = it.trim())) }, "Link (optional)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
    )
    if (form.urlInvalid) ErrorText("Use an https:// link.")
    else if (form.url.isBlank()) Text("Devpost, GitHub, or a demo. https only.", style = MaterialTheme.typography.bodySmall, color = muted)
}

@Composable
private fun ProjectFields(form: ProjectForm, onChange: (ProjectForm) -> Unit) {
    val muted = Arcade.colors.muted
    ArcadeTextField(form.title, { onChange(form.copy(title = it)) }, "Title", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
    if (form.titleInvalid) ErrorText("At most ${TrackRecordRules.NAME_MAX} characters.")
    ArcadeTextField(
        form.blurb, { onChange(form.copy(blurb = it.replace('\n', ' '))) }, "One-line description",
        singleLine = false, minLines = 2, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
    val length = form.blurb.trim().length
    Text(
        "$length / ${TrackRecordRules.BLURB_MAX}",
        style = NumberStyle, color = if (form.blurbInvalid) Arcade.colors.danger else muted,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
    )
    ArcadeTextField(
        form.url, { onChange(form.copy(url = it.trim())) }, "Link (optional)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    )
    if (form.urlInvalid) ErrorText("Use an https:// link.")
    ArcadeTextField(form.tags, { onChange(form.copy(tags = it)) }, "Tech, comma separated (optional)", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
    val tags = form.tagList
    if (tags.isNotEmpty()) TagRow(tags, SkySoft)
    when {
        tags.size > TrackRecordRules.TAGS_MAX -> ErrorText("Up to ${TrackRecordRules.TAGS_MAX} tags.")
        form.tagsInvalid -> ErrorText("Each tag is at most ${TrackRecordRules.TAG_MAX} characters.")
    }
}
