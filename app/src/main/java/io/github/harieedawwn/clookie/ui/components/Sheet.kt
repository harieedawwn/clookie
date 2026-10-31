package io.github.harieedawwn.clookie.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Bottom sheet: solid scrim, flat surface with a 2 dp outline, drag down or tap outside to close.
 * Place it last inside a full-screen Box.
 */
@Composable
fun BoxScope.ClookieSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Clookie.colors
    val motion = Clookie.motion
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { 120.dp.toPx() }
    BackHandler(visible) { onDismiss() }

    AnimatedVisibility(visible, Modifier.matchParentSize(), enter = fadeIn(motion.standard()), exit = fadeOut(motion.standard())) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible,
        Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(motion.offsetSpec()) { it },
        exit = slideOutVertically(motion.offsetSpec()) { it },
    ) {
        val radius = Clookie.shapes.largeRadius
        val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, drag.value.roundToInt()) }
                .clip(shape)
                .background(c.surface)
                .border(Stroke.regular, c.outline, shape)
                .draggable(
                    rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) } },
                    Orientation.Vertical,
                    onDragStopped = {
                        if (drag.value > dismissPx) {
                            onDismiss()
                            drag.snapTo(0f)
                        } else {
                            drag.animateTo(0f, motion.standard())
                        }
                    },
                )
                .navigationBarsPadding()
                .padding(horizontal = Space.gutter, vertical = Space.s3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(c.inkMuted))
            Spacer(Modifier.height(Space.s4))
            content()
        }
    }
}
