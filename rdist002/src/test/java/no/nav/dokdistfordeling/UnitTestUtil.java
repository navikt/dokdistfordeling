package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;

import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;

public class UnitTestUtil {

	public static final Journalposttype JOURNALPOST_TYPE = Journalposttype.U;
	public static final String DOK_TITTEL_1 = "DOK_TITTEL_1";
	public static final String DOK_TITTEL_2 = "DOK_TITTEL_2";
	public static final String BREVKODE = "000001";

	public static final String ADRESSETYPE_NORSK = "norskPostadresse";
	public static final String ADRESSETYPE_UTENLANDSK = "utenlandskPostadresse";
	public static final String ADRESSELINJE1 = "eksempelveien 23 A";
	public static final String ADRESSELINJE2 = "eksempelveien 24 A";
	public static final String ADRESSELINJE3 = "eksempelveien 25 A";
	public static final String POSTSTED = "poststed";
	public static final String POSTNUMMER = "1337";
	public static final String LAND_NO = "NO";
	public static final String LAND_US = "US";

	public static final String JOURNALPOST_ID = "555555555";
	public static final String BATCH_ID = "66666";
	public static final String DOKUMENTTYPEID = "000001";
	public static final String TITTEL = "journalpostTittel";
	public static final String TEMA = "OPP";
	public static final String ARKIV_SYSTEM = "JOARK";
	public static final String MOTTAKER_ID = "09876543210";
	public static final String MOTTAKER_NAVN = "Jan Neimansen";
	public static final String BRUKER_ID = "12345678901";
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

	public static Journalpost.JournalpostBuilder createJournalpostBuilder() {
		return Journalpost.builder()
				.journalposttype(JOURNALPOST_TYPE)
				.journalstatus(FERDIGSTILT)
				.tema(TEMA)
				.tittel(TITTEL)
				.bruker(createBrukerWithFNR())
				.avsenderMottaker(createAvsenderMottaker())
				.dokumenter(createDefaultDokumentInfoList());
	}

	private static Journalpost.AvsenderMottaker createAvsenderMottaker() {
		return Journalpost.AvsenderMottaker.builder()
				.id(MOTTAKER_ID)
				.navn(MOTTAKER_NAVN)
				.build();
	}

	public static Journalpost.Bruker createBrukerWithFNR() {
		return Journalpost.Bruker.builder().id(BRUKER_ID).type(BrukerIdType.FNR).build();
	}

	public static Journalpost.Bruker createBrukerWithOrgnrId() {
		return Journalpost.Bruker.builder().id(ORGNR).type(BrukerIdType.ORGNR).build();
	}

	public Journalpost.Bruker createBrukerWithSamhandlerId() {
		return Journalpost.Bruker.builder().id(SAMHANDLER_ID).type(BrukerIdType.AKTOERID).build();
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
								.variantformat(Variantformat.ARKIV).build(),
						Journalpost.Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(Variantformat.SLADDET).build()));
	}

	public static Journalpost.DokumentInfo.DokumentInfoBuilder createDokumentInfo2Builder() {
		return Journalpost.DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_2)
				.tittel(DOK_TITTEL_2)
				.brevkode(BREVKODE)
				.dokumentstatus(FERDIGSTILT)
				.dokumentvarianter(Arrays.asList(Journalpost.Dokumentvariant.builder()
						.saksbehandlerHarTilgang(true)
						.variantformat(Variantformat.ARKIV).build()));
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresseWithPostSted(String poststed) {
		return createBaseNorskPostadresse(POSTNUMMER, LAND_NO, poststed);
	}
	
	public static DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresseWithPostnummer(String postnummer) {
		return createBaseNorskPostadresse(postnummer, LAND_NO, POSTSTED);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createPostadresseWithLandkode(String landkode) {
		return createBaseNorskPostadresse(POSTNUMMER, landkode, POSTSTED);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresse() {
		return createBaseNorskPostadresse(POSTNUMMER, LAND_NO, POSTSTED);
	}

	private static DistribuerJournalpostRequestTo.AdresseTo createBaseNorskPostadresse(String postnummer, String landkode, String poststed){
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				postnummer,
				poststed,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				landkode
		);
	}

	public static Person createMottaker(){
		return new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createUtenlandskPostadresse() {
		return createUtenlandskPostadresse(ADRESSELINJE1);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createUtenlandskPostadresse(String adresselinje1) {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_UTENLANDSK,
				null,
				null,
				adresselinje1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_US
		);
	}

	public static DistribuerJournalpostRequestTo.AdresseTo createPostadresseAdresstypeNull() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				null,
				null,
				null,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_US
		);
	}

}
