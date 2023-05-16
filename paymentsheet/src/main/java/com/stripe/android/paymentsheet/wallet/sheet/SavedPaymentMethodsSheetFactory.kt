package com.stripe.android.paymentsheet.wallet.sheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.wallet.controller.SavedPaymentMethodsControllerResultCallback

internal class SavedPaymentMethodsSheetFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val customerAdapter: CustomerAdapter,
    private val callback: SavedPaymentMethodsSheetResultCallback,
) {
    constructor(
        activity: ComponentActivity,
        customerAdapter: CustomerAdapter,
        callback: SavedPaymentMethodsSheetResultCallback,
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultCaller = activity,
        statusBarColor = { activity.window.statusBarColor },
        customerAdapter = customerAdapter,
        callback = callback
    )

    fun create(): SavedPaymentMethodsSheet =
        DefaultSavedPaymentMethodsSheet.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            customerAdapter = customerAdapter,
            callback = callback,
        )
}