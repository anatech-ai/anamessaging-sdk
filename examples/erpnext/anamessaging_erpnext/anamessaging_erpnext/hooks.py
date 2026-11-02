app_name = "anamessaging_erpnext"
app_title = "ANA Messaging"
app_publisher = "ANA Messaging"
app_description = "WhatsApp and SMS for ERPNext via ANA Messaging"
app_license = "MIT"
required_apps = ["erpnext"]

# Send automatically when documents are submitted. Remove what you do not need.
doc_events = {
    "Sales Order": {"on_submit": "anamessaging_erpnext.notify.sales_order_submitted"},
    "Sales Invoice": {"on_submit": "anamessaging_erpnext.notify.sales_invoice_submitted"},
    "Delivery Note": {"on_submit": "anamessaging_erpnext.notify.delivery_note_submitted"},
}
