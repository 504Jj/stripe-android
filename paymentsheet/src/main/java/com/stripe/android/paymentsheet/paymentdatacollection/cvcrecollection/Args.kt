package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.os.Parcelable
import com.stripe.android.model.CardBrand
import kotlinx.parcelize.Parcelize

data class Args(
    val lastFour: String,
    val cardBrand: CardBrand,
    val cvc: String? = null,
    val displayMode: DisplayMode
) {
    sealed interface DisplayMode: Parcelable {
        val isLiveMode: Boolean

        @Parcelize
        data class Activity(override val isLiveMode: Boolean) : DisplayMode

        @Parcelize
        data class PaymentScreen(override val isLiveMode: Boolean) : DisplayMode
    }
}


