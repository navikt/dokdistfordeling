package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.domain.Postadresse.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.domain.Postadresse.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.UMIDDELBART;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VIKTIG;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.PDF;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.PDFA;

public class TestData {

	public static final Journalposttype JOURNALPOST_TYPE = Journalposttype.U;
	public static final String DOK_TITTEL_1 = "DOK_TITTEL_1";
	public static final String DOK_TITTEL_2 = "DOK_TITTEL_2";
	public static final String BREVKODE = "000001";

	public static final String ADRESSELINJE1 = "eksempelveien 23 A";
	public static final String ADRESSELINJE2 = "eksempelveien 24 A";
	public static final String ADRESSELINJE3 = "eksempelveien 25 A";
	public static final String POSTSTED = "poststed";
	public static final String POSTNUMMER = "1337";
	public static final String LANDKODE_US = "US";

	public static final Long JOURNALPOST_ID = 555555555L;
	public static final String BATCH_ID = "126767";
	public static final String TITTEL = "journalpostTittel";
	public static final String TEMA = "OPP";
	public static final String ARKIV_SYSTEM = "JOARK";
	public static final String MOTTAKER_ID = "09876543210";
	public static final String MOTTAKER_NAVN = "Jan Neimansen";
	public static final String BRUKER_ID = "12345678901";
	public static final String AKTOER_ID = "123456789";
	public static final String ORGNR = "776677665";
	public static final String ORG_NAVN = "eksempelcorp ASA";
	public static final String TSS_ID = "88998899890";
	public static final String TSS_NAVN = "TSS Mottaker";
	public static final String TSS_KATEGORI = "UKJENT";
	public static final String SAMHANDLER_KATOGORI = "HPR";
	public static final String SAMHANDLER_NAVN = "Betina Samhandlerson";
	public static final String SAMHANDLER_ID = "33322211";
	public static final String DOK_INFO_ID_1 = "666666666";
	public static final String DOK_INFO_ID_2 = "777777777";
	public static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
	public static final String DOKUMENTPRODAPP = "dokumentprodapp";
	public static final String FORSENDSELSE_METADATA = "<xml>Some metadata</xml>";
	public static final Long DISTRIBUERT_JOURNALPOST_ID = 111111111L;
	public static final String DISTRIBUERT_BESTILLINGS_ID = UUID.randomUUID().toString();
	public static final String LANDKODE_NORGE = "NO";

	public static DistribuerJournalpost.DistribuerJournalpostBuilder createDistribuerJournalpostBuilder() {
		return DistribuerJournalpost.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.postadresse(createNorskPostadresseBuilder().build())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstype(VEDTAK)
				.distribusjonstidspunkt(UMIDDELBART)
				.tvingSentralPrint(false);
	}

	public static Journalpost.JournalpostBuilder createJournalpostBuilder() {
		return Journalpost.builder()
				.journalposttype(JOURNALPOST_TYPE)
				.journalstatus(FERDIGSTILT)
				.tema(TEMA)
				.tittel(TITTEL)
				.bruker(createBrukerWithFNR())
				.avsenderMottaker(createAvsenderMottakerBuilder().build())
				.dokumenter(createDefaultDokumentInfoList());
	}

	public static Journalpost.AvsenderMottaker.AvsenderMottakerBuilder createAvsenderMottakerBuilder() {
		return Journalpost.AvsenderMottaker.builder()
				.id(MOTTAKER_ID)
				.navn(MOTTAKER_NAVN);
	}

	public static Journalpost.Bruker createBrukerWithFNR() {
		return Journalpost.Bruker.builder()
				.id(BRUKER_ID)
				.type(BrukerIdType.FNR)
				.build();
	}

	public static Journalpost.Bruker createBrukerWithOrgnrId() {
		return Journalpost.Bruker.builder()
				.id(ORGNR)
				.type(BrukerIdType.ORGNR)
				.build();
	}

	public static Journalpost.Bruker createBrukerWithAktoerId() {
		return Journalpost.Bruker.builder()
				.id(AKTOER_ID)
				.type(BrukerIdType.AKTOERID)
				.build();
	}

	public static List<Journalpost.DokumentInfo> createDefaultDokumentInfoList() {
		return Arrays.asList(
				createDokumentInfo1Builder().build(),
				createDokumentInfo2Builder().build());
	}

	public static Journalpost.DokumentInfo.DokumentInfoBuilder createDokumentInfo1Builder() {
		return Journalpost.DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_1)
				.tittel(DOK_TITTEL_1)
				.brevkode(BREVKODE)
				.dokumentstatus(FERDIGSTILT)
				.dokumentvarianter(Arrays.asList(Journalpost.Dokumentvariant.builder()
								.saksbehandlerHarTilgang(false)
								.filtype(PDFA)
								.variantformat(Variantformat.ARKIV).build(),
						Journalpost.Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.filtype(PDF)
								.variantformat(Variantformat.SLADDET).build()));
	}

	public static Journalpost.DokumentInfo.DokumentInfoBuilder createDokumentInfo2Builder() {
		return Journalpost.DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_2)
				.tittel(DOK_TITTEL_2)
				.brevkode(BREVKODE)
				.dokumentstatus(FERDIGSTILT)
				.dokumentvarianter(singletonList(Journalpost.Dokumentvariant.builder()
						.saksbehandlerHarTilgang(true)
						.filtype(PDFA)
						.variantformat(Variantformat.ARKIV).build()));
	}

	private static Postadresse.PostadresseBuilder createPostadresseBuilder() {
		return Postadresse.builder()
				.adresselinje1(ADRESSELINJE1)
				.adresselinje2(ADRESSELINJE2)
				.adresselinje3(ADRESSELINJE3);
	}

	public static Postadresse.PostadresseBuilder createNorskPostadresseBuilder() {
		return createPostadresseBuilder()
				.adressetype(NORSK_POSTADRESSE)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.land(LANDKODE_NORGE);
	}

	public static Postadresse.PostadresseBuilder createUtenlandskPostadresseBuilder() {
		return createPostadresseBuilder()
				.adressetype(UTENLANDSK_POSTADRESSE)
				.land(LANDKODE_US);
	}

	public static Person createMottaker() {
		Person person = new Person();
		person.setNavn(MOTTAKER_NAVN);
		person.setPersonidentifikator(MOTTAKER_ID);
		return person;
	}

	public static DistribuerJournalpostRequestTo createDistribuerJournalpostTo() {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(String.valueOf(DISTRIBUERT_JOURNALPOST_ID))
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskAdresseTo())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.tvingSentralPrint(false)
				.build();
	}

	public static DistribuerJournalpostRequestTo.DistribuerJournalpostRequestToBuilder createDistribuerJournalpostToBuilder() {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(String.valueOf(JOURNALPOST_ID))
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskAdresseTo())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.tvingSentralPrint(false);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo.AdresseToBuilder createAdresseToBuilder() {
		return DistribuerJournalpostRequestTo.AdresseTo.builder()
				.adresselinje1(ADRESSELINJE1)
				.adresselinje2(ADRESSELINJE2)
				.adresselinje3(ADRESSELINJE3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.land(LANDKODE_NORGE);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createNorskAdresseTo() {
		return createAdresseToBuilder()
				.adressetype(NORSK_POSTADRESSE)
				.build();
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createUtenlandskAdresseTo() {
		return createAdresseToBuilder()
				.adressetype(UTENLANDSK_POSTADRESSE)
				.postnummer(null)
				.poststed(null)
				.land(LANDKODE_US)
				.build();
	}

}
