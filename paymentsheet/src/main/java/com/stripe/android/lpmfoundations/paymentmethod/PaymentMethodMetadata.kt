package com.stripe.android.lpmfoundations.paymentmethod

import android.content.Context
import android.os.Parcelable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import kotlinx.parcelize.Parcelize

/**
 * The metadata we need to determine what payment methods are supported, as well as being able to display them.
 * The purpose of this is to be able to easily plumb this information into the locations it’s needed.
 */
@Parcelize
internal data class PaymentMethodMetadata(
    val stripeIntent: StripeIntent,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val paymentMethodOrder: List<String>,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val sharedDataSpecs: List<SharedDataSpec>,
    val hasCustomerConfiguration: Boolean,
    val financialConnectionsAvailable: Boolean = DefaultIsFinancialConnectionsAvailable(),
    val externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec>,
) : Parcelable {
    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }

    fun requiresMandate(paymentMethodCode: String): Boolean {
        return PaymentMethodRegistry.definitionsByCode[paymentMethodCode]?.requiresMandate(this) ?: false
    }

    fun supportedPaymentMethodDefinitions(): List<PaymentMethodDefinition> {
        return stripeIntent.paymentMethodTypes.mapNotNull {
            PaymentMethodRegistry.definitionsByCode[it]
        }.filter {
            it.isSupported(this)
        }.filterNot {
            stripeIntent.isLiveMode &&
                stripeIntent.unactivatedPaymentMethods.contains(it.type.code)
        }.filter { paymentMethodDefinition ->
            paymentMethodDefinition.uiDefinitionFactory().canBeDisplayedInUi(paymentMethodDefinition, sharedDataSpecs)
        }.run {
            if (paymentMethodOrder.isEmpty()) {
                // Optimization to early out if we don't have a client side order.
                this
            } else {
                val orderedPaymentMethodTypes = orderedPaymentMethodTypes().mapOrderToIndex()
                sortedBy { paymentMethodDefinition ->
                    orderedPaymentMethodTypes[paymentMethodDefinition.type.code]
                }
            }
        }
    }

    fun supportedSavedPaymentMethodTypes(): List<PaymentMethod.Type> {
        return supportedPaymentMethodDefinitions().filter { paymentMethodDefinition ->
            paymentMethodDefinition.supportedAsSavedPaymentMethod
        }.map {
            it.type
        }
    }

    fun supportedPaymentMethodForCode(
        code: String,
    ): SupportedPaymentMethod? {
        // TODO: obvi do this in a better way
        if (code.startsWith("external_")) {
            // TODO: this doesn't show address when you click on it, will need to fix that bc some payment configs
            // collect billing address
            return externalPaymentMethodSpecs.map { it.type to it.toSupportedPaymentMethod() }.toMap().get(code)
        }
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null
        return definition.uiDefinitionFactory().supportedPaymentMethod(definition, sharedDataSpecs)
    }

    fun sortedSupportedPaymentMethods(): List<SupportedPaymentMethod> {
        // TODO: audit and update other parts of this class
        return supportedPaymentMethodDefinitions().mapNotNull { definition ->
            definition.uiDefinitionFactory().supportedPaymentMethod(definition, sharedDataSpecs)
        }.plus(externalPaymentMethodSpecs.map { it.toSupportedPaymentMethod() })
    }

    private fun orderedPaymentMethodTypes(): List<String> {
        val originalOrderedTypes = stripeIntent.paymentMethodTypes.toMutableList()
        val result = mutableListOf<String>()
        // 1. Add each PM in paymentMethodOrder first
        for (pm in paymentMethodOrder) {
            // Ignore the PM if it's not in originalOrderedTypes
            if (originalOrderedTypes.contains(pm)) {
                result += pm
                // 2. Remove each PM we add from originalOrderedTypes.
                originalOrderedTypes.remove(pm)
            }
        }
        // 3. Append the remaining PMs in originalOrderedTypes
        result.addAll(originalOrderedTypes)
        return result
    }

    private fun List<String>.mapOrderToIndex(): Map<String, Int> {
        return mapIndexed { index, s ->
            s to index
        }.toMap()
    }

    fun amount(): Amount? {
        if (stripeIntent is PaymentIntent) {
            return Amount(
                requireNotNull(stripeIntent.amount),
                requireNotNull(stripeIntent.currency)
            )
        }
        return null
    }

    fun formElementsForCode(
        code: String,
        uiDefinitionFactoryArgumentsFactory: UiDefinitionFactory.Arguments.Factory,
    ): List<FormElement>? {
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null

        return definition.uiDefinitionFactory().formElements(
            metadata = this,
            definition = definition,
            sharedDataSpecs = sharedDataSpecs,
            arguments = uiDefinitionFactoryArgumentsFactory.create(
                metadata = this,
                definition = definition,
            ),
        )
    }
}

// TODO: obviously do elsewhere, or increase vis of something needed for this
private class LabelResolvableString(val string : String) : ResolvableString {
    override fun resolve(context: Context): String {
        return string
    }
}

private fun ExternalPaymentMethodSpec.toSupportedPaymentMethod() : SupportedPaymentMethod {
    // TODO: return null if incorrect info? and then filter not nulls?
    return SupportedPaymentMethod(
        code = this.type,
        displayNameResource = LabelResolvableString(this.label),
        iconResource = 0,
        lightThemeIconUrl = this.lightImageUrl,
        darkThemeIconUrl = this.darkImageUrl,
        tintIconOnSelection = false
    )
}
