package com.stripe.android.paymentsheet.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PaymentOptionsItemsMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val customerStateFlow = MutableStateFlow<CustomerState?>(null)
    private val isGooglePayReadyFlow = MutableStateFlow(false)
    private val isLinkEnabledFlow = MutableStateFlow<Boolean?>(null)

    @Test
    fun `Only emits value if required flows have emitted values`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
            nameProvider = { it!!.resolvableString },
            allowsRemovalOfLastSavedPaymentMethod = true,
            isCbcEligible = { false }
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())

            customerStateFlow.value = createCustomerState(
                paymentMethods = PaymentMethodFixtures.createCards(2)
            )
            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            val state = awaitItem()
            assertThat(state).hasSize(5)
            assertThat(state[0].viewType).isEqualTo(PaymentOptionsItem.ViewType.AddCard)
            assertThat(state[1].viewType).isEqualTo(PaymentOptionsItem.ViewType.GooglePay)
            assertThat(state[2].viewType).isEqualTo(PaymentOptionsItem.ViewType.Link)
            assertThat(state[3].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
            assertThat(state[4].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
        }
    }

    @Test
    fun `Doesn't include Google Pay and Link in payment flow`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!!.resolvableString },
            allowsRemovalOfLastSavedPaymentMethod = true,
            isCbcEligible = { false }
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())

            customerStateFlow.value = createCustomerState(
                paymentMethods = PaymentMethodFixtures.createCards(2)
            )
            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            assertThat(awaitItem()).containsNoneOf(
                PaymentOptionsItem.GooglePay,
                PaymentOptionsItem.Link,
            )
        }
    }

    @Test
    fun `If 1 payment method with 'allowsRemovalOfLastSavedPaymentMethod' enabled, should be removable`() =
        runTest {
            testRemove(
                paymentMethods = PaymentMethodFactory.cards(size = 1),
                canRemovePaymentMethods = true,
                allowsRemovalOfLastSavedPaymentMethod = true,
                expected = true
            )
        }

    @Test
    fun `If 1 payment method with 'allowsRemovalOfLastSavedPaymentMethod' disabled, should not be removable`() =
        runTest {
            testRemove(
                paymentMethods = PaymentMethodFactory.cards(size = 1),
                canRemovePaymentMethods = true,
                allowsRemovalOfLastSavedPaymentMethod = false,
                expected = false
            )
        }

    @Test
    fun `If 1 payment method with 'canRemovePaymentMethods' disabled, should not be removable`() = runTest {
        testRemove(
            paymentMethods = PaymentMethodFactory.cards(size = 1),
            canRemovePaymentMethods = false,
            allowsRemovalOfLastSavedPaymentMethod = true,
            expected = false
        )
    }

    @Test
    fun `If multiple payment method with 'canRemovePaymentMethods' enabled, all should be removable`() =
        runTest {
            testRemove(
                paymentMethods = PaymentMethodFactory.cards(size = 2),
                canRemovePaymentMethods = true,
                allowsRemovalOfLastSavedPaymentMethod = false,
                expected = true
            )
        }

    @Test
    fun `If multiple payment method with 'canRemovePaymentMethods' disabled, none should not be removable`() =
        runTest {
            testRemove(
                paymentMethods = PaymentMethodFactory.cards(size = 2),
                canRemovePaymentMethods = false,
                allowsRemovalOfLastSavedPaymentMethod = true,
                expected = false
            )
        }

    private suspend fun testRemove(
        paymentMethods: List<PaymentMethod>,
        canRemovePaymentMethods: Boolean,
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
        expected: Boolean,
    ) {
        val mapper = createPaymentOptionsItemsMapper(
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())

            customerStateFlow.value = createCustomerState(
                paymentMethods = paymentMethods,
                canRemovePaymentMethods = canRemovePaymentMethods,
            )

            isLinkEnabledFlow.value = true

            val items = awaitItem().filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

            val result = items.all { paymentMethod ->
                paymentMethod.isRemovable
            }

            assertThat(result).isEqualTo(expected)
        }
    }

    private fun createPaymentOptionsItemsMapper(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): PaymentOptionsItemsMapper {
        return PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!!.resolvableString },
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            isCbcEligible = { false }
        )
    }

    private fun createCustomerState(
        paymentMethods: List<PaymentMethod> = emptyList(),
        canRemovePaymentMethods: Boolean = true,
    ): CustomerState {
        return CustomerState(
            id = "pi_123",
            ephemeralKeySecret = "ek_123",
            paymentMethods = paymentMethods,
            permissions = CustomerState.Permissions(
                canRemovePaymentMethods = canRemovePaymentMethods,
                canRemoveDuplicates = false,
            )
        )
    }
}
