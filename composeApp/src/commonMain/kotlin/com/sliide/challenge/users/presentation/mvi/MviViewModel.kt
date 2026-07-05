package com.sliide.challenge.users.presentation.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Minimal hand-rolled MVI base — deliberately not a framework dependency.
 *
 * Contract per feature:
 *  - STATE: single immutable object, the only thing the UI renders.
 *  - INTENT: sealed user/system actions, the only way in.
 *  - EFFECT: one-shot events (snackbars, navigation) that must not be
 *    replayed on recomposition, delivered via a Channel.
 */
abstract class MviViewModel<STATE, INTENT, EFFECT>(initialState: STATE) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effects = Channel<EFFECT>(Channel.BUFFERED)
    val effects: Flow<EFFECT> = _effects.receiveAsFlow()

    protected val currentState: STATE get() = _state.value

    protected fun setState(reduce: STATE.() -> STATE) = _state.update(reduce)

    protected fun sendEffect(effect: EFFECT) {
        _effects.trySend(effect)
    }

    abstract fun onIntent(intent: INTENT)
}
