import SwiftUI
import Multipaz

/// The wallet's renderers for transaction data in the consent prompt.
///
/// A `DelegateTransaction` is drawn as a spending permission: the limits and conditions in the
/// prompt, and every line of every mandate on the details page. Transaction types without a
/// renderer here keep the consent prompt's built-in rendering.
let walletTransactionDataRenderers: TransactionDataRendererRepository = {
    let repository = TransactionDataRendererRepository()
    repository.addRenderer(
        identifier: DelegateTransaction.shared.identifier,
        renderer: TransactionDataRendererSwiftUi(
            summary: { context in AnyView(SpendingPermissionSummary(context: context)) },
            details: { context in AnyView(SpendingPermissionDetails(context: context)) }
        )
    )
    return repository
}()

private func delegatePayloads(_ context: TransactionDataContext) -> [DelegateTransaction.Payload] {
    context.transactionData.compactMap { $0.payload as? DelegateTransaction.Payload }
}

/// The spending permission in the consent prompt.
///
/// The limits lead and are set large, because they are what the person has to read correctly.
/// The conditions follow as short rows. Strings come from the relying party, so they are shown
/// verbatim and bounded here; the details page shows them in full.
private struct SpendingPermissionSummary: View {
    let context: TransactionDataContext

    var body: some View {
        let summary = DelegateTransaction.shared.describePermission(payloads: delegatePayloads(context))
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 12) {
                Image(systemName: "creditcard")
                    .font(.title3)
                    .foregroundStyle(.tint)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Color.accentColor.opacity(0.15)))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Spending permission")
                        .font(.headline)
                    Text("An agent will be able to spend for you without asking again, until these limits are reached.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            if !summary.limits.isEmpty {
                VStack(spacing: 0) {
                    ForEach(Array(summary.limits.enumerated()), id: \.offset) { index, limit in
                        if index > 0 { Divider() }
                        HStack(alignment: .firstTextBaseline) {
                            Text(verbatim: limit.label)
                                .font(.subheadline)
                            Spacer(minLength: 12)
                            Text(verbatim: limit.amount)
                                .font(.title2.weight(.bold))
                            Text(verbatim: limit.currency)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .accessibilityElement(children: .combine)
                    }
                }
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(.secondarySystemBackground))
                )
            }

            if !summary.rows.isEmpty {
                VStack(spacing: 0) {
                    ForEach(Array(summary.rows.enumerated()), id: \.offset) { index, line in
                        if index > 0 { Divider() }
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: symbolFor(line))
                                .foregroundStyle(.secondary)
                                .frame(width: 20)
                            Text(verbatim: line.label)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .frame(width: 72, alignment: .leading)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(verbatim: line.value)
                                    .font(.subheadline)
                                    .lineLimit(4)
                                if let detail = line.detail {
                                    Text(verbatim: detail)
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(1)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.vertical, 10)
                        .accessibilityElement(children: .combine)
                    }
                }
            }
        }
        .padding(.top, context.hasClaims ? 12 : 0)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // The labels are the SDK's wording for the rows it recognises; anything else gets a neutral icon.
    private func symbolFor(_ line: DelegateTransaction.SummaryLine) -> String {
        if line.shape == DelegateTransaction.Shape.instant { return "calendar" }
        if line.shape == DelegateTransaction.Shape.party { return "key" }
        switch line.label {
        case "At": return "storefront"
        case "For": return "bag"
        default: return "info.circle"
        }
    }
}

/// Everything the spending permission contains, mandate by mandate, as it will be signed.
///
/// The summary deduplicates what the mandates share. This page does not: AP2 requires the signed
/// Mandate Content to be shown, so each mandate is listed in full.
private struct SpendingPermissionDetails: View {
    let context: TransactionDataContext

    var body: some View {
        let payloads = delegatePayloads(context)
        let requestedBy = context.requester.origin ?? context.requester.appId
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Spending permission")
                    .font(.title2.weight(.semibold))
                Text("Everything below is part of what you sign.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            if let requestedBy {
                DetailLine(label: "Requested by", value: requestedBy)
            }
            ForEach(Array(payloads.enumerated()), id: \.offset) { _, payload in
                let mandateType = DelegateTransaction.shared.mandateType(mandate: payload.delegatePayload)
                VStack(alignment: .leading, spacing: 8) {
                    Text(verbatim: DelegateTransaction.shared.headingFor(mandateType: mandateType))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.tint)
                    if !mandateType.isEmpty {
                        Text(verbatim: "Type: \(mandateType)")
                            .font(.footnote.monospaced())
                            .foregroundStyle(.secondary)
                    }
                    let lines = DelegateTransaction.shared.summarize(payload: payload)
                    ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                        DetailLine(
                            label: line.label,
                            value: line.value,
                            detail: line.detail,
                            monospaced: line.shape == DelegateTransaction.Shape.opaque
                        )
                    }
                }
            }
        }
        .textSelection(.enabled)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct DetailLine: View {
    let label: String
    let value: String
    var detail: String? = nil
    var monospaced: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(verbatim: value)
                .font(monospaced ? .subheadline.monospaced() : .subheadline)
                .fixedSize(horizontal: false, vertical: true)
            if let detail {
                Text(verbatim: detail)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Divider()
                .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
