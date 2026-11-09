package org.multipaz.wallet.android.ui.presentment

import org.multipaz.compose.presentment.TransactionDataRendererCompose
import org.multipaz.prompt.TransactionDataRendererRepository
import org.multipaz.utopia.knowntypes.DelegateTransaction

/**
 * The wallet's renderers for transaction data in the consent prompt.
 *
 * A [DelegateTransaction] is drawn as a spending permission: the limits and conditions in the
 * prompt, and every line of every mandate on the details page. Transaction types without a
 * renderer here keep the consent prompt's built-in rendering.
 */
val walletTransactionDataRenderers = TransactionDataRendererRepository().apply {
    addRenderer(
        DelegateTransaction.identifier,
        TransactionDataRendererCompose(
            summary = { context -> SpendingPermissionSummary(context) },
            details = { context -> SpendingPermissionDetails(context) },
        )
    )
}
