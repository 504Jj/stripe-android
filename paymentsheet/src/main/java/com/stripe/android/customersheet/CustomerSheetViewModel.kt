package com.stripe.android.customersheet

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.injection.CustomerSessionScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
internal class CustomerSheetViewModel @Inject constructor(
    paymentConfiguration: PaymentConfiguration,
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
) : ViewModel() {

    private val _backstack = MutableStateFlow<List<CustomerSheetViewState>>(
        value = emptyList()
    )

    private val _viewState = MutableStateFlow<CustomerSheetViewState>(
        CustomerSheetViewState.Loading(
            isLiveMode = paymentConfiguration.publishableKey.contains("live")
        )
    )
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    init {
        loadPaymentMethods()
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEditPressed()
            is CustomerSheetViewAction.OnItemRemoved -> onItemRemoved(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
            is CustomerSheetViewAction.OnPrimaryButtonPressed -> onPrimaryButtonPressed()
        }
    }

    fun providePaymentMethodName(code: PaymentMethodCode?): String {
        val paymentMethod = lpmRepository.fromCode(code)
        return paymentMethod?.displayNameResource?.let {
            resources.getString(it)
        }.orEmpty()
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            var savedPaymentMethods: List<PaymentMethod> = emptyList()
            var paymentSelection: PaymentSelection? = null
            var errorMessage: String? = null
            customerAdapter.retrievePaymentMethods().fold(
                onSuccess = { paymentMethods ->
                    savedPaymentMethods = paymentMethods
                    customerAdapter.retrieveSelectedPaymentOption().onSuccess { paymentOption ->
                        paymentSelection = when (paymentOption) {
                            is CustomerAdapter.PaymentOption.GooglePay -> {
                                PaymentSelection.GooglePay
                            }
                            is CustomerAdapter.PaymentOption.Link -> {
                                PaymentSelection.Link
                            }
                            is CustomerAdapter.PaymentOption.StripeId -> {
                                paymentMethods.find { it.id == paymentOption.id }?.let {
                                    PaymentSelection.Saved(it)
                                }
                            }
                            else -> null
                        }
                    }
                },
                onFailure = { throwable ->
                    errorMessage = throwable.message
                }
            )
            _viewState.update {
                CustomerSheetViewState.SelectPaymentMethod(
                    title = configuration.headerTextForSelectionScreen,
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = paymentSelection,
                    showEditMenu = savedPaymentMethods.isNotEmpty(),
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = configuration.googlePayEnabled,
                    primaryButtonLabel = paymentSelection?.let {
                        resources.getString(
                            com.stripe.android.ui.core.R.string.stripe_continue_button_label
                        )
                    },
                    primaryButtonEnabled = paymentSelection != null,
                    errorMessage = errorMessage,
                )
            }
        }
    }

    private fun onAddCardPressed() {
        transition(
            from = viewState.value,
            to = CustomerSheetViewState.AddCard(
                cardNumber = ""
            )
        )
    }

    private fun onBackPressed() {
        popBackStack()
    }

    private fun onEditPressed() {
        TODO()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onItemRemoved(paymentMethod: PaymentMethod) {
        TODO()
    }

    private fun onItemSelected(paymentSelection: PaymentSelection?) {
        when (paymentSelection) {
            is PaymentSelection.GooglePay, is PaymentSelection.Saved -> {
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        paymentSelection = paymentSelection,
                        primaryButtonLabel = resources.getString(
                            com.stripe.android.ui.core.R.string.stripe_continue_button_label
                        ),
                        primaryButtonEnabled = true
                    )
                }
            }
            else -> {
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        paymentSelection = null,
                        primaryButtonLabel = null,
                        primaryButtonEnabled = false
                    )
                }
            }
        }
    }

    private fun onPrimaryButtonPressed() {
        TODO()
    }

    override fun onCleared() {
        _result.update {
            null
        }
        super.onCleared()
    }

    private inline fun <reified T : CustomerSheetViewState> updateViewState(block: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                block(it as T)
            }
        }
    }

    private fun transition(from: CustomerSheetViewState, to: CustomerSheetViewState) {
        _backstack.update {
            it + from
        }
        _viewState.update {
            to
        }
    }

    private fun popBackStack() {
        if (_backstack.value.isEmpty()) {
            _result.tryEmit(
                InternalCustomerSheetResult.Canceled
            )
        } else {
            val previous = _backstack.value.last()
            _backstack.update {
                it - previous
            }
            _viewState.update {
                previous
            }
        }
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSessionViewModel.component.customerSheetViewModel as T
        }
    }
}