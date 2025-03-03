package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo.AdresseTo;
import no.nav.dokdistfordeling.domain.Adresse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.dokdistfordeling.TestData.ADRESSELINJE1;
import static no.nav.dokdistfordeling.TestData.ADRESSELINJE2;
import static no.nav.dokdistfordeling.TestData.ADRESSELINJE3;
import static no.nav.dokdistfordeling.TestData.LAND_NO;
import static no.nav.dokdistfordeling.TestData.LAND_US;
import static no.nav.dokdistfordeling.TestData.POSTNUMMER;
import static no.nav.dokdistfordeling.TestData.POSTSTED;
import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static org.assertj.core.api.Assertions.assertThat;

class RegoppslagAdresseMapperTest {

    @Test
    void shouldMapNorskAdresse() {
        AdresseTo adresseTo = createNorskAdresseBuilder().build();

        Adresse adresse = RegoppslagAdresseMapper.map(adresseTo);

        assertThat(adresse.adresselinje1()).isEqualTo(ADRESSELINJE1);
        assertThat(adresse.adresselinje2()).isEqualTo(ADRESSELINJE2);
        assertThat(adresse.adresselinje3()).isEqualTo(ADRESSELINJE3);
        assertThat(adresse.postnummer()).isEqualTo(POSTNUMMER);
        assertThat(adresse.poststed()).isEqualTo(POSTSTED);
        assertThat(adresse.land()).isEqualTo(LAND_NO);
        assertThat(adresse.adressetype()).isEqualTo(NORSK_POSTADRESSE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    @NullSource
    void shouldMapBlankNorskAdresselinje1ToNull(String adresselinje1) {
        AdresseTo adresseTo = createNorskAdresseBuilder()
                .adresselinje1(adresselinje1)
                .build();

        Adresse adresse = RegoppslagAdresseMapper.map(adresseTo);

        assertThat(adresse.adresselinje1()).isNull();
    }

    @Test
    void shouldMapUtenlandskAddresse() {
        Adresse adresse = RegoppslagAdresseMapper.map(createUtenlandskAdresse());

        assertThat(adresse.adresselinje1()).isEqualTo(ADRESSELINJE1);
        assertThat(adresse.adresselinje2()).isEqualTo(ADRESSELINJE2);
        assertThat(adresse.adresselinje3()).isEqualTo(ADRESSELINJE3);
        assertThat(adresse.postnummer()).isNull();
        assertThat(adresse.poststed()).isNull();
        assertThat(adresse.land()).isEqualTo(LAND_US);
        assertThat(adresse.adressetype()).isEqualTo(UTENLANDSK_POSTADRESSE);
    }

    private AdresseTo.AdresseToBuilder createNorskAdresseBuilder() {
        return AdresseTo.builder()
                .adresselinje1(ADRESSELINJE1)
                .adresselinje2(ADRESSELINJE2)
                .adresselinje3(ADRESSELINJE3)
                .postnummer(POSTNUMMER)
                .poststed(POSTSTED)
                .landkode(LAND_NO);
    }

    private AdresseTo createUtenlandskAdresse() {
        return AdresseTo.builder()
                .adresselinje1(ADRESSELINJE1)
                .adresselinje2(ADRESSELINJE2)
                .adresselinje3(ADRESSELINJE3)
                .landkode(LAND_US)
                .build();
    }
}