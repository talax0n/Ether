package dev.ether.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Cream
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft
import dev.ether.app.ui.theme.MintSoft
import kotlinx.coroutines.delay

val LineWidth = 3.dp

/**
 * The Arcade container: fill, 3dp outline, and a solid offset "ledge" under it instead of a blur shadow.
 * Reserves [ledge] below itself so the ledge never overlaps a sibling. [depth] (px) pushes the surface
 * down onto its ledge; it is read in the draw phase, so pressing never recomposes.
 */
fun Modifier.ledge(
    shape: Shape,
    fill: Color,
    line: Color,
    ledgeColor: Color,
    ledge: Dp,
    depth: () -> Float = { 0f },
    lineWidth: Dp = LineWidth,
): Modifier = padding(bottom = ledge)
    .drawBehind {
        if (ledge > 0.dp) {
            val outline = shape.createOutline(size, layoutDirection, this)
            // Only the part peeking out below the surface, so see-through (ghost) fills stay see-through.
            clipPath(Path().apply { addOutline(outline) }, ClipOp.Difference) {
                translate(top = ledge.toPx()) { drawOutline(outline, ledgeColor) }
            }
        }
    }
    .graphicsLayer { translationY = depth() }
    .border(lineWidth, line, shape)
    .background(fill, shape)
    .clip(shape)

@Composable
fun Modifier.ledge(shape: Shape, fill: Color = Arcade.colors.surface, ledge: Dp = 5.dp): Modifier =
    ledge(shape, fill, Arcade.colors.line, Arcade.colors.ledge, ledge)

/** A ledge surface that sinks onto its ledge while pressed, then springs back. */
@Composable
fun Modifier.pressableLedge(
    shape: Shape,
    fill: Color,
    ledge: Dp,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    line: Color = Arcade.colors.line,
    ledgeColor: Color = Arcade.colors.ledge,
    role: Role = Role.Button,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val maxDepth = with(LocalDensity.current) { (ledge - 1.dp).coerceAtLeast(0.dp).toPx() }
    val depth by animateFloatAsState(if (pressed) maxDepth else 0f, if (pressed) Motion.press() else Motion.pop(), label = "press")
    val base = ledge(shape, fill, line, ledgeColor, ledge, depth = { depth })
    return if (onClick == null) base else base.clickable(interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun LedgeCard(
    modifier: Modifier = Modifier,
    color: Color = Arcade.colors.surface,
    shape: Shape = MaterialTheme.shapes.large,
    ledge: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.pressableLedge(shape, color, ledge, onClick), content = content)
}

@Composable
fun LedgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Coral,
    contentColor: Color = Ink,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    line: Color = Arcade.colors.line,
    ledgeColor: Color = Arcade.colors.ledge,
    height: Dp = 56.dp,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val fill = if (enabled) color else Arcade.colors.track
    val ink = if (enabled) contentColor else Arcade.colors.muted
    Box(
        modifier
            .heightIn(min = height + 5.dp)
            .pressableLedge(shape, fill, if (enabled) 5.dp else 2.dp, onClick, enabled && !busy, line, ledgeColor),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp).heightIn(min = height - 6.dp).graphicsLayer { alpha = if (busy) 0f else 1f },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon?.let { Icon(it, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp)) }
            Text(text, color = ink, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (busy) BouncingDots(ink)
    }
}

@Composable
fun LedgeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    color: Color = Arcade.colors.surface,
    tint: Color = Arcade.colors.text,
    shape: Shape = RoundedCornerShape(16.dp),
    ledge: Dp = 3.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier.size(size + ledge).pressableLedge(shape, color, ledge, onClick, enabled).semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Plain 48dp icon target for top bars, where a ledge would be too loud. */
@Composable
fun BarIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, Motion.pop(), label = "bar-press")
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Arcade.colors.text, modifier = Modifier.size(24.dp)) }
}

