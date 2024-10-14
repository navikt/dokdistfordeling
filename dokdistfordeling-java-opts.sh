#!/usr/bin/env sh

JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStore=${NAV_TRUSTSTORE_PATH}"
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStorePassword=${NAV_TRUSTSTORE_PASSWORD}"
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStoreType=jks"
JAVA_OPTS="${JAVA_OPTS} -Xmx2048m -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=nais"

export JAVA_OPTS
