{
    "name": "ANA Messaging Connector",
    "version": "17.0.1.0.0",
    "summary": "Send WhatsApp and SMS from Odoo through ANA Messaging",
    "description": "Sends order confirmations, invoices and custom messages over WhatsApp / SMS "
                   "using the anamessaging Python SDK. Needs an active ANA Messaging API subscription.",
    "author": "ANA Messaging",
    "website": "https://anamessaging.com",
    "category": "Productivity",
    "license": "LGPL-3",
    "depends": ["base_setup", "sale", "account"],
    "external_dependencies": {"python": ["anamessaging"]},
    "data": [
        "security/ir.model.access.csv",
        "views/res_config_settings_views.xml",
        "views/actions.xml",
    ],
    "installable": True,
}
