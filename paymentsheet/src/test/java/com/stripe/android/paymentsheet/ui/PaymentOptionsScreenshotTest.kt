package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class PaymentOptionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testWidthLessThanScreen() {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.Link,
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testWidthMoreThanScreen() {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4242"),
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4000"),
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("1234"),
                            )
                        ),
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testWidthMoreThanScreenAndScrollToEnd() {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4242"),
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4000"),
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("1234"),
                            )
                        ),
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
                scrollState = LazyListState(firstVisibleItemIndex = 2),
            )
        }
    }

    @Test
    fun testItemsNotRemovableWhileEditing() {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4242"),
                                isRemovable = false,
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("4000").run {
                                    copy(
                                        card = card?.copy(
                                            networks = PaymentMethod.Card.Networks(
                                                available = setOf("visa", "cartes_bancaires")
                                            )
                                        )
                                    )
                                },
                                isRemovable = false,
                                isCbcEligible = true,
                            )
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            DisplayableSavedPaymentMethod(
                                displayName = "Card",
                                paymentMethod = createCard("1234"),
                                isRemovable = false,
                            )
                        ),
                    ),
                    selectedIndex = 1,
                ),
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
    }

    private fun createCard(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            card = original.card?.copy(last4 = last4),
        )
    }
}
