from odoo import models


class SaleOrder(models.Model):
    _inherit = "sale.order"

    def action_ana_send_whatsapp(self):
        from anamessaging import TemplateComponent

        ana = self.env["ana.messaging"]
        template = self.env["ir.config_parameter"].sudo().get_param("anamessaging.order_template") or "order_confirmation"
        for order in self:
            # Template body: "Hi {{1}}, your order {{2}} for {{3}} is confirmed."
            ana.run(order, lambda c: c.whatsapp.send_template(
                ana.phone_of(order.partner_id), template,
                components=[TemplateComponent.body(order.partner_id.name, order.name,
                                                   "%s %.2f" % (order.currency_id.name, order.amount_total))],
                campaign_id=order.name,
            ))
        return True

    def action_confirm(self):
        res = super().action_confirm()
        if self.env["ir.config_parameter"].sudo().get_param("anamessaging.auto_order"):
            try:
                self.action_ana_send_whatsapp()
            except Exception:  # never block order confirmation on messaging
                pass
        return res
