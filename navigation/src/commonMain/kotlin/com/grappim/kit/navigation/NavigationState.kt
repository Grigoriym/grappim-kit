package com.grappim.kit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlin.reflect.KClass

/**
 * The dual back stack: [topLevelStack] records which section is active, and each section owns an
 * independent sub-stack in [subStacks], so switching sections keeps each one's history.
 *
 * [subStacks] is keyed by [NavKey]'s runtime class, not by instance equality. A top-level route
 * can carry a payload (e.g. a `ProjectSelectorNavDestination(isFromLogin: Boolean)`-shaped key),
 * and the instance navigated to at runtime is rarely `equals()` to whichever instance seeded this
 * map in [rememberNavigationState] — class identity is what "which section is this" actually
 * means here.
 */
class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<KClass<out NavKey>, NavBackStack<NavKey>>
) {
    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val topLevelKeyClasses: Set<KClass<out NavKey>>
        get() = subStacks.keys

    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelKey::class]
            ?: error("Sub stack for ${currentTopLevelKey::class} does not exist")

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }

    /**
     * Bumped by [Navigator.resetTo] and read by [toEntries]'s `key()` wrap. A payload-less
     * `data object` top-level key is `equals()`-identical before and after a reset, so writing it
     * back into a sub-stack's root slot is invisible to `ViewModelStoreNavEntryDecorator`'s own
     * structural diffing — it never sees the key "disappear", so it never disposes the
     * `ViewModelStore` created the first time that section was shown. This generation counter is
     * what actually forces disposal: bumping it forces `toEntries` to discard and recreate every
     * section's decorators (Compose's normal behavior for a changed `key()`), which frees every
     * `ViewModelStore` — including the ones sitting on unrelated inactive sections — regardless of
     * whether any individual key's identity changed. `navigate()`/`goToTopLevel()` never touch
     * this, so ordinary tab switching still preserves each section's state exactly as before —
     * only `resetTo`'s documented "forget everything" contract triggers it.
     */
    var resetGeneration: Int by mutableIntStateOf(0)
        internal set
}

/**
 * [configuration] is a parameter rather than something built here: it carries the polymorphic
 * `SerializersModule` listing every route the consuming app defines — this module can't build
 * that itself without depending on every feature module's route classes, which would invert the
 * dependency direction — so passing it in is what keeps this module free of route imports.
 */
@Composable
fun rememberNavigationState(
    startKey: NavKey,
    topLevelKeys: Set<NavKey>,
    configuration: SavedStateConfiguration
): NavigationState {
    val topLevelStack = rememberNavBackStack(configuration, startKey)
    val subStacks = topLevelKeys.associate { key -> key::class to rememberNavBackStack(configuration, key) }

    return remember(startKey, topLevelKeys) {
        NavigationState(
            startKey = startKey,
            topLevelStack = topLevelStack,
            subStacks = subStacks
        )
    }
}

/**
 * Flattens the dual back stack into the single list of decorated entries `NavDisplay` renders.
 * Every sub-stack is decorated on every composition — a decorator dropped and recreated loses its
 * state — and only then are the active sections' entries concatenated.
 *
 * The whole decoration step is wrapped in `key(resetGeneration)`: on an ordinary recomposition
 * (navigate/goBack/goToTopLevel) this key doesn't change, so Compose keeps reusing the same
 * decorator instances and every section's state survives exactly as documented above. Only
 * [Navigator.resetTo] bumps [NavigationState.resetGeneration], which makes Compose discard this
 * whole group and recreate it from scratch — freeing every section's `ViewModelStore` (and
 * saveable state) even for the payload-less singleton keys that a plain structural diff can never
 * see change. See [NavigationState.resetGeneration] for why that diff-based path doesn't work.
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = key(resetGeneration) {
        subStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator<NavKey>()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }
    }

    return topLevelStack
        .flatMap { decoratedEntries[it::class] ?: emptyList() }
        .toMutableStateList()
}
