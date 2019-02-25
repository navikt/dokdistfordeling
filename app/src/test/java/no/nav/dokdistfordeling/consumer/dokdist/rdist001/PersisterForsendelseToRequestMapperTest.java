package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.MottakerTypeCode;
import no.nav.dokdistfordeling.kodeverk.TemaCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.DistribuerForsendelseTo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class PersisterForsendelseToRequestMapperTest {

	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String ARKIV_ID = "arkivId";
	private static final String PERSON_IDENTIFIKATOR = "personId";
	private static final String AKTOER_ID = "aktoerId";
	private static final String ORGNUMMER = "orgnr";
	private static final String SAMHANDLER_IDENTIFIKATOR = "samhandlerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND_NORGE = "land_norge";
	private static final String UTLAND = "utland";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "dokumenttypeId1";
	private static final String DOKUMENTTYPE_ID_2 = "dokumenttypeId2";
	private static final String OBJEKT_REFERANSE_1 = "objektReferanse1";
	private static final String OBJEKT_REFERANSE_2 = "objektReferanse2";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";
	private static final int REKKEFOLGE_1 = 1;
	private static final int REKKEFOLGE_2 = 2;
	private static final String MOTTAKERNAVN = "mottakernavn";
	private static final MottakerTypeCode PERSON_MOTTAKER_TYPE_CODE = MottakerTypeCode.PERSON;
	private static final MottakerTypeCode SAMHANDLER_MOTTAKER_TYPE_CODE = MottakerTypeCode.SAMHANDLER_HPR;
	private static final MottakerTypeCode ORGANISASJON_MOTTAKER_TYPE_CODE = MottakerTypeCode.ORGANISASJON;
	private static final TemaCode TEMA = TemaCode.FS22;
	private static final TilknyttetSomCode TILKNYTTET_SOM_CODE_1 = TilknyttetSomCode.HOVEDDOKUMENT;
	private static final TilknyttetSomCode TILKNYTTET_SOM_CODE_2 = TilknyttetSomCode.VEDLEGG;
	private static final ArkivSystemCode ARKIV_SYSTEM_CODE = ArkivSystemCode.JOARK;
	private static final String DOKUMENTTITTEL = "dokumentTittel";

	private static final DistribusjonsKanalCode DISTRIBUSJONS_KANAL_CODE = DistribusjonsKanalCode.PRINT;

	private PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper = new PersisterForsendelseToRequestMapper();

	@Test
	public void shouldMapWithPerson() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithPerson();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithPerson(persisterForsendelseRequestTo);
	}

	@Test
	public void shouldMapWithAktoerId() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithAktoerId();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithAktoer(persisterForsendelseRequestTo);
	}

	@Test
	public void shouldMapWithOrganisasjonsNr() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithOrganisasjonsNr();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithOrganisasjon(persisterForsendelseRequestTo);
	}

	@Test
	public void shouldMapWithSamhandler() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithSamhandler();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithSamhandler(persisterForsendelseRequestTo);
	}

	@Test
	public void shouldMapWithUtenlandsPostaddresse() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithUtenlands();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithPostaddresseUtenlands(persisterForsendelseRequestTo);

	}

	@Test
	public void shouldMapWithTomTittel() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = createPersisterForsendelseRequestToWithTomTittel();
		assertCommon(persisterForsendelseRequestTo);
		assertResponseWithTomTittel(persisterForsendelseRequestTo);
	}

	// Common
	private void assertCommon(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertEquals(BATCH_ID, persisterForsendelseRequestTo.getBatchId());
		assertEquals(BESTILLENDE_FAGSYSTEM, persisterForsendelseRequestTo.getBestillendeFagsystem());
		assertEquals(BESTILLINGS_ID, persisterForsendelseRequestTo.getBestillingsId());
		assertEquals(DOKUMENT_PROD_APP, persisterForsendelseRequestTo.getDokumentProdApp());
		assertEquals(TEMA, persisterForsendelseRequestTo.getTema());

		assertArkivInformasjonTo(persisterForsendelseRequestTo.getArkivInformasjon());
	}

	private void assertArkivInformasjonTo(PersisterForsendelseRequestTo.ArkivInformasjonTo arkivInformasjonTo) {
		assertEquals(ARKIV_ID, arkivInformasjonTo.getArkivId());
		assertEquals(ARKIV_SYSTEM_CODE, arkivInformasjonTo.getArkivSystem());
	}

	// Person
	private void assertResponseWithPerson(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithPerson(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
	}

	private void assertMottakerWithPerson(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(PERSON_MOTTAKER_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(PERSON_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	// Aktoer
	private void assertResponseWithAktoer(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithAktoer(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
	}

	private void assertMottakerWithAktoer(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(PERSON_MOTTAKER_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(PERSON_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	// Organisasjon
	private void assertResponseWithOrganisasjon(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithOrganisasjon(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
		assertEquals(FORSENDELSE_TITTEL, persisterForsendelseRequestTo.getForsendelseTittel());
	}

	private void assertMottakerWithOrganisasjon(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(ORGANISASJON_MOTTAKER_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(ORGNUMMER, mottaker.getMottakerId());
	}

	// Organisasjon
	private void assertResponseWithSamhandler(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithSamhandler(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
		assertEquals(FORSENDELSE_TITTEL, persisterForsendelseRequestTo.getForsendelseTittel());
	}

	private void assertMottakerWithSamhandler(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(SAMHANDLER_MOTTAKER_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(SAMHANDLER_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	// NorskPostAddresse
	private void assertPostaddresseTo(PersisterForsendelseRequestTo.PostadresseTo postadresseTo) {
		assertEquals(ADRESSELINJE_1, postadresseTo.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresseTo.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresseTo.getAdresselinje3());
		assertEquals(POSTNUMMER, postadresseTo.getPostnummer());
		assertEquals(POSTSTED, postadresseTo.getPoststed());
		assertEquals(LAND_NORGE, postadresseTo.getLandkode());
	}

	// UtenlandsPostAddresse
	private void assertResponseWithPostaddresseUtenlands(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithOrganisasjon(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseUtenlandsTo(persisterForsendelseRequestTo.getPostadresse());
		assertEquals(FORSENDELSE_TITTEL, persisterForsendelseRequestTo.getForsendelseTittel());
	}

	private void assertPostaddresseUtenlandsTo(PersisterForsendelseRequestTo.PostadresseTo postadresseTo) {
		assertEquals(ADRESSELINJE_1, postadresseTo.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresseTo.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresseTo.getAdresselinje3());
		assertEquals(UTLAND, postadresseTo.getLandkode());
	}

	// Tom tittel
	private void assertResponseWithTomTittel(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertMottakerWithPerson(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
		assertEquals(FORSENDELSE_TITTEL, persisterForsendelseRequestTo.getForsendelseTittel());
	}

	private void assertDokumentInformasjon(List<PersisterForsendelseRequestTo.DokumentTo> dokumenter) {

		assertEquals(DOKUMENTTYPE_ID_1, dokumenter.get(0).getDokumenttypeId());
		assertEquals(ARKIV_DOKUMENTINFO_ID_1, dokumenter.get(0).getArkivDokumentInfoId());
		assertEquals(TILKNYTTET_SOM_CODE_1, dokumenter.get(0).getTilknyttetSom());
		assertEquals(ARKIV_DOKUMENTINFO_ID_1, dokumenter.get(0).getArkivDokumentInfoId());
		assertEquals(REKKEFOLGE_1, dokumenter.get(0).getRekkefolge());

		assertEquals(DOKUMENTTYPE_ID_2, dokumenter.get(1).getDokumenttypeId());
		assertEquals(ARKIV_DOKUMENTINFO_ID_2, dokumenter.get(1).getArkivDokumentInfoId());
		assertEquals(TILKNYTTET_SOM_CODE_2, dokumenter.get(1).getTilknyttetSom());
		assertEquals(ARKIV_DOKUMENTINFO_ID_2, dokumenter.get(1).getArkivDokumentInfoId());
		assertEquals(REKKEFOLGE_2, dokumenter.get(1).getRekkefolge());
	}

	private List<DistribuerForsendelseTo.DokumentInformasjonTo> createDokumentInformasjonToListe() {
		return Arrays.asList(DistribuerForsendelseTo.DokumentInformasjonTo.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_1)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
						.tilknyttetSom(TILKNYTTET_SOM_CODE_1)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
						.rekkefolge(REKKEFOLGE_1)
						.build(),
				DistribuerForsendelseTo.DokumentInformasjonTo.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_2)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_2)
						.tilknyttetSom(TILKNYTTET_SOM_CODE_2)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
						.rekkefolge(REKKEFOLGE_2)
						.build());
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithPerson() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createNorskPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithPerson())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.dokumentTittel(DOKUMENTTITTEL)
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithAktoerId() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createNorskPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithAktoerId())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.dokumentTittel(DOKUMENTTITTEL)
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithSamhandler() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createNorskPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithSamhandler())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.dokumentTittel(DOKUMENTTITTEL)
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithOrganisasjonsNr() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createNorskPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithOrganisasjonsNr())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.dokumentTittel(DOKUMENTTITTEL)
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithUtenlands() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createUtenlandsPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithOrganisasjonsNr())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.dokumentTittel(DOKUMENTTITTEL)
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}

	private PersisterForsendelseRequestTo createPersisterForsendelseRequestToWithTomTittel() {
		return persisterForsendelseToRequestMapper.map(
				DistribuerForsendelseTo.DistribusjonbestillingTo
						.builder()
						.adresse(createNorskPostadresseTo())
						.arkivInformasjon(createArkivInformasjonTo())
						.batchId(BATCH_ID)
						.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.bestillingsId(BESTILLINGS_ID)
						.dokumenter(createDokumentInformasjonToListe())
						.dokumentProdApp(DOKUMENT_PROD_APP)
						.forsendelseTittel(FORSENDELSE_TITTEL)
						.mottaker(createMottakerToWithPerson())
						.tema(TemaCode.FS22)
						.build()
				,
				DokumenttypeInfoTo.builder()
						.build(),
				HentIdentForAktoerIdResponseTo.builder()
						.foedselsnr(PERSON_IDENTIFIKATOR)
						.build(),
				DISTRIBUSJONS_KANAL_CODE);
	}


	private DistribuerForsendelseTo.MottakerTo createMottakerToWithPerson() {
		return DistribuerForsendelseTo.MottakerTo.builder()
				.identifikator(PERSON_IDENTIFIKATOR)
				.identifikatorAktoerId(false)
				.mottakerType(PERSON_MOTTAKER_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.MottakerTo createMottakerToWithAktoerId() {
		return DistribuerForsendelseTo.MottakerTo.builder()
				.identifikator(AKTOER_ID)
				.identifikatorAktoerId(true)
				.mottakerType(PERSON_MOTTAKER_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.MottakerTo createMottakerToWithSamhandler() {
		return DistribuerForsendelseTo.MottakerTo.builder()
				.identifikator(SAMHANDLER_IDENTIFIKATOR)
				.identifikatorAktoerId(false)
				.mottakerType(SAMHANDLER_MOTTAKER_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}


	private DistribuerForsendelseTo.MottakerTo createMottakerToWithOrganisasjonsNr() {
		return DistribuerForsendelseTo.MottakerTo.builder()
				.identifikator(ORGNUMMER)
				.identifikatorAktoerId(false)
				.mottakerType(ORGANISASJON_MOTTAKER_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.ArkivInformasjonTo createArkivInformasjonTo() {
		return DistribuerForsendelseTo.ArkivInformasjonTo.builder()
				.arkivSystem(ArkivSystemCode.JOARK)
				.arkivId(ARKIV_ID)
				.build();
	}

	private DistribuerForsendelseTo.NorskPostadresseTo createNorskPostadresseTo() {
		return DistribuerForsendelseTo.NorskPostadresseTo.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.land(LAND_NORGE)
				.build();
	}

	private DistribuerForsendelseTo.UtenlandskPostadresseTo createUtenlandsPostadresseTo() {
		return DistribuerForsendelseTo.UtenlandskPostadresseTo.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.land(UTLAND)
				.build();
	}
}