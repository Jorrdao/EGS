#!/bin/bash
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${BLUE}[INFO]${NC}  $1"; }
success() { echo -e "${GREEN}[OK]${NC}    $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Kubeconfig ────────────────────────────────────────────────────────────────
if [ -n "$1" ]; then
    export KUBECONFIG="$1"
elif [ -z "$KUBECONFIG" ]; then
    FOUND=$(find . -maxdepth 3 -name "*kubeconfig*" 2>/dev/null | head -1)
    [ -n "$FOUND" ] && export KUBECONFIG="$FOUND" || error "Kubeconfig not found. Pass it as argument: ./deploy.sh /path/to/kubeconfig.yaml"
fi

NAMESPACE="tenant-grupo7-egs-deti-ua-pt"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DASHBOARDS_DIR="$SCRIPT_DIR/../infra/grafana/dashboards"
K8S_DIR="$SCRIPT_DIR"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  StormOS — Kubernetes Deploy${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# ── Preflight ─────────────────────────────────────────────────────────────────
command -v kubectl >/dev/null 2>&1 || error "kubectl not found"
kubectl get pods -n "$NAMESPACE" --request-timeout=10s > /dev/null 2>&1 || error "Cannot reach cluster. Check kubeconfig and VPN."
success "Cluster reachable — namespace: $NAMESPACE"

# ── Main deployment ───────────────────────────────────────────────────────────
log "Applying deployment.yaml..."
kubectl apply -f "$K8S_DIR/deployment.yaml"
success "deployment.yaml applied"

# ── Dashboard ConfigMaps from JSON files ──────────────────────────────────────
# These are created directly from the files in infra/grafana/dashboards/
# so you never need to hardcode JSON inside deployment.yaml.
# Just update the .json file and re-run this script.
echo ""
log "Updating Grafana dashboard ConfigMaps from $DASHBOARDS_DIR..."

for json_file in "$DASHBOARDS_DIR"/*.json; do
    filename=$(basename "$json_file")                        # e.g. user_behaviour.json
    cm_name="grafana-dashboard-$(basename "$json_file" .json | tr '_' '-')"  # grafana-dashboard-user-behaviour

    kubectl create configmap "$cm_name" \
        --from-file="$filename=$json_file" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -

    success "ConfigMap $cm_name ← $filename"
done

# ── Restart Grafana to pick up new ConfigMaps ─────────────────────────────────
echo ""
log "Restarting Grafana..."
kubectl rollout restart deployment/grafana -n "$NAMESPACE"
kubectl rollout status deployment/grafana  -n "$NAMESPACE" --timeout=90s
success "Grafana restarted"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Done!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  Grafana:  http://stormos-103075.duckdns.org/grafana"
echo "  API:      http://stormos-103075.duckdns.org/api/v1/items"
echo "  InfluxDB: http://stormos-103075.duckdns.org/influx"
echo ""