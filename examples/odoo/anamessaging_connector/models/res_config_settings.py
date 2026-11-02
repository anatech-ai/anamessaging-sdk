from odoo import fields, models


class ResConfigSettings(models.TransientModel):
    _inherit = "res.config.settings"

    ana_client_id = fields.Char("ANA Client ID", config_parameter="anamessaging.client_id")
    ana_client_secret = fields.Char("ANA Client Secret", config_parameter="anamessaging.client_secret")
    ana_base_url = fields.Char("ANA API URL", config_parameter="anamessaging.base_url",
                               default="https://api.anamessaging.com")
    ana_order_template = fields.Char("Order confirmation template", config_parameter="anamessaging.order_template",
                                     default="order_confirmation")
    ana_invoice_template = fields.Char("Invoice template (document header)",
                                      config_parameter="anamessaging.invoice_template", default="invoice_document")
    ana_auto_order = fields.Boolean("WhatsApp customers when an order is confirmed",
                                    config_parameter="anamessaging.auto_order")
