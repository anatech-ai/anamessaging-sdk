from odoo import models


class AccountMove(models.Model):
    _inherit = "account.move"

    def action_ana_send_invoice_whatsapp(self):
        """Send the invoice PDF as the document header of an approved template."""
        from anamessaging import FileAttachment, TemplateComponent

        ana = self.env["ana.messaging"]
        template = self.env["ir.config_parameter"].sudo().get_param("anamessaging.invoice_template") or "invoice_document"
        for move in self:
            pdf, _ = self.env["ir.actions.report"]._render_qweb_pdf("account.account_invoices", move.ids)
            ana.run(move, lambda c: c.whatsapp.send_template(
                ana.phone_of(move.partner_id), template,
                components=[TemplateComponent.body(move.partner_id.name, move.name,
                                                   "%s %.2f" % (move.currency_id.name, move.amount_residual))],
                attachment=FileAttachment(pdf, "%s.pdf" % move.name.replace("/", "-"), "application/pdf"),
                campaign_id=move.name,
            ))
        return True

    def action_ana_send_sms_reminder(self):
        ana = self.env["ana.messaging"]
        for move in self:
            ana.run(move, lambda c: c.sms.send(
                to=ana.phone_of(move.partner_id),
                message="Dear %s, invoice %s of %s %.2f is due on %s." % (
                    move.partner_id.name, move.name, move.currency_id.name, move.amount_residual,
                    move.invoice_date_due or ""),
                campaignId=move.name,
            ))
        return True
