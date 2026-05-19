#!/bin/bash
set -e

echo "=== StormOS Kubernetes Deploy ==="
echo "Namespace: 103075"
echo ""

echo "1. A configurar kubectl..."
mkdir -p ~/.kube
cp config ~/.kube/config
kubectl config set-cluster default --insecure-skip-tls-verify=false

echo "2. A criar namespace..."
kubectl create namespace 103075 --dry-run=client -o yaml | kubectl apply -f -

echo "3. A fazer build e push da imagem FastAPI..."
docker buildx build --platform linux/amd64 --network=host \
  -t registry.deti:5000/103075/geolocation:v1 \
  -f Dockerfile .
docker push registry.deti:5000/103075/geolocation:v1

echo "4. A aplicar init SQL ConfigMap..."
kubectl apply -f init-sql-configmap.yaml

echo "5. A criar volumes persistentes..."
kubectl apply -f storage.yaml

echo "6. A fazer deploy de todos os servicos..."
kubectl apply -f deployment.yaml

echo ""
echo "=== Deploy concluido! ==="
echo ""
echo "A verificar pods..."
kubectl get pods -n 103075

echo ""
echo "URLs publicos:"
echo "  API:     https://stormos-103075.deti.ua.pt"
echo "  Grafana: https://stormos-grafana-103075.deti.ua.pt"
