# =============================================================================
# build-and-push.ps1
# Builds and pushes the Producer and Consumer images to your container registry.
# Run this from the repository root.
#
# Usage:
#   $env:REGISTRY = "your-registry.example.com/illumina"
#   .\docker\build-and-push.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

$REGISTRY = if ($env:REGISTRY) { $env:REGISTRY } else { "<YOUR_IMAGE_REGISTRY>" }
$TAG      = if ($env:TAG)      { $env:TAG      } else { "latest" }

$PRODUCER_IMAGE = "${REGISTRY}/illumina-jms-producer:${TAG}"
$CONSUMER_IMAGE = "${REGISTRY}/illumina-jms-consumer:${TAG}"

Write-Host "==> Building Producer image: $PRODUCER_IMAGE"
docker build `
  -f docker/Dockerfile.producer `
  -t $PRODUCER_IMAGE `
  .

Write-Host "==> Building Consumer image: $CONSUMER_IMAGE"
docker build `
  -f docker/Dockerfile.consumer `
  -t $CONSUMER_IMAGE `
  .

Write-Host "==> Pushing images..."
docker push $PRODUCER_IMAGE
docker push $CONSUMER_IMAGE

Write-Host ""
Write-Host "Done. Update the 'image:' fields in the OpenShift YAMLs if needed:"
Write-Host "  openshift/deployment-producer.yaml"
Write-Host "  openshift/deployment-consumer.yaml"
