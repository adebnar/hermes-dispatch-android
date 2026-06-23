package co.hermesdispatch.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Shared push/pop transitions for forward navigation (e.g. task → chat). A short
 * horizontal slide + fade so navigation feels responsive without being slow.
 * Applied per-destination in [AppNav]; bottom-nav tab switches keep the default
 * (no slide) so lateral tab changes don't look like a drill-down.
 */
private const val DURATION = 280

fun enterPush(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(DURATION)) { it / 4 } + fadeIn(tween(DURATION))
}

fun exitPush(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(DURATION))
}

fun enterPop(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(DURATION))
}

fun exitPop(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(DURATION)) { it / 4 } + fadeOut(tween(DURATION))
}
