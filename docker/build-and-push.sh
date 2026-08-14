#!/usr/bin/env bash
# =============================================================================
# build-and-push.sh
# Builds and pushes the Producer and Consumer images to your container registry.
# Run this from the repository root.
#
# Usage:
#   REGISTRY=your-registry.example.com/illumina ./docker/build-and-push.sh
#   TAG=1.2.3 REGISTRY=your-registry.example.com/illumina ./docker/build-and-push.sh
# =============================================================================
set -euo pipefail

REGISTRY=${REGISTRY:-"<YOUR_IMAGE_REGISTRY>"}
TAG=${TAG:-"latest"}

PRODUCER_IMAGE="${REGISTRY}/illumina-jms-producer:${TAG}"
CONSUMER_IMAGE="${REGISTRY}/illumina-jms-consumer:${TAG}"

echo "==> Building Producer image: ${PRODUCER_IMAGE}"
docker build \
  -f docker/Dockerfile.producer \
  -t "${PRODUCER_IMAGE}" \
  .

echo "==> Building Consumer image: ${CONSUMER_IMAGE}"
docker build \
  -f docker/Dockerfile.consumer \
  -t "${CONSUMER_IMAGE}" \
  .

echo "==> Pushing images..."
docker push "${PRODUCER_IMAGE}"
docker push "${CONSUMER_IMAGE}"

echo ""
echo "Done. Update the 'image:' fields in the OpenShift YAMLs if needed:"
echo "  openshift/deployment-producer.yaml"
echo "  openshift/deployment-consumer.yaml"
