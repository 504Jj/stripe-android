package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface CvcRecollectionInteractor {
    val viewState: StateFlow<CvcRecollectionViewState>
    val cvcCompletion: StateFlow<CVCRecollectionCompletion>

    fun handleViewAction(action: CvcRecollectionViewAction)
}

internal class DefaultCvcRecollectionInteractor(
    args: Args,
    private val onCompletionChanged: (CVCRecollectionCompletion) -> Unit,
) : CvcRecollectionInteractor {
    private val _isCvcComplete = MutableStateFlow<CVCRecollectionCompletion>(CVCRecollectionCompletion.Incomplete)
    override val cvcCompletion: StateFlow<CVCRecollectionCompletion>
        get() = _isCvcComplete

    private val _viewState = MutableStateFlow<CvcRecollectionViewState>(
        CvcRecollectionViewState(
            cardBrand = args.cardBrand,
            lastFour = args.lastFour,
            cvc = null,
            displayMode = args.displayMode
        )
    )
    override val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    override fun handleViewAction(action: CvcRecollectionViewAction) {
        when (action) {
            is CvcRecollectionViewAction.CVCCompletionChanged -> {
                onCompletionChanged(action.completion)
                _isCvcComplete.value = action.completion
            }
            CvcRecollectionViewAction.OnBackPressed -> Unit
            is CvcRecollectionViewAction.OnConfirmPressed -> Unit
        }
    }
}


