package com.stripe.android.financialconnections.domain

import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import javax.inject.Inject

internal class LookupConsumerAndStartVerification @Inject constructor(
    private val lookupAccount: LookupAccount,
    private val startVerification: StartVerification,
) {

    sealed interface Result {
        data class Success(val consumerSession: ConsumerSession) : Result
        data object ConsumerNotFound : Result
        data class LookupError(val error: Throwable) : Result
        data class VerificationError(val error: Throwable) : Result
    }

    suspend operator fun invoke(
        email: String,
        businessName: String?,
        verificationType: VerificationType,
    ): Result {
        return runCatching {
            lookupAccount(email)
        }.fold(
            onSuccess = { session ->
                if (session.exists) {
                    runCatching {
                        val consumerSecret = session.consumerSession!!.clientSecret
                        when (verificationType) {
                            VerificationType.EMAIL -> startVerification.email(
                                consumerSessionClientSecret = consumerSecret,
                                businessName = businessName
                            )
                            VerificationType.SMS -> startVerification.sms(
                                consumerSessionClientSecret = consumerSecret
                            )
                        }
                    }.fold(
                        onSuccess = { Result.Success(it) },
                        onFailure = { Result.VerificationError(it) }
                    )
                } else {
                    Result.ConsumerNotFound
                }
            },
            onFailure = {
                Result.LookupError(it)
            },
        )
    }

    /**
     * Looks up a consumer account and starts verification.
     *
     * If the consumer account exists, starts verification.
     * If the consumer account does not exist, calls [onConsumerNotFound].
     * If there is an error looking up the consumer account, calls [onLookupError].
     * If there is an error starting verification, calls [onStartVerificationError].
     * If verification is started successfully, calls [onVerificationStarted].
     */
    @Deprecated("Don't use this!")
    suspend operator fun invoke(
        email: String,
        businessName: String?,
        verificationType: VerificationType,
        onConsumerNotFound: suspend () -> Unit,
        onLookupError: suspend (Throwable) -> Unit,
        onVerificationStarted: suspend (ConsumerSession) -> Unit,
        onStartVerificationError: suspend (Throwable) -> Unit
    ) {
        runCatching { lookupAccount(email) }
            .onSuccess { session ->
                if (session.exists) {
                    runCatching {
                        val consumerSecret = session.consumerSession!!.clientSecret
                        when (verificationType) {
                            VerificationType.EMAIL -> startVerification.email(
                                consumerSessionClientSecret = consumerSecret,
                                businessName = businessName
                            )
                            VerificationType.SMS -> startVerification.sms(
                                consumerSessionClientSecret = consumerSecret
                            )
                        }
                    }
                        .onSuccess { onVerificationStarted(it) }
                        .onFailure { onStartVerificationError(it) }
                } else {
                    onConsumerNotFound()
                }
            }.onFailure { onLookupError(it) }
    }
}
