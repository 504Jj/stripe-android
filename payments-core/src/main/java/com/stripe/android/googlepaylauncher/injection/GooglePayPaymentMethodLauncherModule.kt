package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.DefaultPaymentsClientFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.PaymentsClientFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [GooglePayPaymentMethodLauncherViewModelSubcomponent::class]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class GooglePayPaymentMethodLauncherModule {
    @Binds
    @Singleton
    abstract fun bindsGooglePayRepository(
        defaultGooglePayRepository: DefaultGooglePayRepository
    ): GooglePayRepository

    @Binds
    @Singleton
    abstract fun bindsPaymentsClientFactory(
        defaultPaymentsClientFactory: DefaultPaymentsClientFactory
    ): PaymentsClientFactory

    companion object {
        @Provides
        @Singleton
        fun providePaymentsClient(
            googlePayConfig: GooglePayPaymentMethodLauncher.Config,
            paymentsClientFactory: PaymentsClientFactory
        ) = paymentsClientFactory.create(googlePayConfig.environment)

        @Provides
        @Singleton
        fun providesAnalyticsRequestFactory(
            context: Context,
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): AnalyticsRequestFactory {
            return PaymentAnalyticsRequestFactory(
                context = context,
                publishableKeyProvider = publishableKeyProvider,
            )
        }

        @Provides
        @Singleton
        fun provideErrorReporter(
            analyticsRequestFactory: AnalyticsRequestFactory,
            analyticsRequestExecutor: AnalyticsRequestExecutor
        ): ErrorReporter = RealErrorReporter(
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsRequestExecutor = analyticsRequestExecutor,
        )
    }
}
