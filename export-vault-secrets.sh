#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistfordeling/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistfordeling/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistfordeling/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdokdistfordeling/password)
fi

echo "Exporting appdynamics environment variables"
if test -f /var/run/secrets/nais.io/appdynamics/appdynamics.env;
then
    export $(cat /var/run/secrets/nais.io/appdynamics/appdynamics.env)
    echo "Appdynamics environment variables exported"
else
    echo "No such file or directory found at /var/run/secrets/nais.io/appdynamics/appdynamics.env"
fi

if test -f /var/run/secrets/nais.io/vault/gcloud_serviceaccount
then
    echo "Setting GOOGLE_APPLICATION_CREDENTIALS"
    export GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/nais.io/vault/gcloud_serviceaccount
fi

if test -f /var/run/secrets/nais.io/certificate/keystore
then
    echo "Setting DOKDISTFORDELING_CERT_KEYSTORE"
    CERT_PATH='/var/run/secrets/nais.io/certificate/keystore-extracted'
    openssl base64 -d -A -in /var/run/secrets/nais.io/certificate/keystore -out $CERT_PATH
    export DOKDISTFORDELING_CERT_KEYSTORE=$CERT_PATH
fi

if test -f /var/run/secrets/nais.io/certificate/keystorepassword
then
    echo "Setting DOKDISTFORDELING_CERT_KEYSTORE_PASSWORD"
    export DOKDISTFORDELING_CERT_KEYSTORE_PASSWORD=$(cat /var/run/secrets/nais.io/certificate/keystorepassword)
fi