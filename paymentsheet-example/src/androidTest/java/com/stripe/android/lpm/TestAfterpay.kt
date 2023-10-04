package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultShippingAddressSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAfterpay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "afterpay_clearpay"
    ) { settings ->
        settings[CountrySettingsDefinition] = CountrySettingsDefinition.Country.US
        settings[CurrencySettingsDefinition] = CurrencySettingsDefinition.Currency.USD
        settings[DefaultShippingAddressSettingsDefinition] = true
    }

    @Test
    fun testAfterpay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testAfterpayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
