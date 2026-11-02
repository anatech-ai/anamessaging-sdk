# Odoo: ANA Messaging Connector

An Odoo 17 addon (it also runs on 16: replace the `<app>/<block>/<setting>` settings view with the
16 `div` layout). It adds:

- **Settings → ANA Messaging**: client id, secret, API URL, template names, and an "auto-send on order
  confirmation" switch. Values are stored in `ir.config_parameter`.
- **Sales Order** action: *Send WhatsApp (ANA Messaging)*. It sends the `order_confirmation`
  template, and also sends automatically on confirm if that switch is on.
- **Invoice** actions: *Send invoice on WhatsApp* (the invoice PDF goes as the template's document
  header) and *Send SMS payment reminder*.
- `self.env["ana.messaging"].client()` returns the SDK client anywhere in your own code.

Each send is logged in the record's chatter. SDK errors become user-facing messages, including a
clear one when the ANA subscription is inactive.

## Install

```bash
pip install anamessaging                     # in the Odoo server's Python environment
cp -r anamessaging_connector /path/to/odoo/custom-addons/
# restart Odoo -> Apps -> Update Apps List -> install "ANA Messaging Connector"
```

Create and get approval for the templates you use (`order_confirmation`, `invoice_document`) in
the ANA Messaging portal. Then set the names in Settings.
