package org.multipaz.wallet.android.ui.presentment

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.compose.presentment.TransactionDataRendererCompose
import org.multipaz.utopia.knowntypes.DelegateTransaction

class SpendingPermissionTest {

    private val agentCnf =
        """{"jwk":{"kty":"EC","crv":"P-256","x":"wu7CCOUoAAAAAAAAAAAAAAAAac733T-o","y":"y"}}"""

    private val merchant =
        """{"id":"shop.example.com","name":"utopia","origin":"https://shop.example.com"}"""

    private val checkoutMandate = payloadOf(
        """{"vct":"mandate.checkout.open.1","exp":1821496085,"cnf":$agentCnf,"constraints":[
             {"type":"checkout.allowed_merchants","allowed":[$merchant]},
             {"type":"checkout.line_items","items":[
               {"id":"aurora","acceptable_items":[{"id":"aurora","title":"Aurora headphones"}],"quantity":1}]}]}"""
    )

    private val paymentMandate = payloadOf(
        """{"vct":"mandate.payment.open.1","exp":1821496085,"cnf":$agentCnf,"constraints":[
             {"type":"payment.amount_range","currency":"USD","max":13000},
             {"type":"payment.budget","currency":"USD","max":20000},
             {"type":"payment.allowed_payees","allowed":[$merchant]}]}"""
    )

    private fun payloadOf(json: String) =
        DelegateTransaction.Payload("dSD-JWT", "unused", Json.parseToJsonElement(json).jsonObject)

    // AP2 requires the signed Mandate Content to be shown, so the summary alone is not enough:
    // the delegate renderer must come with a details page.
    @Test
    fun delegateIsDrawnByTheWalletWithADetailsPage() {
        val renderer = walletTransactionDataRenderers.getRenderer(DelegateTransaction.identifier)
        assertTrue(renderer is TransactionDataRendererCompose)
        assertNotNull((renderer as TransactionDataRendererCompose).details)
    }

    @Test
    fun detailsListEachMandateInFull() {
        val sections = mandateSections(listOf(checkoutMandate, paymentMandate))

        assertEquals(2, sections.size)
        assertEquals("What can be bought", sections[0].heading)
        assertEquals("mandate.checkout.open.1", sections[0].mandateType)
        assertEquals("How much can be spent", sections[1].heading)
        assertEquals("mandate.payment.open.1", sections[1].mandateType)
        assertEquals(DelegateTransaction.summarize(checkoutMandate), sections[0].lines)
        assertEquals(DelegateTransaction.summarize(paymentMandate), sections[1].lines)
    }

    // The summary prints the shared merchant, expiry and agent once; the details page must not
    // hide the second mandate's copy of them.
    @Test
    fun detailsKeepWhatTheSummaryDeduplicates() {
        val payloads = listOf(checkoutMandate, paymentMandate)
        val detailLines = mandateSections(payloads).sumOf { it.lines.size }

        assertTrue(detailLines > DelegateTransaction.describePermission(payloads).details.size)
    }

    @Test
    fun aMandateWithoutATypeIsSaidToBeUnrecognised() {
        val untyped = payloadOf("""{"exp":1821496085,"cnf":$agentCnf}""")
        val section = mandateSections(listOf(untyped)).single()

        assertEquals("", section.mandateType)
        assertEquals("A condition your wallet doesn't recognise", section.heading)
    }
}
