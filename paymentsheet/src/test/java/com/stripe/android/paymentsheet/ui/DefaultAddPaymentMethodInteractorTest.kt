package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DefaultAddPaymentMethodInteractorTest {

    @Test
    fun handleViewAction_OnLinkSignUpStateUpdated_updatesLinkState() {
        var updatedLinkSignupState: InlineSignupViewState? = null
        fun onLinkSignupStateUpdated(inlineSignupViewState: InlineSignupViewState) {
            updatedLinkSignupState = inlineSignupViewState
        }
        val inlineSignUpState = InlineSignupViewState(
            userInput = null,
            merchantName = "Example merchant",
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            fields = emptyList(),
            prefillEligibleFields = emptySet(),
        )

        runScenario(
            onLinkSignUpStateUpdated = ::onLinkSignupStateUpdated,
        ) {
            interactor.handleViewAction(AddPaymentMethodInteractor.ViewAction.OnLinkSignUpStateUpdated(
                inlineSignUpState
            ))

            assertThat(updatedLinkSignupState).isEqualTo(inlineSignUpState)
        }

    }

    @Test
    fun handleViewAction_ReportFieldInteraction_reportsFieldInteraction() {
        var latestCodeWithInteraction: PaymentMethodCode? = null
        fun reportFieldInteraction(code: PaymentMethodCode) {
            latestCodeWithInteraction = code
        }
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            reportFieldInteraction = ::reportFieldInteraction,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction(expectedCode)
            )

            assertThat(latestCodeWithInteraction).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnFormFieldValuesChanged_updatesFormFields() {
        var latestCodeWithChangedFormFields: PaymentMethodCode? = null
        fun onFormFieldValuesChanged(code: PaymentMethodCode) {
            latestCodeWithChangedFormFields = code
        }
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            onFormFieldValuesChanged = { _, code -> onFormFieldValuesChanged(code) },
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(null, expectedCode)
            )

            assertThat(latestCodeWithChangedFormFields).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnPaymentMethodSelected_selectsPaymentMethod() {
        var reportedSelectedPaymentMethodCode: PaymentMethodCode? = null
        fun reportPaymentMethodSelected(code: PaymentMethodCode) {
            reportedSelectedPaymentMethodCode = code
        }
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
            reportPaymentMethodTypeSelected = ::reportPaymentMethodSelected,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            assertThat(reportedSelectedPaymentMethodCode).isEqualTo(expectedCode)
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedCode)
                }
            }
        }
    }

    // TODO: failing because it's trying to call createUsBankAccount... to update state which is notImplemented()
    @Test
    fun handleViewAction_OnPaymentMethodSelected_withoutNewPaymentMethod_doesntReportSelection() {
        var reportedSelectedPaymentMethodCode: PaymentMethodCode? = null
        fun reportPaymentMethodSelected(code: PaymentMethodCode) {
            reportedSelectedPaymentMethodCode = code
        }
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            initiallySelectedPaymentMethodType = expectedCode,
            reportPaymentMethodTypeSelected = ::reportPaymentMethodSelected,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            assertThat(reportedSelectedPaymentMethodCode).isNull()
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedCode)
                }
            }
        }
    }


    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        initiallySelectedPaymentMethodType: PaymentMethodCode = PaymentMethod.Type.Card.code,
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        selection: StateFlow<PaymentSelection?> = MutableStateFlow(null),
        linkSignupMode: StateFlow<LinkSignupMode?> = MutableStateFlow(null),
        processing: StateFlow<Boolean> = MutableStateFlow(false),
        supportedPaymentMethods: List<SupportedPaymentMethod> = emptyList(),
        createFormArguments: (PaymentMethodCode) -> FormArguments = {
            FormArguments(
                initiallySelectedPaymentMethodType,
                cbcEligibility = CardBrandChoiceEligibility.create(
                    isEligible = true,
                    preferredNetworks = emptyList(),
                ),
                merchantName = "Example, Inc.",
            )
        },
        formElementsForCode: (PaymentMethodCode) -> List<FormElement> = { emptyList() },
        clearErrorMessages: () -> Unit = { notImplemented() },
        onLinkSignUpStateUpdated: (InlineSignupViewState) -> Unit = { notImplemented() },
        reportFieldInteraction: (PaymentMethodCode) -> Unit = { notImplemented() },
        onFormFieldValuesChanged: (FormFieldValues?, String) -> Unit = { _, _ -> notImplemented() },
        reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit = { notImplemented() },
        createUSBankAccountFormArguments: (PaymentMethodCode) -> USBankAccountFormArguments = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        val interactor = DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initiallySelectedPaymentMethodType,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            selection = selection,
            linkSignupMode = linkSignupMode,
            processing = processing,
            supportedPaymentMethods = supportedPaymentMethods,
            createFormArguments = createFormArguments,
            formElementsForCode = formElementsForCode,
            clearErrorMessages = clearErrorMessages,
            onLinkSignUpStateUpdated = onLinkSignUpStateUpdated,
            reportFieldInteraction = reportFieldInteraction,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = reportPaymentMethodTypeSelected,
            createUSBankAccountFormArguments = createUSBankAccountFormArguments,
            dispatcher = dispatcher,
        )

        TestParams(
            interactor = interactor,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val interactor: AddPaymentMethodInteractor,
    )
}
