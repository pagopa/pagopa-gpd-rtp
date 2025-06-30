#!/bin/bash

export INGESTION_EVENTHUB_HOST=$1
export INGESTION_EVENTHUB_CONN_STRING=$2
export INGESTION_EVENTHUB_TOPICS=$3

docker compose up -d --remove-orphans --force-recreate --build