locals {
  product = "${var.prefix}-${var.env_short}"
  project = "${var.prefix}-${var.env_short}-${var.location_short}-${var.domain}"

  apim = {
    name       = "${local.product}-apim"
    rg         = "${local.product}-api-rg"
    product_id = "gpsrtp"
  }
  apim_hostname = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
  hostname      = var.env == "prod" ? "weuprod.gps.internal.platform.pagopa.it" : "weu${var.env}.gps.internal.${var.env}.platform.pagopa.it"


}

