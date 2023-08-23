package com.stripe.android.core.networking

import android.content.Context
import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestExecutor(
    private val context: Context,
    private val stripeNetworkClient: StripeNetworkClient,
    private val workContext: CoroutineContext,
) : AnalyticsRequestExecutor {

    private val canUseWorkManager: Boolean by lazy {
        val workManagerInClasspath = runCatching {
            Class.forName("androidx.work.WorkManager")
        }.isSuccess

        workManagerInClasspath && WorkManager.isInitialized()
    }

    @Inject
    constructor(
        context: Context,
    ) : this(
        context = context.applicationContext,
        stripeNetworkClient = DefaultStripeNetworkClient(
            workContext = Dispatchers.IO,
            logger = Logger.getInstance(BuildConfig.DEBUG),
        ),
        workContext = Dispatchers.IO,
    )

    constructor(
        context: Context,
        workContext: CoroutineContext,
    ) : this(
        context = context.applicationContext,
        stripeNetworkClient = DefaultStripeNetworkClient(
            workContext = workContext,
            logger = Logger.getInstance(BuildConfig.DEBUG),
        ),
        workContext = workContext,
    )

    override fun executeAsync(request: AnalyticsRequest) {
        if (canUseWorkManager) {
            val workManager = WorkManager.getInstance(context)
            enqueue(workManager, request)
        } else {
            execute(request)
        }
    }

    private fun execute(request: AnalyticsRequest) {
        val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
        val eventName = request.params[FIELD_EVENT]
        logger.info("Event: $eventName")

        CoroutineScope(workContext).launch {
            runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onSuccess {
                logger.info("Successfully sent event $eventName")
            }.onFailure {
                logger.error("Exception while sending event $eventName", it)
            }
        }
    }

    private fun enqueue(
        workManager: WorkManager,
        request: AnalyticsRequest
    ) {
        val inputData = SendAnalyticsEventWorker.createInputData(request)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SendAnalyticsEventWorker>()
            .addTag(makeWorkerTag())
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(workRequest)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class SendAnalyticsEventWorker(
        appContext: Context,
        params: WorkerParameters,
    ) : CoroutineWorker(appContext, params) {

        override suspend fun doWork(): Result {
            val request = inputData.getSerializable<AnalyticsRequest>(FIELD_DATA)
                ?: return Result.failure()

            val timestamp = inputData.getLong(FIELD_TIMESTAMP, 0)
            val delta = (SystemClock.elapsedRealtime() - timestamp).milliseconds

            if (delta > 10.minutes) {
                return Result.failure()
            }

            val stripeNetworkClient = WorkManagerHelpers.getOrCreateNetworkClient()
            val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

            logger.info("Event: ${request.params[FIELD_EVENT]}")

            return runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }.fold(
                onSuccess = { Result.success() },
                onFailure = {
                    // We can't retry due to missing idempotency support on q.stripe.com
                    Result.failure()
                },
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {

            fun createInputData(request: AnalyticsRequest): Data {
                return Data.Builder()
                    .putSerializable(FIELD_DATA, request)
                    .putSerializable(FIELD_TIMESTAMP, SystemClock.elapsedRealtime())
                    .build()
            }
        }
    }

    internal companion object {
        const val FIELD_DATA = "data"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_EVENT = "event"

        fun makeWorkerTag(): String {
            return SendAnalyticsEventWorker::class.java.name
        }
    }
}
