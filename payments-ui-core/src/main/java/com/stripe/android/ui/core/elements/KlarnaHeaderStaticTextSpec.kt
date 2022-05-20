package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is for elements that do not receive user input
 */
@Serializable
@SerialName("static_text")
internal data class KlarnaHeaderStaticTextSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.api_path,
            stringResId = KlarnaHelper.getKlarnaHeader(),
        )
}
