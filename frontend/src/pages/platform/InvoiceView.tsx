import { useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { Printer } from "react-feather";
import { errorMessage } from "../../api/client";
import {
  organizationsApi,
  paymentsApi,
  receiptsApi,
  subscriptionsApi,
} from "../../api/platform";
import { CHANNEL_LABEL, type Invoice } from "../../api/types";
import { Button, ErrorBox, Modal } from "../../components/ui";
import { formatMoney, label } from "../../lib/format";
import "../../styles/invoice.css";

const money = (v: number, c: string | null) => `${formatMoney(v)} ${c ?? ""}`;
const day = (v: string | null) => (v ? v.slice(0, 10) : "-");

/**
 * Prints one element on its own: clones it into #mh-print-root (a direct child of <body>) and hides
 * everything else with print CSS. Works on mobile browsers too, where printing an iframe prints the
 * whole page. The browser's print dialog offers "Save as PDF"; the document title becomes the file name.
 */
function printElement(el: HTMLElement, fileName: string) {
  document.getElementById("mh-print-root")?.remove();
  const root = document.createElement("div");
  root.id = "mh-print-root";
  root.appendChild(el.cloneNode(true));
  document.body.appendChild(root);
  document.body.classList.add("mh-printing");
  const oldTitle = document.title;
  document.title = fileName;

  const cleanup = () => {
    document.body.classList.remove("mh-printing");
    root.remove();
    document.title = oldTitle;
    window.removeEventListener("afterprint", cleanup);
  };
  window.addEventListener("afterprint", cleanup);
  window.print();
}

/** Invoice preview dialog with a Print / Save as PDF button (platform staff, Billing page). */
export function InvoiceViewModal({
  invoice,
  onClose,
}: {
  invoice: Invoice;
  onClose: () => void;
}) {
  const docRef = useRef<HTMLDivElement>(null);
  const org = useQuery({
    queryKey: ["organization", invoice.organizationId],
    queryFn: () => organizationsApi.get(invoice.organizationId),
  });
  const subs = useQuery({
    queryKey: ["subscriptions"],
    queryFn: () => subscriptionsApi.list(),
  });
  const payments = useQuery({
    queryKey: ["payments"],
    queryFn: () => paymentsApi.list(),
  });
  const receipts = useQuery({
    queryKey: ["receipts"],
    queryFn: () => receiptsApi.list(),
  });

  const o = org.data;
  const plan = subs.data?.find(
    (s) => s.id === invoice.subscriptionId,
  )?.planName;
  const paid = (payments.data ?? []).filter((p) => p.invoiceId === invoice.id);
  const receiptNo = (paymentId: number) =>
    receipts.data?.find((r) => r.paymentId === paymentId)?.receiptNumber ?? "-";
  const description = plan
    ? `${plan} plan subscription`
    : "Subscription services";
  const loading = org.isLoading || payments.isLoading;

  return (
    <Modal title={`Invoice ${invoice.invoiceNumber}`} onClose={onClose}>
      <div className="modal-body">
        {org.error && <ErrorBox message={errorMessage(org.error)} />}
        <div className="mh-invoice" ref={docRef}>
          <div className="inv-head">
            <div>
              <img
                src="/brand/mindhr-lockup.png"
                alt="mindHR"
                className="inv-logo"
              />
              <div className="mt-2 small text-muted">
                mindHR workforce management
              </div>
            </div>
            <div className="inv-title">
              <h2>INVOICE</h2>
              <div className="inv-number">{invoice.invoiceNumber}</div>
              <span className={`inv-status ${invoice.status}`}>
                {label(invoice.status)}
              </span>
            </div>
          </div>

          <div className="inv-parties">
            <div>
              <div className="inv-label">Bill to</div>
              <div className="fw-bold">
                {o?.name ?? `Organization #${invoice.organizationId}`}
              </div>
              {o?.ownerName && <div>{o.ownerName}</div>}
              {o?.address && <div>{o.address}</div>}
              {o?.email && <div>{o.email}</div>}
              {o?.phone && <div>{o.phone}</div>}
            </div>
            <table className="inv-dates">
              <tbody>
                <tr>
                  <td className="inv-label">Issue date</td>
                  <td>{invoice.issueDate}</td>
                </tr>
                <tr>
                  <td className="inv-label">Due date</td>
                  <td>{invoice.dueDate}</td>
                </tr>
                <tr>
                  <td className="inv-label">Paid on</td>
                  <td>{day(invoice.paidAt)}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <table className="inv-lines">
            <thead>
              <tr>
                <th>Description</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <div className="fw-bold">{description}</div>
                  {invoice.notes && (
                    <div className="small text-muted">{invoice.notes}</div>
                  )}
                </td>
                <td className="num">
                  {money(invoice.amount, invoice.currency)}
                </td>
              </tr>
              <tr className="inv-total">
                <td className="num">Total</td>
                <td className="num">
                  {money(invoice.amount, invoice.currency)}
                </td>
              </tr>
            </tbody>
          </table>

          <h6>Payments</h6>
          {loading ? (
            <p className="text-muted small">Loading...</p>
          ) : paid.length === 0 ? (
            <p className="text-muted small mb-0">
              No payment recorded for this invoice.
            </p>
          ) : (
            <div className="inv-scroll">
              <table className="inv-lines">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Method</th>
                    <th>Transaction</th>
                    <th>Status</th>
                    <th>Receipt</th>
                    <th className="num">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {paid.map((p) => (
                    <tr key={p.id}>
                      <td className="text-nowrap">{day(p.paidAt)}</td>
                      <td>
                        {p.channel
                          ? CHANNEL_LABEL[p.channel]
                          : (p.gateway ?? "-")}
                      </td>
                      <td>{p.gatewayTransactionId ?? "-"}</td>
                      <td>{label(p.status)}</td>
                      <td className="text-nowrap">
                        {p.hasReceipt ? receiptNo(p.id) : "-"}
                      </td>
                      <td className="num">{money(p.amount, p.currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="inv-foot">
            Thank you for your business. This invoice was generated by mindHR.
          </div>
        </div>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
        <Button
          disabled={loading}
          onClick={() =>
            docRef.current &&
            printElement(docRef.current, invoice.invoiceNumber)
          }
        >
          <Printer size={15} className="me-1" />
          Print / Save as PDF
        </Button>
      </div>
    </Modal>
  );
}
