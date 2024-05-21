package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Stable
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodConfigurationSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings

@Stable
internal sealed interface PlaygroundState {
    val integrationType: PlaygroundConfigurationData.IntegrationType

    data class Payment(
        private val snapshot: PlaygroundSettings.Snapshot,
        val amount: Long,
        val paymentMethodTypes: List<String>,
        val customerConfig: PaymentSheet.CustomerConfiguration?,
        val clientSecret: String,
    ) : PlaygroundState {
        override val integrationType = snapshot.configurationData.integrationType

        val initializationType = snapshot[InitializationTypeSettingsDefinition]
        val currencyCode = snapshot[CurrencySettingsDefinition]
        val countryCode = snapshot[CountrySettingsDefinition]
        val checkoutMode = snapshot[CheckoutModeSettingsDefinition]
        val paymentMethodConfigurationId: String? =
            snapshot[PaymentMethodConfigurationSettingsDefinition].ifEmpty { null }

        val stripeIntentId: String
            get() = clientSecret.substringBefore("_secret_")

        fun paymentSheetConfiguration(): PaymentSheet.Configuration {
            return snapshot.paymentSheetConfiguration(this)
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    data class Customer(
        private val snapshot: PlaygroundSettings.Snapshot,
        val adapter: CustomerAdapter,
    ) : PlaygroundState {
        override val integrationType = snapshot.configurationData.integrationType

        fun customerSheetConfiguration(): CustomerSheet.Configuration {
            // TODO(samer-stripe): Implement building customer sheet configuration in playground state
            error("Not implemented!")
        }
    }

    companion object {
        fun CheckoutResponse.asPlaygroundState(
            snapshot: PlaygroundSettings.Snapshot,
        ): PlaygroundState {
            val paymentMethodTypes = if (snapshot[AutomaticPaymentMethodsSettingsDefinition]) {
                emptyList()
            } else {
                paymentMethodTypes
                    .orEmpty()
                    .split(",")
            }
            return Payment(
                snapshot = snapshot,
                amount = amount,
                paymentMethodTypes = paymentMethodTypes,
                customerConfig = makeCustomerConfig(snapshot.checkoutRequest().customerKeyType),
                clientSecret = intentClientSecret,
            )
        }
    }
}
