#!/bin/sh
# Removes the Java trust-store roots named by https://openjdk.org/emudhra (CVE-2026-91403).
# Interim until a Temurin 8 release with the eMudhra fix is published.
set -eu

cacerts="${JAVA_HOME}/lib/security/cacerts"
roots='emSign ECC Root CA - G3;AAA Certificate Services;emSign Root CA - G1;emSign Root CA - G2'

trust_store() {
  keytool -list -v -keystore "$cacerts" -storepass changeit
}

aliases_to_remove() {
  printf '%s\n' "$1" | awk -v roots="$roots" '
    BEGIN { count = split(roots, names, ";") }
    /^Alias name: / { alias = substr($0, 13) }
    /^Owner: / {
      owner = ", " substr($0, 8) ","
      for (i = 1; i <= count; i++)
        if (index(owner, ", CN=" names[i] ",")) print alias
    }'
}

listing=$(trust_store)
aliases_to_remove "$listing" | while IFS= read -r alias; do
  echo "Distrusting $alias"
  keytool -delete -keystore "$cacerts" -storepass changeit -alias "$alias"
done

listing=$(trust_store)
remaining=$(aliases_to_remove "$listing")
if [ -n "$remaining" ]; then
  printf 'Still trusted:\n%s\n' "$remaining" >&2
  exit 1
fi
case "$listing" in
  *trustedCertEntry*) ;;
  *)
    echo 'Trust store is empty' >&2
    exit 1
    ;;
esac
