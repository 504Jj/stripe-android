package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.InitialAddPaymentMethodState
import com.stripe.android.lpmfoundations.PaymentMethodConfirmParams
import com.stripe.android.lpmfoundations.PrimaryButtonCustomizer
import com.stripe.android.lpmfoundations.UiState
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.R
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal object PayPalPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayPal

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = emptySet()

    override suspend fun initialAddState(
        metadata: PaymentMethodMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_paypal)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal
            }

            if (metadata.hasIntentToSetup()) {
                mandate(resolvableString(R.string.stripe_paypal_mandate, metadata.merchantName))
            }
        }

        // TODO(jaynewstrom): This is just an example, remove me!
        state(PayPalState(needsToShowMandate = true))
        primaryButtonCustomizer { state ->
            state[payPalKey].map { payPalState ->
                if (payPalState.needsToShowMandate) {
                    PrimaryButtonCustomizer.State(text = resolvableString("Continue"), enabled = true) {
                        // TODO(jaynewstrom): Show the dialog, when the dialog is acknowledged, set the needs to show
                        //  mandate to false.
                        state.update(payPalKey) {
                            copy(needsToShowMandate = false)
                        }
                    }
                } else {
                    null
                }
            }
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.createPayPal()
        )
    }
}

private val payPalKey = PaymentMethodDefinition.UiStateKey.create<PayPalState>(PayPalPaymentMethodDefinition)

@Parcelize
internal data class PayPalState(val needsToShowMandate: Boolean) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<out UiState.Value> = payPalKey
}
