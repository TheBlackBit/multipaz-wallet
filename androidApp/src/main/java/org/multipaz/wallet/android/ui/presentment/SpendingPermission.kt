package org.multipaz.wallet.android.ui.presentment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionDataContext
import org.multipaz.utopia.knowntypes.DelegateTransaction
import org.multipaz.wallet.android.R

/**
 * One mandate as the details page shows it: every line it carries, none deduplicated away.
 *
 * @property heading what the mandate governs, in words.
 * @property mandateType the raw mandate type, or blank when the mandate does not say.
 * @property lines every line of the mandate, in the order it arrived.
 */
internal data class MandateSection(
    val heading: String,
    val mandateType: String,
    val lines: List<DelegateTransaction.SummaryLine>,
)

/**
 * The details page's sections, one per mandate.
 *
 * The summary deduplicates what the mandates share. The details page does not: AP2 requires the
 * signed Mandate Content to be shown, so each mandate is listed in full, as it will be signed.
 */
internal fun mandateSections(payloads: List<DelegateTransaction.Payload>): List<MandateSection> =
    payloads.map { payload ->
        val mandateType = DelegateTransaction.mandateType(payload.delegatePayload)
        MandateSection(
            heading = DelegateTransaction.headingFor(mandateType),
            mandateType = mandateType,
            lines = DelegateTransaction.summarize(payload),
        )
    }

private fun List<TransactionData<*>>.delegatePayloads(): List<DelegateTransaction.Payload> =
    mapNotNull { it.payload as? DelegateTransaction.Payload }

/**
 * The spending permission in the consent prompt.
 *
 * The limits lead and are set large, because they are what the person has to read correctly.
 * The conditions follow as short rows. Strings come from the relying party, so they are shown
 * as plain text and bounded here; the details page shows them in full.
 */
@Composable
internal fun SpendingPermissionSummary(context: TransactionDataContext) {
    val summary = remember(context.transactionData) {
        DelegateTransaction.describePermission(context.transactionData.delegatePayloads())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (context.hasClaims) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionHeader()
        if (summary.limits.isNotEmpty()) {
            LimitsCard(limits = summary.limits)
        }
        if (summary.rows.isNotEmpty()) {
            ConditionRows(rows = summary.rows)
        }
    }
}

@Composable
private fun PermissionHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.spending_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.spending_permission_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LimitsCard(limits: List<DelegateTransaction.Limit>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            limits.forEachIndexed { index, limit ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = limit.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = limit.amount,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = limit.currency,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionRows(rows: List<DelegateTransaction.SummaryLine>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, line ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Icon(
                    imageVector = iconFor(line),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = line.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(88.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.value,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    line.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// The labels are the SDK's wording for the rows it recognises; anything else gets a neutral icon.
private fun iconFor(line: DelegateTransaction.SummaryLine): ImageVector = when {
    line.shape == DelegateTransaction.Shape.INSTANT -> Icons.Outlined.Event
    line.shape == DelegateTransaction.Shape.PARTY -> Icons.Outlined.Key
    line.label == "At" -> Icons.Outlined.Storefront
    line.label == "For" -> Icons.Outlined.ShoppingBag
    else -> Icons.Outlined.Info
}

/**
 * Everything the spending permission contains, mandate by mandate, as it will be signed.
 */
@Composable
internal fun SpendingPermissionDetails(context: TransactionDataContext) {
    val sections = remember(context.transactionData) {
        mandateSections(context.transactionData.delegatePayloads())
    }
    val requestedBy = context.requester.origin ?: context.requester.appId
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.spending_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.spending_permission_details_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            requestedBy?.let {
                DetailLine(
                    label = stringResource(R.string.spending_permission_requested_by),
                    value = it,
                )
            }
            sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (section.mandateType.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.spending_permission_mandate_type, section.mandateType),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    section.lines.forEach { line ->
                        DetailLine(
                            label = line.label,
                            value = line.value,
                            detail = line.detail,
                            monospace = line.shape == DelegateTransaction.Shape.OPAQUE,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    detail: String? = null,
    monospace: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
        )
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
