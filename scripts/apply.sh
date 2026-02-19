#!/usr/bin/env bash
set -euo pipefail

K="${K:-sudo kubectl}"
NS="${NS:-semicolon}"

$K apply -f k8s/semicolon/00-namespace.yml
$K apply -f k8s/semicolon/ingress/clusterissuer-letsencrypt-prod.yml

$K -n "$NS" apply --recursive -f k8s/semicolon/services
$K -n "$NS" apply --recursive -f k8s/semicolon/ingress

$K -n "$NS" get deploy -o wide
$K -n "$NS" get svc -o wide
$K -n "$NS" get ingress -o wide
