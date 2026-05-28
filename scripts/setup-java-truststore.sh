#!/usr/bin/env bash
# Import Zscaler Root CA (macOS keychain) into a project-local Java truststore for Maven.
# Required on RingCentral laptops where HTTPS is inspected (PKIX errors on repo.maven.apache.org).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_PEM="${ROOT_DIR}/config/zscaler-root-ca.pem"
TRUSTSTORE="${ROOT_DIR}/config/java-truststore.jks"
STORE_PASS="${DSG_TRUSTSTORE_PASSWORD:-changeit}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "ERROR: Set JAVA_HOME to the JDK Maven uses (e.g. export JAVA_HOME=\$(/usr/libexec/java_home -v 17))" >&2
  exit 1
fi

SYSTEM_CACERTS="${JAVA_HOME}/lib/security/cacerts"
if [[ ! -f "${SYSTEM_CACERTS}" ]]; then
  echo "ERROR: cacerts not found at ${SYSTEM_CACERTS}" >&2
  exit 1
fi

mkdir -p "${ROOT_DIR}/config"

echo "Exporting Zscaler Root CA from macOS keychain..."
security find-certificate -a -c "Zscaler Root CA" -p > "${CERT_PEM}" 2>/dev/null || {
  echo "ERROR: Could not find 'Zscaler Root CA' in keychain. Ask IT for the corporate root CA PEM." >&2
  exit 1
}

echo "Creating truststore at ${TRUSTSTORE} (copy of JDK cacerts + Zscaler)..."
cp "${SYSTEM_CACERTS}" "${TRUSTSTORE}"
chmod u+w "${TRUSTSTORE}"

keytool -importcert \
  -alias zscaler-root-ca \
  -file "${CERT_PEM}" \
  -keystore "${TRUSTSTORE}" \
  -storepass "${STORE_PASS}" \
  -noprompt

echo ""
echo "Done. Maven will use this truststore via .mvn/jvm.config"
echo "Run: mvn -pl dsg-messaging verify"
