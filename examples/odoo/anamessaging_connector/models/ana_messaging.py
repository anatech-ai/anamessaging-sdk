import logging
import re

from odoo import _, api, models
from odoo.exceptions import UserError

_logger = logging.getLogger(__name__)
_CLIENTS = {}  # one SDK client per database; it is thread-safe and caches its token


class AnaMessaging(models.AbstractModel):
    """Service model: ``self.env["ana.messaging"].client()`` anywhere in Odoo."""

    _name = "ana.messaging"
    _description = "ANA Messaging service"

    @api.model
    def client(self):
        try:
            from anamessaging import AnaMessagingClient
        except ImportError:
            raise UserError(_("Install the SDK on the Odoo server: pip install anamessaging"))
        icp = self.env["ir.config_parameter"].sudo()
        cid, secret = icp.get_param("anamessaging.client_id"), icp.get_param("anamessaging.client_secret")
        base = icp.get_param("anamessaging.base_url") or "https://api.anamessaging.com"
        if not cid or not secret:
            raise UserError(_("Set the ANA Messaging client id and secret in Settings."))
        key = (self.env.cr.dbname, cid, secret, base)
        if key not in _CLIENTS:
            _CLIENTS[key] = AnaMessagingClient(cid, secret, base_url=base)
        return _CLIENTS[key]

    @api.model
    def phone_of(self, partner):
        number = partner.mobile or partner.phone or ""
        digits = re.sub(r"\D", "", number)
        if digits and len(digits) == 10 and partner.country_id.phone_code:
            digits = "%s%s" % (partner.country_id.phone_code, digits)
        if not digits:
            raise UserError(_("%s has no mobile number.") % partner.display_name)
        return digits

    @api.model
    def run(self, record, fn):
        """Call the SDK and turn its errors into user messages + chatter notes."""
        from anamessaging import AnaMessagingError, SubscriptionInactiveError

        try:
            res = fn(self.client())
        except SubscriptionInactiveError:
            raise UserError(_("Your ANA Messaging subscription is not active. Renew it on anamessaging.com."))
        except AnaMessagingError as exc:
            _logger.warning("ANA Messaging error on %s: %s", record, exc)
            raise UserError(_("ANA Messaging: %s") % exc)
        if hasattr(record, "message_post"):
            record.message_post(body=_("ANA Messaging: %s (message id %s)") % (
                res.message, getattr(res.data, "messageId", "-")))
        return res
