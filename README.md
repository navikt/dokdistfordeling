#dokdistfordeling

* [Funksjonelle Krav](#1-funksjonelle-krav)
* [Distribusjon av tjenesten (deployment)](#2-distribusjon-av-tjenesten-deployment)
* [Utviklingsmiljø](#3-utviklingsmilj)
* [Drift og støtte](#4-drift-og-sttte)

## Funksjonelle krav
Dokdistfordeling gjør det mulig å distribuere journalposter. 

For mer informasjon: [confluence](https://confluence.adeo.no/display/BOA/dokdistfordeling)


## Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort av Jenkins:
[regoppslag CI / CD](https://dok-jenkins.adeo.no/job/dokdistfordeling/job/master/)
Push/merge til masterbranch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.


## Utviklingsmiljø
### Forutsetninger
* Java 11
* Kubectl
* Maven

### Kjøre prosjektet lokalt
For å kjøre opp applikasjonen lokal, bruk profile `nais` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/list/dokument/dokdistfordeling/) 

### Bygge app.jar og kjøre tester
`mvn clean package`/`mvn clean install`


## Drift og støtte
### Logging
Loggene til tjenesten kan leses på to måter:

### Kibana
For [dev-fss](https://logs.adeo.no/goto/5630b72b08c476476ba26a1360c738b9)

For [prod-fss](https://logs.adeo.no/goto/47ebcd03a7317e0cbb88057206bec786)

### Kubectl
For dev-fss:
```shell script
kubectl config use-context dev-fss
kubectl get pods -n q1 -l app=dokdistfordeling
kubectl logs -f dokdistfordeling-<POD-ID> -n teamdokumenthandtering -c dokdistfordeling
```

For prod-fss:
```shell script
kubectl config use-context prod-fss
kubectl get pods -l app=dokdistfordeling
kubectl logs -f dokdistfordeling-<POD-ID> -n teamdokumenthandtering -c dokdistfordeling
```

### Henvendelser
Spørsmål til koden eller prosjektet kan rettes til Team Dokumentløsninger på:
* [\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)



