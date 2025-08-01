resource "github_repository_environment" "github_repository_environment" {
  environment = var.env
  repository  = local.github.repository
  # filter teams reviewers from github_organization_teams
  # if reviewers_teams is null no reviewers will be configured for environment
  dynamic "reviewers" {
    for_each = (var.github_repository_environment.reviewers_teams == null || var.env_short != "p" ? [] : [1])
    content {
      teams = matchkeys(
        data.github_organization_teams.all.teams.*.id,
        data.github_organization_teams.all.teams.*.name,
        var.github_repository_environment.reviewers_teams
      )
    }
  }
  deployment_branch_policy {
    protected_branches     = var.github_repository_environment.protected_branches
    custom_branch_policies = var.github_repository_environment.custom_branch_policies
  }
}

locals {
  env_secrets = {
    "CLIENT_ID" : data.azurerm_user_assigned_identity.identity_cd_01.client_id,
    "TENANT_ID" : data.azurerm_client_config.current.tenant_id,
    "SUBSCRIPTION_ID" : data.azurerm_subscription.current.subscription_id,
    "SUBKEY" : data.azurerm_key_vault_secret.key_vault_integration_test_subkey.value,
  }
  env_variables = {
    "CONTAINER_APP_ENVIRONMENT_NAME" : local.container_app_environment.name,
    "CONTAINER_APP_ENVIRONMENT_RESOURCE_GROUP_NAME" : local.container_app_environment.resource_group,
    "CLUSTER_NAME" : local.aks_cluster.name,
    "CLUSTER_RESOURCE_GROUP" : local.aks_cluster.resource_group_name,
    "NAMESPACE" : local.domain,
    "WORKLOAD_IDENTITY_ID": data.azurerm_user_assigned_identity.workload_identity_clientid.client_id
  }
  repo_secrets = {
    "SONAR_TOKEN" : data.azurerm_key_vault_secret.key_vault_sonar.value,
    "BOT_TOKEN_GITHUB" : data.azurerm_key_vault_secret.key_vault_bot_token.value,
    "CUCUMBER_PUBLISH_TOKEN" : data.azurerm_key_vault_secret.key_vault_cucumber_token.value,
    "DEPLOY_SLACK_WEBHOOK_URL": data.azurerm_key_vault_secret.key_vault_deploy_slack_webhook.value,
    "INTEGRATION_TEST_SLACK_WEBHOOK_URL": data.azurerm_key_vault_secret.key_vault_integration_test_slack_webhook.value
  }
  special_repo_secrets = {
    "CLIENT_ID" : {
      "key" : "${upper(var.env)}_CLIENT_ID",
      "value" : data.azurerm_user_assigned_identity.identity_oidc.client_id
    },
    "TENANT_ID" : {
      "key" : "${upper(var.env)}_TENANT_ID",
      "value" : data.azurerm_client_config.current.tenant_id
    },
    "SUBSCRIPTION_ID" : {
      "key" : "${upper(var.env)}_SUBSCRIPTION_ID",
      "value" : data.azurerm_subscription.current.subscription_id
    },
    "PG_GPD_PASSWORD" : {
      "key" : "${upper(var.env)}_PG_GPD_PASSWORD",
      "value" : data.azurerm_key_vault_secret.key_vault_db_apd_password.value
    },
    "PG_GPD_USERNAME" : {
      "key" : "${upper(var.env)}_PG_GPD_USERNAME",
      "value" : data.azurerm_key_vault_secret.key_vault_db_apd_user_name.value
    },
    "OPT_IN_REDIS_PASSWORD" : {
      "key" : "${upper(var.env)}_OPT_IN_REDIS_PASSWORD",
      "value" : data.azurerm_key_vault_secret.key_vault_redis_password.value
    },
    "OPT_IN_REDIS_HOSTNAME" : {
      "key" : "${upper(var.env)}_OPT_IN_REDIS_HOSTNAME",
      "value" : data.azurerm_key_vault_secret.key_vault_redis_hostname.value
    },
    "RTP_EVENTHUB_CONN_STRING" : {
      "key" : "${upper(var.env)}_RTP_EVENTHUB_CONN_STRING",
      "value" : (var.env_short != "p" ? data.azurerm_key_vault_secret.key_vault_ehub_rtp_integration_test_connection_string[0].value : "not-used-only-4-test")
    },
  }
}

###############
# ENV Secrets #
###############

resource "github_actions_environment_secret" "github_environment_runner_secrets" {
  for_each        = local.env_secrets
  repository      = local.github.repository
  environment     = var.env
  secret_name     = each.key
  plaintext_value = each.value
}

#################
# ENV Variables #
#################


resource "github_actions_environment_variable" "github_environment_runner_variables" {
  for_each      = local.env_variables
  repository    = local.github.repository
  environment   = var.env
  variable_name = each.key
  value         = each.value
}

#############################
# Secrets of the Repository #
#############################


resource "github_actions_secret" "repo_secrets" {
  for_each        = local.repo_secrets
  repository      = local.github.repository
  secret_name     = each.key
  plaintext_value = each.value
}


resource "github_actions_secret" "special_repo_secrets" {
  for_each        = local.special_repo_secrets
  repository      = local.github.repository
  secret_name     = each.value.key
  plaintext_value = each.value.value
}
