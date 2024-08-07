package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.launcher.InstantDebitsResult
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerPaymentDetails.BankAccount
import javax.inject.Inject

internal interface CreateInstantDebitsResult {
    suspend operator fun invoke(
        bankAccountId: String,
    ): InstantDebitsResult
}

internal class RealCreateInstantDebitsResult @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val consumerSessionProvider: ConsumerSessionProvider,
) : CreateInstantDebitsResult {

    override suspend fun invoke(
        bankAccountId: String,
    ): InstantDebitsResult {
        val consumerSession = consumerSessionProvider.provideConsumerSession()

        val response = consumerRepository.createPaymentDetails(
            consumerSessionClientSecret = consumerSession!!.clientSecret,
            bankAccountId = bankAccountId,
        )

        val paymentDetails = response.paymentDetails.filterIsInstance<BankAccount>().first()

        // TODO(tillh-stripe) Create the PaymentMethod from the PaymentDetails

        return InstantDebitsResult(
            paymentMethodId = "pm_123", // TODO(tillh-stripe) Replace with actual PaymentMethod ID
            bankName = paymentDetails.bankName,
            last4 = paymentDetails.last4,
        )
    }
}
