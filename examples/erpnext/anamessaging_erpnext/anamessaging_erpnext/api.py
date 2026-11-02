"""ANA Messaging for Frappe / ERPNext.

Credentials go in ``site_config.json`` (never in code):

    bench --site mysite set-config anamessaging_client_id "..."
    bench --site mysite set-config anamessaging_client_secret "..."
    bench --site mysite set-config anamessaging_base_url "https://api.anamessaging.com"   # optional

Whitelisted methods are callable from client scripts / REST:
``frappe.call("anamessaging_erpnext.api.send_whatsapp_text", {to, text})``.
"""
import re

import frappe
from frappe import _

_CLIENTS = {}


def get_client():
    from anamessaging import AnaMessagingClient

    conf = frappe.conf
    cid, secret = conf.get("anamessaging_client_id"), conf.get("anamessaging_client_secret")
    if not cid or not secret:
        frappe.throw(_("Set anamessaging_client_id and anamessaging_client_secret in site_config.json"))
    key = (frappe.local.site, cid)
    if key not in _CLIENTS:
        _CLIENTS[key] = AnaMessagingClient(cid, secret, base_url=conf.get("anamessaging_base_url"))
    return _CLIENTS[key]


def normalize(number, default_country_code="91"):
    digits = re.sub(r"\D", "", number or "")
    if len(digits) == 10:
        digits = default_country_code + digits
    if not digits:
        frappe.throw(_("No mobile number"))
    return digits


def call(fn, reference_doctype=None, reference_name=None):
    """Run an SDK call; map errors to Frappe messages and log the result as a Comment."""
    from anamessaging import AnaMessagingError, SubscriptionInactiveError

    try:
        res = fn(get_client())
    except SubscriptionInactiveError:
        frappe.throw(_("ANA Messaging subscription is not active. Renew it on anamessaging.com."))
    except AnaMessagingError as exc:
        frappe.log_error(title="ANA Messaging", message=str(exc))
        frappe.throw(_("ANA Messaging: {0}").format(exc))
    if reference_doctype and reference_name:
        frappe.get_doc(reference_doctype, reference_name).add_comment(
            "Info", _("ANA Messaging: {0}").format(res.message))
    return res.raw


@frappe.whitelist()
def send_whatsapp_text(to, text, reference_doctype=None, reference_name=None):
    return call(lambda c: c.whatsapp.send_text(normalize(to), text), reference_doctype, reference_name)


@frappe.whitelist()
def send_whatsapp_template(to, template_name, params=None, language="en", reference_doctype=None, reference_name=None):
    from anamessaging import TemplateComponent

    params = frappe.parse_json(params) if isinstance(params, str) else (params or [])
    comps = [TemplateComponent.body(*params)] if params else None
    return call(lambda c: c.whatsapp.send_template(normalize(to), template_name, language, comps),
                reference_doctype, reference_name)


@frappe.whitelist()
def send_sms(to, message, dlt_template_id=None, principal_entity_id=None, reference_doctype=None, reference_name=None):
    return call(lambda c: c.sms.send(to=normalize(to), message=message, dltTemplateId=dlt_template_id,
                                     principalEntityId=principal_entity_id), reference_doctype, reference_name)
