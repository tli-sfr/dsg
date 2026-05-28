# Maven TLS setup (Zscaler / corporate proxy)

## Symptom

```
PKIX path building failed: unable to find valid certification path to requested target
```

when running `mvn verify`, while `curl https://repo.maven.apache.org` works in the browser or curl.

## Cause

RingCentral laptops often use **Zscaler** (or similar) TLS inspection. The JDK truststore (`$JAVA_HOME/lib/security/cacerts`) does not include the Zscaler root CA, so Java/Maven cannot validate Maven Central.

## Fix (recommended — project truststore)

From the repo root:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # match your `mvn -version` JDK
chmod +x scripts/setup-java-truststore.sh
./scripts/setup-java-truststore.sh
mvn -pl dsg-messaging verify
```

This script:

1. Exports **Zscaler Root CA** from the macOS keychain
2. Copies your JDK `cacerts` to `config/java-truststore.jks`
3. Imports the Zscaler CA into that truststore

Maven picks it up via [`.mvn/jvm.config`](../../.mvn/jvm.config) (no sudo required).

Files (gitignored): `config/java-truststore.jks`, `config/zscaler-root-ca.pem`

## Alternative — import into JDK (global)

```bash
sudo keytool -importcert -alias zscaler-root-ca \
  -file config/zscaler-root-ca.pem \
  -keystore "$JAVA_HOME/lib/security/cacerts" \
  -storepass changeit
```

Use only if your team allows modifying the JDK truststore.

## Alternative — internal Maven mirror

If RingCentral provides an Artifactory/Nexus mirror already trusted by Java, add `~/.m2/settings.xml` with `<mirror>` pointing to that repository and remove the need for Zscaler import. Ask platform/IT for the correct `settings.xml` template.

## Verify

```bash
keytool -printcert -sslserver repo.maven.apache.org:443 -rfc | openssl x509 -noout -subject
# Often shows CN=repo.maven.apache.org, O=Zscaler Inc. on corp laptops

# Build from repo root (installs dsg-domain first)
mvn verify
```

Or:

```bash
mvn -pl dsg-domain,dsg-messaging -am verify
```

## Corrupted downloads (`zip END header not found`)

If Zscaler interrupted a download, delete the broken artifact and rebuild:

```bash
rm -rf ~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.15.4
mvn verify
```
