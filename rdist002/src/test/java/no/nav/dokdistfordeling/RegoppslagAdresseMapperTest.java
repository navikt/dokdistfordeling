package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE1;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE2;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE3;
import static no.nav.dokdistfordeling.UnitTestUtil.LAND_NO;
import static no.nav.dokdistfordeling.UnitTestUtil.LAND_US;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTNUMMER;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTSTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class RegoppslagAdresseMapperTest {
    private final RegoppslagAdresseMapper regoppslagAdresseMapper = new RegoppslagAdresseMapper();

    @Test
    void shouldMapNorskAdresse() {
        final DistribuerJournalpostRequestTo.AdresseTo adresseTo = regoppslagAdresseMapper.mapAdresseTo(createNorskAdresse());
        assertThat(adresseTo.getAdresselinje1()).isEqualTo(ADRESSELINJE1);
        assertThat(adresseTo.getAdresselinje2()).isEqualTo(ADRESSELINJE2);
        assertThat(adresseTo.getAdresselinje3()).isEqualTo(ADRESSELINJE3);
        assertThat(adresseTo.getPostnummer()).isEqualTo(POSTNUMMER);
        assertThat(adresseTo.getPoststed()).isEqualTo(POSTSTED);
        assertThat(adresseTo.getLand()).isEqualTo(LAND_NO);
        assertThat(adresseTo.getAdressetype()).isEqualTo(NORSK_POSTADRESSE);
    }

    @Test
    void shouldMapBlankNorskAdresselinje1ToNull() {
        final DistribuerJournalpostRequestTo.AdresseTo adresseTo = regoppslagAdresseMapper.mapAdresseTo(createNorskAdresseWithBlank());
        assertThat(adresseTo.getAdresselinje1()).isEqualTo(null);
        assertThat(adresseTo.getAdresselinje2()).isEqualTo(ADRESSELINJE2);
        assertThat(adresseTo.getAdresselinje3()).isEqualTo(ADRESSELINJE3);
        assertThat(adresseTo.getPostnummer()).isEqualTo(POSTNUMMER);
        assertThat(adresseTo.getPoststed()).isEqualTo(POSTSTED);
        assertThat(adresseTo.getLand()).isEqualTo(LAND_NO);
        assertThat(adresseTo.getAdressetype()).isEqualTo(NORSK_POSTADRESSE);
    }

    @Test
    void shouldMapUtenlandskAddresse() {
        final DistribuerJournalpostRequestTo.AdresseTo adresseTo = regoppslagAdresseMapper.mapAdresseTo(createUtenlandskAdresse());
        assertThat(adresseTo.getAdresselinje1()).isEqualTo(ADRESSELINJE1);
        assertThat(adresseTo.getAdresselinje2()).isEqualTo(ADRESSELINJE2);
        assertThat(adresseTo.getAdresselinje3()).isEqualTo(ADRESSELINJE3);
        assertThat(adresseTo.getPostnummer()).isNull();
        assertThat(adresseTo.getPoststed()).isNull();
        assertThat(adresseTo.getLand()).isEqualTo(LAND_US);
        assertThat(adresseTo.getAdressetype()).isEqualTo(UTENLANDSK_POSTADRESSE);
    }

    private HentMottakerOgAdresseResponseTo.AdresseTo createNorskAdresse() {
        return HentMottakerOgAdresseResponseTo.AdresseTo.builder()
                .adresselinje1(ADRESSELINJE1)
                .adresselinje2(ADRESSELINJE2)
                .adresselinje3(ADRESSELINJE3)
                .postnummer(POSTNUMMER)
                .poststed(POSTSTED)
                .landkode(LAND_NO)
                .build();
    }

    private HentMottakerOgAdresseResponseTo.AdresseTo createNorskAdresseWithBlank() {
        return HentMottakerOgAdresseResponseTo.AdresseTo.builder()
                .adresselinje1("")
                .adresselinje2(ADRESSELINJE2)
                .adresselinje3(ADRESSELINJE3)
                .postnummer(POSTNUMMER)
                .poststed(POSTSTED)
                .landkode(LAND_NO)
                .build();
    }

    private HentMottakerOgAdresseResponseTo.AdresseTo createUtenlandskAdresse() {
        return HentMottakerOgAdresseResponseTo.AdresseTo.builder()
                .adresselinje1(ADRESSELINJE1)
                .adresselinje2(ADRESSELINJE2)
                .adresselinje3(ADRESSELINJE3)
                .landkode(LAND_US)
                .build();
    }
}