#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistfordeling/username;
then
    echo "Setting DOKDISTFORDELING_SERVICEUSER_USERNAME"
    export DOKDISTFORDELING_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistfordeling/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistfordeling/password;
then
    echo "Setting DOKDISTFORDELING_SERVICEUSER_PASSWORD"
    export DOKDISTFORDELING_SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdokdistfordeling/password)
fi

if test -f /var/run/secrets/nais.io/vault/gcloud_serviceaccount
then
    echo "Setting GOOGLE_APPLICATION_CREDENTIALS"
    export GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/nais.io/vault/gcloud_serviceaccount
fi