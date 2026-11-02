"""Document hooks (see hooks.py). Sends are enqueued so a slow network never blocks a submit."""
import frappe
from frappe.utils import fmt_money

from .api import call, normalize


def _mobile(doc):
    number = doc.get("contact_mobile") or frappe.db.get_value("Customer", doc.customer, "mobile_no")
    return normalize(number) if number else None


def sales_order_submitted(doc, method=None):
    to = _mobile(doc)
    if to:
        frappe.enqueue(_send_order, queue="short", name=doc.name, to=to, enqueue_after_commit=True)


def _send_order(name, to):
    from anamessaging import TemplateComponent

    doc = frappe.get_doc("Sales Order", name)
    # Template "order_confirmation": "Hi {{1}}, your order {{2}} for {{3}} is confirmed."
    call(lambda c: c.whatsapp.send_template(
        to, "order_confirmation",
        components=[TemplateComponent.body(doc.customer_name, doc.name, fmt_money(doc.grand_total, currency=doc.currency))],
        campaign_id=doc.name), "Sales Order", name)


def sales_invoice_submitted(doc, method=None):
    to = _mobile(doc)
    if to:
        frappe.enqueue(_send_invoice, queue="short", name=doc.name, to=to, enqueue_after_commit=True)


def _send_invoice(name, to):
    from anamessaging import FileAttachment, TemplateComponent

    doc = frappe.get_doc("Sales Invoice", name)
    pdf = frappe.get_print("Sales Invoice", name, as_pdf=True)
    # Template "invoice_document" with a DOCUMENT header: the PDF goes as the attachment.
    call(lambda c: c.whatsapp.send_template(
        to, "invoice_document",
        components=[TemplateComponent.body(doc.customer_name, doc.name, fmt_money(doc.grand_total, currency=doc.currency))],
        attachment=FileAttachment(pdf, "%s.pdf" % name, "application/pdf"),
        campaign_id=doc.name), "Sales Invoice", name)


def delivery_note_submitted(doc, method=None):
    to = _mobile(doc)
    if to:
        frappe.enqueue(
            "anamessaging_erpnext.api.send_sms", queue="short", enqueue_after_commit=True,
            to=to, message="Dear %s, your delivery %s has been dispatched." % (doc.customer_name, doc.name),
            reference_doctype="Delivery Note", reference_name=doc.name)
