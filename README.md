# illumina-jms-sample

An IBM MQ JMS demo application that runs a **Producer** and a **Consumer** against a uniform cluster, deployed as containers on OpenShift.

## Repository layout

```
.
├── src/main/java/com/illumina/demo/
│   ├── Producer.java          # Sends a text message every 2 s
│   └── Consumer.java          # Receives messages in a loop
├── docker/
│   ├── Dockerfile.producer    # Two-stage build → lean JRE runtime image
│   ├── Dockerfile.consumer
│   ├── build-and-push.sh      # Build + push helper (bash)
│   └── build-and-push.ps1     # Build + push helper (PowerShell)
├── openshift/
│   ├── deployment-producer.yaml
│   ├── deployment-consumer.yaml
│   └── secret-tls.yaml        # Template — never commit real secrets
└── pom.xml                    # Maven build — downloads all dependencies
```

## Prerequisites

| Tool | Minimum version |
|---|---|
| JDK | 17 |
| Maven | 3.9 |
| Docker / Podman | any recent |
| `oc` CLI | 4.x |

## Building locally

```bash
mvn package
```

This produces `target/illumina-jms.jar` — a fat JAR containing all IBM MQ client dependencies downloaded automatically from Maven Central.

## Building and pushing container images

```bash
# bash
REGISTRY=your-registry.example.com/illumina ./docker/build-and-push.sh

# PowerShell
$env:REGISTRY = "your-registry.example.com/illumina"
.\docker\build-and-push.ps1
```

The Dockerfiles use a two-stage build:
1. **Stage 1 (`build`)** — Maven downloads dependencies and compiles the fat JAR inside the container; no local JARs needed.
2. **Stage 2 (runtime)** — Only the JRE + the fat JAR are in the final image.

## OpenShift deployment

### 1. Create the namespace

```bash
oc new-project illumina-jms
```

### 2. Create the TLS secret

```bash
oc create secret generic illumina-jms-tls \
  --from-file=keystore.jks=/path/to/keystore.jks \
  --from-file=truststore.jks=/path/to/truststore.jks \
  --from-file=ccdt.json=/path/to/ccdt.json \
  --from-literal=keystore-password=<PASSWORD> \
  --from-literal=truststore-password=<PASSWORD> \
  -n illumina-jms
```

The secret keys are mounted read-only to `/app/config/` inside each pod. The container performs a **startup pre-flight check** and exits with a clear error message if any file is missing.

### 3. Deploy

```bash
# Set your image registry first
sed -i 's|<YOUR_IMAGE_REGISTRY>|your-registry.example.com/illumina|g' \
  openshift/deployment-producer.yaml openshift/deployment-consumer.yaml

oc apply -f openshift/deployment-producer.yaml
oc apply -f openshift/deployment-consumer.yaml
```

## Runtime environment variables

| Variable | Default | Description |
|---|---|---|
| `SSL_KEYSTORE_PASSWORD` | — | Injected from the `illumina-jms-tls` secret |
| `SSL_TRUSTSTORE_PASSWORD` | — | Injected from the `illumina-jms-tls` secret |
| `PRODUCER_NAME` | `illumina-producer` | Logical name embedded in sent messages |
| `JAVA_OPTS` | _(empty)_ | Extra JVM flags, e.g. `-Xmx512m` |
| `MQ_TRACE_ENABLED` | `true` | Set to `false` to disable MQ client trace output |
| `MQCLNTCF` | `/app/config/mqclient.ini` | Path to the MQ client ini file (do not change) |

## Security notes

- The container runs as **UID 1001** (non-root), required by OpenShift's default SCC.
- TLS keystores, the CCDT file, and passwords are **never baked into the image** — they are always injected at runtime via an OpenShift Secret.
- Do not commit real `.jks`, `.pem`, `.pfx`, or `.p12` files — they are excluded by `.gitignore`.
