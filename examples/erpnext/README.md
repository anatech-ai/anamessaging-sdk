# ERPNext / Frappe: anamessaging_erpnext

A Frappe app that sends WhatsApp and SMS from ERPNext through ANA Messaging.

- `doc_events` (in `hooks.py`):
  - **Sales Order** submit sends the `order_confirmation` template.
  - **Sales Invoice** submit sends the `invoice_document` template, with the print-format PDF as
    its document header.
  - **Delivery Note** submit sends an SMS.

  Sends are enqueued after commit, so a submit never waits on the network.
- Whitelisted methods for client scripts, server scripts and REST:
  `anamessaging_erpnext.api.send_whatsapp_text`, `send_whatsapp_template`, `send_sms`.
- Each send adds a Comment on the document. Errors go to the Error Log.

## Install

```bash
cd frappe-bench
bench get-app /path/to/anamessaging-sdk/examples/erpnext/anamessaging_erpnext
bench --site mysite install-app anamessaging_erpnext
bench --site mysite set-config anamessaging_client_id "..."
bench --site mysite set-config anamessaging_client_secret "..."
```

`bench get-app` installs the `anamessaging` SDK from PyPI as a dependency.

From a client script:

```js
frappe.call('anamessaging_erpnext.api.send_whatsapp_template', {
  to: frm.doc.contact_mobile, template_name: 'payment_reminder',
  params: [frm.doc.customer_name, frm.doc.name, frm.doc.outstanding_amount],
  reference_doctype: frm.doctype, reference_name: frm.doc.name,
});
```
