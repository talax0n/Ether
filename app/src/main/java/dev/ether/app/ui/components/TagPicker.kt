package dev.ether.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.ether.app.domain.addTag
import dev.ether.app.domain.tagSuggestions
import dev.ether.app.ui.theme.Arcade

/**
 * Pick tags from [catalog] by searching, or add your own: typing filters the dropdown, and Done or a comma
 * adds what's typed when nothing in the list fits. Picked tags show as chips; tapping one removes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    label: String,
    selected: List<String>,
    catalog: List<String>,
    color: Color,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(false) }
    val matches = tagSuggestions(query, catalog, selected)
    val custom = query.trim().takeIf { q -> q.isNotEmpty() && (catalog + selected).none { it.equals(q, ignoreCase = true) } }
    fun add(raw: String) {
        onChange(addTag(selected, raw, catalog))
        query = ""
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(open, { open = it }) {
            ArcadeTextField(
                value = query,
                onValueChange = {
                    // A comma ends a tag, like pressing Done.
                    if (it.endsWith(',')) add(it) else query = it
                    open = true
                },
                label = label,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add(query) }),
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            )
            if (matches.isNotEmpty() || custom != null) {
                ExposedDropdownMenu(open, { open = false }, containerColor = Arcade.colors.surface) {
                    custom?.let { DropdownMenuItem({ Text("Add \"$it\"") }, { add(it) }, leadingIcon = { Icon(ArcadeIcons.Plus, null) }) }
                    matches.forEach { tag -> DropdownMenuItem({ Text(tag) }, { add(tag) }) }
                }
            }
        }
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { tag ->
                    Chip(
                        tag,
                        Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Remove") { onChange(selected - tag) }
                            .semantics { contentDescription = "$tag, remove" },
                        color = color,
                        icon = ArcadeIcons.Close,
                    )
                }
            }
        }
    }
}
