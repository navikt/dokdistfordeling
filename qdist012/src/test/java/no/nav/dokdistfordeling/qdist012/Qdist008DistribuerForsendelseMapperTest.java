package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AktoerTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.ArkivInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DokumentInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.UtenlandskPostadresseTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.UtenlandskPostadresse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.ORGANISASJON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.PERSON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_ARKIVMELDING;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_AVTALEMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class Qdist008DistribuerForsendelseMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String TEMA = "DAG";
	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String ARKIV_SYSTEM = "JOARK";
	private static final String ARKIV_ID = "arkivId";
	private static final String PERSON_NAVN_MOTTAKER = "personNavnMottaker";
	private static final String PERSON_NAVN_BRUKER = "personNavnBruker";
	private static final String MOTTAKER_ID_NAVN = "mottakerIdNavn";
	private static final String ORGANISASJON_NAVN = "organisasjonNavn";
	private static final String SAMHANDLER_NAVN = "samhandlerNavn";
	private static final String PERSON_IDENTIFIKATOR_MOTTAKER = "personIdMottaker";
	private static final String PERSON_IDENTIFIKATOR_BRUKER = "personIdBruker";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ORGNUMMER = "orgnr";
	private static final String SAMHANDLER_IDENTIFIKATOR = "samhandlerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND = "land";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "dokumenttypeId1";
	private static final String DOKUMENTTYPE_ID_2 = "dokumenttypeId2";
	private static final String VARIANTFORMAT_1 = "variantformat1";
	private static final String VARIANTFORMAT_2 = "variantformat2";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";
	private static final int REKKEFOLGE_1 = 1;
	private static final int REKKEFOLGE_2 = 2;

	private final Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper = new Qdist008DistribuerForsendelseMapper();

	@Test
	public void shouldMap() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.distribusjonstype(VEDTAK)
						.distribusjonstidspunkt(KJERNETID)
						.distribusjonKanal(PRINT.name())
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertResponse(distribuerForsendelse);
	}

	@ParameterizedTest
	@MethodSource
	public void shouldMapForsendelseMetadataType(ForsendelseMetadataType forsendelseMetadataType, String forventetForsendelseMetadataType) {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.forsendelseMetadataType(forsendelseMetadataType)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getForsendelseMetadataType()).isEqualTo(forventetForsendelseMetadataType);
	}

	private static Stream<Arguments> shouldMapForsendelseMetadataType() {
		return Stream.of(
				Arguments.of(DPO_ARKIVMELDING, "DPO_ARKIVMELDING"),
				Arguments.of(DPO_AVTALEMELDING, "DPO_AVTALEMELDING"),
				Arguments.of(null, null)
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	@NullSource
	public void shouldMapToNullWhenBatchIdIsBlankOrNull(String batchId) {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.batchId(batchId)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getBatchId()).isNull();
	}

	@Test
	public void shouldMapOkWhenForsendelseTittelIsNull() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.forsendelseTittel(null)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getForsendelseTittel()).isNull();
	}

	@Test
	public void shouldMapOkWhenArkivinformasjonIsNull() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.arkivInformasjon(null)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon()).isNull();
	}

	@Test
	public void shouldMapAktoerId() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerAktoerId(MOTTAKER_ID_NAVN, MOTTAKER_ID))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();

		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isInstanceOf(AktoerId.class);
		final AktoerId mottaker = (AktoerId) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertThat(mottaker.getAktoerId()).isEqualTo(MOTTAKER_ID);
		assertThat(mottaker.getNavn()).isEqualTo(MOTTAKER_ID_NAVN);
	}

	@Test
	public void shouldMapOrganisasjon() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerOrganisasjon(ORGANISASJON_NAVN, ORGNUMMER))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();

		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isInstanceOf(Organisasjon.class);
		final Organisasjon organisasjon = (Organisasjon) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertThat(organisasjon.getOrgnummer()).isEqualTo(ORGNUMMER);
		assertThat(organisasjon.getNavn()).isEqualTo(ORGANISASJON_NAVN);
	}

	@ParameterizedTest
	@MethodSource
	public void shouldMapSamhandler(AktoerTypeCode aktoerTypeCode, String samhandlerkategori) {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerSamhandler(SAMHANDLER_NAVN, SAMHANDLER_IDENTIFIKATOR, aktoerTypeCode))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();

		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling().getMottaker()).isInstanceOf(Samhandler.class);
		final Samhandler samhandler = (Samhandler) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertThat(samhandler.getSamhandleridentifikator()).isEqualTo(SAMHANDLER_IDENTIFIKATOR);
		assertThat(samhandler.getNavn()).isEqualTo(SAMHANDLER_NAVN);
		assertThat(samhandler.getSamhandlerkategori()).isEqualTo(samhandlerkategori);
	}

	private static Stream<Arguments> shouldMapSamhandler() {
		return Stream.of(
				Arguments.of(SAMHANDLER_HPR, "HPR"),
				Arguments.of(SAMHANDLER_UKJENT, "UKJENT"),
				Arguments.of(SAMHANDLER_UTL_ORG, "UTL_ORG")
		);
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.distribusjonKanal(PRINT.name())
						.adresse(createUtenlandskPostadresse())
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();

		assertThat(distribuerForsendelse.getDistribusjonbestilling().getAdresse()).isInstanceOf(UtenlandskPostadresse.class);
		final UtenlandskPostadresse postadresse = (UtenlandskPostadresse) distribuerForsendelse.getDistribusjonbestilling().getAdresse();
		assertThat(postadresse.getAdresselinje1()).isEqualTo(ADRESSELINJE_1);
		assertThat(postadresse.getAdresselinje2()).isEqualTo(ADRESSELINJE_2);
		assertThat(postadresse.getAdresselinje3()).isEqualTo(ADRESSELINJE_3);
		assertThat(postadresse.getLand()).isEqualTo(LAND);
	}

	private void assertResponse(DistribuerForsendelse distribuerForsendelse) {
		assertThat(distribuerForsendelse).isNotNull();
		assertThat(distribuerForsendelse.getDistribusjonbestilling()).isNotNull();

		//assert DistribuerForsendelse
		final Distribusjonbestilling distBestilling = distribuerForsendelse.getDistribusjonbestilling();
		assertThat(distBestilling.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
		assertThat(distBestilling.getBatchId()).isEqualTo(BATCH_ID);
		assertThat(distBestilling.getBestillendeFagsystem()).isEqualTo(BESTILLENDE_FAGSYSTEM);
		assertThat(distBestilling.getTema()).isEqualTo(TEMA);
		assertThat(distBestilling.getForsendelseTittel()).isEqualTo(FORSENDELSE_TITTEL);
		assertThat(distBestilling.getDokumentProdApp()).isEqualTo(DOKUMENT_PROD_APP);
		assertThat(distBestilling.getDistribusjonstype()).isEqualTo(VEDTAK.name());
		assertThat(distBestilling.getDistribusjonstidspunkt()).isEqualTo(KJERNETID.name());

		//assert Arkivinformasjon
		assertThat(distBestilling.getArkivInformasjon()).isNotNull();
		assertThat(distBestilling.getArkivInformasjon().getArkivId()).isEqualTo(ARKIV_ID);
		assertThat(distBestilling.getArkivInformasjon().getArkivSystem()).isEqualTo(ARKIV_SYSTEM);

		//assert mottaker Person
		Person mottaker = (Person) distBestilling.getMottaker();
		assertThat(mottaker).isNotNull();
		assertThat(mottaker.getPersonidentifikator()).isEqualTo(PERSON_IDENTIFIKATOR_MOTTAKER);
		assertThat(mottaker.getNavn()).isEqualTo(PERSON_NAVN_MOTTAKER);

		//assert bruker Person
		Person bruker = (Person) distBestilling.getBruker();
		assertThat(bruker).isNotNull();
		assertThat(bruker.getPersonidentifikator()).isEqualTo(PERSON_IDENTIFIKATOR_BRUKER);
		assertThat(bruker.getNavn()).isEqualTo(PERSON_NAVN_BRUKER);

		//assert norsk postadresse
		assertThat(distBestilling.getAdresse()).isInstanceOf(NorskPostadresse.class);
		final NorskPostadresse postadresse = (NorskPostadresse) distBestilling.getAdresse();
		assertThat(postadresse.getAdresselinje1()).isEqualTo(ADRESSELINJE_1);
		assertThat(postadresse.getAdresselinje2()).isEqualTo(ADRESSELINJE_2);
		assertThat(postadresse.getAdresselinje3()).isEqualTo(ADRESSELINJE_3);
		assertThat(postadresse.getPostnummer()).isEqualTo(POSTNUMMER);
		assertThat(postadresse.getPoststed()).isEqualTo(POSTSTED);
		assertThat(postadresse.getLand()).isEqualTo(LAND);

		//assert dokumenter
		assertThat(distBestilling.getDokumenter())
				.extracting(DokumentInformasjon::getDokumenttypeId,
						DokumentInformasjon::getDokumentObjektReferanse,
						DokumentInformasjon::getTilknyttetSom,
						DokumentInformasjon::getArkivDokumentInfoId,
						DokumentInformasjon::getRekkefolge)
				.hasSize(2)
				.containsExactlyInAnyOrder(
						tuple(DOKUMENTTYPE_ID_1, null, TILKNYTTET_SOM_HOVEDDOK, ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1),
						tuple(DOKUMENTTYPE_ID_2, null, TILKNYTTET_SOM_VEDLEGG, ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2)
				);
	}

	private DistribusjonbestillingToBuilder createDistribusjonbestillingToBuilder() {

		return DistribusjonbestillingTo.builder()
				.bestillingsId(BESTILLINGS_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.tema(TEMA)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.arkivInformasjon(ArkivInformasjonTo.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID)
						.build())
				.mottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER))
				.bruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER))
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.dokumenter(Arrays.asList(
						DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_1)
								.variantFormat(VARIANTFORMAT_1)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
								.rekkefolge(REKKEFOLGE_1)
								.build(),
						DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_2)
								.variantFormat(VARIANTFORMAT_2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
								.rekkefolge(REKKEFOLGE_2)
								.build())
				);
	}

	private NorskPostadresseTo createNorskPostadresse() {
		return new NorskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND, POSTNUMMER, POSTSTED);
	}

	private UtenlandskPostadresseTo createUtenlandskPostadresse() {
		return new UtenlandskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND);
	}

	private AktoerTo createAktoerPerson(String navn, String identifikator) {
		return new AktoerTo(identifikator, navn, false, PERSON);
	}

	private AktoerTo createMottakerAktoerId(String navn, String identifikator) {
		return new AktoerTo(identifikator, navn, true, PERSON);
	}

	private AktoerTo createAktoerOrganisasjon(String navn, String identifikasjon) {
		return new AktoerTo(identifikasjon, navn, false, ORGANISASJON);
	}

	private AktoerTo createAktoerSamhandler(String navn, String identifikasjon, AktoerTypeCode aktoerTypeCode) {
		return new AktoerTo(identifikasjon, navn, false, aktoerTypeCode);
	}

}