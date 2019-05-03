package no.nav.dokdistfordeling.unittest;

import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.PRODUKSJON;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;

import no.nav.dokdistfordeling.consumer.saf.journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Dokumentvariant;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Dokumentstatus;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;

import java.util.Arrays;
import java.util.List;

public class UnitTestUtil {

	public static final Journalposttype JOURNALPOST_TYPE = Journalposttype.U;
	public static final Journalstatus JP_FERDIGSTILT = Journalstatus.FERDIGSTILT;
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
	public static final String MOTTAKER_ID = "***gammelt_fnr***";
	public static final String MOTTAKER_NAVN = "Jan Neimansen";
	public static final String BRUKER_ID = "***gammelt_fnr***";
	public static final String BRUKER_NAVN = "***gammelt_fnr***";
	public static final String ORGNR = "776677665";
	public static final String ORG_NAVN = "eksempelcorp ASA";
	public static final String SAMHANDLER_KATOGORI = "HPR";
	public static final String SAMHANDLER_NAVN = "Betina Samhandlerson";
	public static final String SAMHANDLER_ID = "33322211";
	public static final String DOK_INFO_ID_1 = "666666666";
	public static final String DOK_INFO_ID_2 = "777777777";
	public static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
	public static final String DOKUMENTPRODAPP = "dokumentprodapp";

	public Journalpost.JournalpostBuilder createJournalpostBuilder() {
		return Journalpost.builder()
				.journalposttype(JOURNALPOST_TYPE)
				.journalstatus(JP_FERDIGSTILT)
				.tema(TEMA)
				.tittel(TITTEL)
				.bruker(createBrukerWithFNR())
				.avsenderMottaker(createAvsenderMottaker())
				.dokumenter(createDefaultDokumentInfoList());
	}

	private AvsenderMottaker createAvsenderMottaker() {
		return AvsenderMottaker.builder()
				.id(MOTTAKER_ID)
				.navn(MOTTAKER_NAVN)
				.build();
	}

	public Bruker createBrukerWithFNR() {
		return new Bruker(BRUKER_NAVN, BrukerIdType.FNR);
	}

	public Bruker createBrukerWithOrgnrId() {
		return new Bruker(ORGNR, BrukerIdType.ORGNR);
	}

	public Bruker createBrukerWithSamhandlerId() {
		return new Bruker(SAMHANDLER_ID, BrukerIdType.AKTOERID);
	}

	private Person createPersonMottaker() {
		return new Person()
				.withPersonidentifikator(MOTTAKER_ID)
				.withNavn(MOTTAKER_NAVN);
	}

	public List<DokumentInfo> createDefaultDokumentInfoList() {
		return Arrays.asList(
				createDokumentInfo1Builder().build(),
				createDokumentInfo2Builder().build());
	}

	public DokumentInfo.DokumentInfoBuilder createDokumentInfo1Builder() {
		return DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_1)
				.tittel(DOK_TITTEL_1)
				.brevkode(BREVKODE)
				.dokumentstatus(Dokumentstatus.FERDIGSTILT)
				.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
								.saksbehandlerHarTilgang(false)
								.variantformat(ARKIV).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(SLADDET).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(PRODUKSJON).build()));
	}

	public DokumentInfo.DokumentInfoBuilder createDokumentInfo2Builder() {
		return DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_2)
				.tittel(DOK_TITTEL_2)
				.brevkode(BREVKODE)
				.dokumentstatus(Dokumentstatus.FERDIGSTILT)
				.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(ARKIV).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(PRODUKSJON).build()));
	}

	public DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				POSTNUMMER,
				POSTSTED,
				ADRESSELINJE1,
				null,
				null,
				LAND_NO
		);
	}

	public DistribuerJournalpostRequestTo.AdresseTo createUtenlandskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_UTENLANDSK,
				null,
				null,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_US
		);
	}

}
