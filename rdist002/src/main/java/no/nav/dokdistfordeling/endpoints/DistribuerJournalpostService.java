package no.nav.dokdistfordeling.endpoints;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.apache.commons.lang3.StringUtils.isBlank;

import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Dokumentvariant;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.Dokumentstatus;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
import no.nav.dokdistfordeling.qdist012.Adresse;
import no.nav.dokdistfordeling.qdist012.Aktoer;
import no.nav.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.dokdistfordeling.qdist012.Organisasjon;
import no.nav.dokdistfordeling.qdist012.Person;
import no.nav.dokdistfordeling.qdist012.Samhandler;
import no.nav.dokdistfordeling.qdist012.UtenlandskPostadresse;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.informasjon.arkiverdokumentproduksjon.JournalpostType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DistribuerJournalpostService {

	private static final String NORSK_POSTADRESSE = "norskPostadresse";
	private static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	private SafJournalpostQueryService safJournalpostQueryService;
	private DistribuerForsendelseProducer distribuerForsendelseProducer;
	private DokumentkatalogAdmin dokumentkatalogAdmin;


	public DistribuerJournalpostService(SafJournalpostQueryService safJournalpostQueryService,
										DistribuerForsendelseProducer distribuerForsendelseProducer,
										DokumentkatalogAdmin dokumentkatalogAdmin) {
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
	}

	public String distribuerForsendelse(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, String authorizationHeader) {

		// steg 1, validering av request
		validateRequest(distribuerJournalpostRequestTo);

		// steg 2, hent journalpost fra saf
		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);

		validateJournalpost(journalpost); // comment for testing
		// steg 3, validering av journalpost

		List<DokumentInfo> dokumenter = journalpost.getDokumenter();

		DokumentInfo hovedDokumentInfo = dokumenter.iterator().next();
		// steg 4, kontroller dokumenter på journalpost

		// steg 5, kontroller dokumenttype, kall dokkat
		dokumentkatalogAdmin.getDokumenttypeInfo(hovedDokumentInfo.getBrevkode()); //comment for testing
//		dokumentkatalogAdmin.getDokumenttypeInfo("000001"); //uncomment for testing

		// generere guid som skal sendes med til qdist008
		String bestillingsId = UUID.randomUUID().toString();

		//  steg 6, legg på kø til qdist012,
		distribuerForsendelseProducer.produce(
				new HentDokumenterFraJoark()
						.withDistribusjonbestilling(
								new Distribusjonbestilling()
										.withArkivInformasjon(
												new ArkivInformasjon()
														.withArkivId(distribuerJournalpostRequestTo.getJournalpostId())
														.withArkivSystem(journalpost.getTema())
										)
										.withDokumenter(IntStream
												.range(0, dokumenter.size())
												.mapToObj(i -> {
													DokumentInfo dokumentInfo = dokumenter.get(i);
													return new DokumentInformasjon()
															.withArkivDokumentInfoId(dokumentInfo.getDokumentInfoId())
															.withDokumenttypeId(hovedDokumentInfo.getBrevkode())
															.withTilknyttetSom(i == 0 ? HOVEDDOKUMENT.name() : VEDLEGG.name())
															.withVariantFormat(dokumentInfo
																	.getDokumentvarianter().stream()
																	.map(Dokumentvariant::getVariantformat)
																	.anyMatch(SLADDET::equals) ? SLADDET.name() : ARKIV.name())
															.withRekkefolge(i + 1);
												})
												.collect(Collectors.toList()))

										.withAdresse(mapAdresse(distribuerJournalpostRequestTo.getAdresse()))
										.withBatchId(distribuerJournalpostRequestTo.getBatchId())
										.withBestillingsId(bestillingsId)
										.withBestillendeFagsystem(distribuerJournalpostRequestTo.getBestillendeFagsystem())
										.withDokumentProdApp(distribuerJournalpostRequestTo.getDokumentProdApp())
										.withForsendelseTittel(journalpost.getTittel())
										.withMottaker(mapMottaker(journalpost.getAvsenderMottaker()))
										.withTema(journalpost.getTema())
						)
		);

		// steg 7, returner bestillingsid til bestiller
		return bestillingsId;
	}


	private void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		try {
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getJournalpostId(), "journalpostId");
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getBestillendeFagsystem(), "bestillendeFagsystem");
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getDokumentProdApp(), "dokumentProdapp");

			// todo assert adresse depends on mottaker being samhandler, adapt when saf offers avsenderMottakerType, might have to be moved
			DistribuerJournalpostRequestTo.AdresseTo adresse = distribuerJournalpostRequestTo.getAdresse();

			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getAdresse().getLand(), "adresse.land");

			switch (distribuerJournalpostRequestTo.getAdresse().getAdresseType()) {
				case NORSK_POSTADRESSE:
					assertNotNullOrEmpty(adresse.getPoststed(), "adresse.poststed for norsk postadresse");
					assertNotNullOrEmpty(adresse.getPostnummer(), "adresse.postnummer for norsk postadresse");
					break;
				case UTENLANDSK_POSTADRESSE:
					assertNotNullOrEmpty(adresse.getAdresselinje1(), "adresse.adresselinje1 for utenlands postadresse");
					break;
				default:
					throw new IllegalArgumentException(String.format("AdresseType må være enten \"norskPostadresse\" eller \"utenlandskPostadresse\", mottok %s", adresse.getAdresseType()));
			}

		} catch (IllegalArgumentException e) {
			throw new ValidationException("Validering av distribuerJournalpostRequest feilet.", e);
		}
	}

	private void validateJournalpost(Journalpost journalpost) {
		try {
			forParameterAssertEquals("journalposttype", journalpost.getJournalposttype().name(), JournalpostType.U.name());
			forParameterAssertEquals("journalpoststatus", journalpost.getJournalstatus().name(), Journalstatus.FERDIGSTILT.name());
			assertNotNullOrEmpty(journalpost.getBruker(), "bruker");
			assertNotNullOrEmpty(journalpost.getAvsenderMottaker(), "avsenderMottaker");

			validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

			journalpost.getDokumenter().forEach(this::validateVedleggDokumentInfo);

		} catch (IllegalArgumentException e) {
			throw new ValidationException("Validering av distribuerJournalpostRequest feilet.", e);
		}
	}

	private void validateHovedDokumentInfo(DokumentInfo dokumentInfo) {
		assertNotNullOrEmpty(dokumentInfo.getTittel(), "dokumentinfo.tittel");
		assertNotNullOrEmpty(dokumentInfo.getBrevkode(), "dokumentinfo.brevkode");
	}

	private void validateVedleggDokumentInfo(DokumentInfo dokumentInfo) {
		forParameterAssertEquals("dokumentinfo.dokumentstatus", dokumentInfo.getDokumentstatus().name(), Dokumentstatus.FERDIGSTILT.name());
//		forParameterAssertEquals("dokumentinfo.dokumentvariant", dokumentInfo.getDokumentvarianter().get(0).getVariantformat().name(), Variantformat.ARKIV.name());
		// todo await more documentation.
	}

	private void assertNotNullOrEmpty(Object value, String parameter) {
		if (Objects.isNull(value) || (value instanceof String && isBlank((String) value))) {
			throw new IllegalArgumentException(String.format("Input mangler påkrevd parameter \"%s\"", parameter));
		}
	}

	private void forParameterAssertEquals(String parameterName, String value, String expected) {
		if (!value.equals(expected)) {
			throw new IllegalArgumentException(String.format("%s er ikke som forventet, fikk: \"%s\", men forventet \"%s\"", parameterName, value, expected));
		}
	}

	private Adresse mapAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (adresseTo.getAdresseType().equals(NORSK_POSTADRESSE)) {
			return new NorskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withLand(adresseTo.getLand())
					.withPostnummer(adresseTo.getPostnummer())
					.withPoststed(adresseTo.getPostnummer());
		} else {
			return new UtenlandskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withLand(adresseTo.getLand());
		}
	}

	private Aktoer mapMottaker(AvsenderMottaker avsenderMottaker) {
		if (avsenderMottaker.getId().length() == 11) { // todo replace when saf offers AvsenderMottakerType field
			return new Person()
					.withNavn(avsenderMottaker.getNavn())
					.withPersonidentifikator(avsenderMottaker.getId());
		} else if (avsenderMottaker.getId().length() == 9) {
			return new Organisasjon()
					.withNavn(avsenderMottaker.getNavn())
					.withOrgnummer(avsenderMottaker.getId());
		} else {
			new Samhandler()
					.withNavn(avsenderMottaker.getNavn())
					.withSamhandleridentifikator(avsenderMottaker.getId());
		}
		return null;
	}

}