/** Pill sticker. Fills are pastel or white, so the text is always Ink. */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = Paper, icon: ImageVector? = null, contentColor: Color = Ink, numeric: Boolean = false) {
    Row(
        modifier.height(30.dp).border(2.dp, Arcade.colors.line, CircleShape).background(color, CircleShape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp)) }
        Text(text, color = contentColor, style = if (numeric) NumberStyle.copy(fontSize = 12.5.sp) else MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(tags: List<String>, color: Color, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxLines = maxLines) {
        tags.forEach { Chip(it, color = color) }
    }
}

fun initials(name: String) = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }

@Composable
fun Avatar(
    name: String,
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = Peach,
    ledge: Dp = 4.dp,
    lineWidth: Dp = LineWidth,
    label: String = initials(name),
) {
    Box(
        modifier.ledge(CircleShape, color, Arcade.colors.line, Arcade.colors.ledge, ledge, lineWidth = lineWidth).size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(label, color = Ink, style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * (if (label.length > 2) 0.24f else 0.36f)).sp))
        }
    }
}

/** Title row for pushed screens: back, title + subtitle, trailing actions. */
@Composable
fun ArcadeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(start = if (onBack == null) 20.dp else 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        onBack?.let { BarIconButton(ArcadeIcons.Back, "Back", it) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Arcade.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        actions()
    }
}

/** Big title for the four top-level tabs. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 18.dp, top = 10.dp, bottom = 6.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Arcade.colors.text, modifier = Modifier.weight(1f).semantics { heading() })
        action()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = Arcade.colors.text, modifier = Modifier.weight(1f).semantics { heading() })
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted) }
    }
}

/** Small caps label above a group of chips. */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = Arcade.colors.muted) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

/** Material TextField (so its label stays the tap target) dressed as an outlined ledge card. */
@Composable
fun ArcadeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    numeric: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = Arcade.colors
    val shape = MaterialTheme.shapes.medium
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        textStyle = if (numeric) NumberStyle.copy(fontSize = 22.sp, letterSpacing = 6.sp, color = colors.text) else MaterialTheme.typography.bodyLarge.copy(color = colors.text),
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            errorContainerColor = colors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLabelColor = colors.text,
            unfocusedLabelColor = colors.muted,
            cursorColor = Coral,
        ),
        modifier = modifier.fillMaxWidth().ledge(shape, colors.surface, if (focused) Coral else colors.line, colors.ledge, if (focused) 5.dp else 3.dp),
    )
}

@Composable
fun ArcadeSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            data,
            modifier = Modifier.padding(12.dp).ledge(RoundedCornerShape(18.dp), Ink, Ink, Coral, 4.dp),
            shape = RoundedCornerShape(18.dp),
            containerColor = Ink,
            contentColor = Cream,
        )
    }
}

/** Error line with a small sticker so it reads as feedback, not decoration. */
@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier, color = Arcade.colors.danger, style = MaterialTheme.typography.bodyMedium)
}

/** Fades and springs an item up into place, [index] steps after the first. Plays once per composition. */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val skip = Arcade.reducedMotion || index > 8
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    val rise = with(LocalDensity.current) { 36.dp.toPx() }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index * Motion.STAGGER_MS)
            progress.animateTo(1f, Motion.settle())
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * rise
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}

/** A row of [total] team seats: [filled] short labels (initials), then dashed empties. */
@Composable
fun Seats(filled: List<String>, total: Int, modifier: Modifier = Modifier) {
    val fills = listOf(Peach, SkySoft, SunSoft, MintSoft)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total.coerceAtMost(8)) { i ->
            val name = filled.getOrNull(i)
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.size(38.dp).then(
                    if (name != null) Modifier.border(LineWidth, Arcade.colors.line, shape).background(fills[i % fills.size], shape)
                    else Modifier.dashedBorder(Arcade.colors.muted, shape)
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(name ?: "+", style = NumberStyle.copy(fontSize = 12.sp), color = if (name != null) Ink else Arcade.colors.muted, textAlign = TextAlign.Center)
            }
        }
    }
}

fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = 2.5.dp): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline, color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        ),
    )
}
