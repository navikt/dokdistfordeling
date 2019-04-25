package no.nav.dokdistfordeling.endpoints;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.dokdistfordeling.melding.qdist012.Adresse;
import no.nav.dokdistfordeling.melding.qdist012.Aktoer;
import no.nav.dokdistfordeling.melding.qdist012.ArkivInformasjon;
import no.nav.dokdistfordeling.melding.qdist012.Distribusjonbestilling;
import no.nav.dokdistfordeling.melding.qdist012.DokumentInformasjon;
import no.nav.dokdistfordeling.melding.qdist012.HentDokumenterFraJoark;
import no.nav.dokdistfordeling.melding.qdist012.NorskPostadresse;
import no.nav.dokdistfordeling.melding.qdist012.Organisasjon;
import no.nav.dokdistfordeling.melding.qdist012.Person;
import no.nav.dokdistfordeling.melding.qdist012.Samhandler;
import no.nav.dokdistfordeling.melding.qdist012.UtenlandskPostadresse;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.informasjon.arkiverdokumentproduksjon.JournalpostType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private static final String NORSK_POSTADRESSE = "norskPostadresse";
	private static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";
	private static final String UTGAAENDE = JournalpostType.U.name();
	private static final String FERDIGSTILT = Journalstatus.FERDIGSTILT.name();

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

		validateRequest(distribuerJournalpostRequestTo);

		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);
		validateJournalpostAndDokumenter(journalpost);

		Aktoer mottaker = mapMottaker(journalpost.getAvsenderMottaker());
		validateAdresse(distribuerJournalpostRequestTo.getAdresse(), mottaker);

		List<DokumentInfo> dokumenter = journalpost.getDokumenter();
		DokumentInfo hovedDokumentInfo = dokumenter.iterator().next();

		// brevkode for utgående dokumenter tilsvarer dokumenttypeid
		dokumentkatalogAdmin.getDokumenttypeInfo(hovedDokumentInfo.getBrevkode());

		String bestillingsId = UUID.randomUUID().toString();

		distribuerForsendelseProducer.produce(createHentDokumenterFraJoarkBestilling(distribuerJournalpostRequestTo, journalpost, mottaker, dokumenter, bestillingsId), bestillingsId);

		return bestillingsId;
	}


	private void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		try {
			assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
			assertNotNullOrEmpty("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
			assertNotNullOrEmpty("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
		} catch (ValidationException e) {
			log.warn("Validering av distribuerJournalpostRequest: " + e.getMessage());
			throw new ValidationException("Validering av distribuerJournalpostRequest feilet.", e);
		}
	}

	private void validateAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo, Aktoer mottaker) {

		try {
			if (mottaker instanceof Samhandler) {
				assertNotNull(DistribuerJournalpostRequestTo.AdresseTo.class, adresseTo);
			}

			if (adresseTo != null) {
				assertNotNullOrEmpty("land", adresseTo.getLand());

				switch (adresseTo.getAdresseType()) {
					case NORSK_POSTADRESSE:
						assertNotNullOrEmpty("poststed", adresseTo.getPoststed());
						assertNotNullOrEmpty("postnummer", adresseTo.getPostnummer());
						break;
					case UTENLANDSK_POSTADRESSE:
						assertNotNullOrEmpty("adresselinje1", adresseTo.getAdresselinje1());
						break;
					default:
						throw new ValidationException(String.format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, mottok %s", adresseTo.getAdresseType()));
				}
			}
		} catch (ValidationException e) {
			log.warn("Validering av adresse feilet: " + e.getMessage());
			throw new ValidationException("Validering av adresse for mottaker feilet.", e);
		}
	}

	private void validateJournalpostAndDokumenter(Journalpost journalpost) {
		try {
			assertParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
			assertParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus().name(), FERDIGSTILT);
			assertNotNull(Bruker.class, journalpost.getBruker());
			assertNotNull(AvsenderMottaker.class, journalpost.getAvsenderMottaker());

			validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

			journalpost.getDokumenter().forEach(this::validateDokumentInfo);

		} catch (ValidationException e) {
			log.warn("Validering av journalpost mottatt fra saf feilet: " + e.getMessage());
			throw new ValidationException(String.format("Validering av journalpost mottatt fra saf feilet. %s", e.getMessage()), e);
		}
	}


	private void validateHovedDokumentInfo(DokumentInfo dokumentInfo) {
		assertNotNullOrEmpty("tittel", dokumentInfo.getTittel());
		assertNotNullOrEmpty("brevkode", dokumentInfo.getBrevkode());
	}

	private void validateDokumentInfo(DokumentInfo dokumentInfo) {
		assertParameterIsAsExpected("dokumentstatus", dokumentInfo.getDokumentstatus().name(), FERDIGSTILT);

		if (dokumentInfo.getDokumentvarianter().stream().noneMatch(dokInfo -> dokInfo.isSaksbehandlerHarTilgang() && (dokInfo.getVariantformat() == Variantformat.ARKIV || dokInfo.getVariantformat() == Variantformat.SLADDET))) {
			log.warn("Validering av journalpost mottatt fra saf feilet, ingen variantformater av dokumentet med tilgang for saksbehandler ble funnet.");
			throw new ValidationException("Validering av dokumentInfo feilet, ingen variantformater av dokumentet med tilgang for saksbehandler ble funnet.");
		}
	}

	private HentDokumenterFraJoark createHentDokumenterFraJoarkBestilling(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost, Aktoer mottaker, List<DokumentInfo> dokumenter, String bestillingsId) {
		return new HentDokumenterFraJoark()
				.withDistribusjonbestilling(
						new Distribusjonbestilling()
								.withBestillingsId(bestillingsId)
								.withBatchId(distribuerJournalpostRequestTo.getBatchId())
								.withBestillendeFagsystem(distribuerJournalpostRequestTo.getBestillendeFagsystem())
								.withTema(journalpost.getTema())
								.withForsendelseTittel(journalpost.getTittel())
								.withArkivInformasjon(
										new ArkivInformasjon()
												.withArkivId(distribuerJournalpostRequestTo.getJournalpostId())
												.withArkivSystem(journalpost.getTema())
								)
								.withMottaker(mottaker)
								.withBruker(mapBruker(journalpost.getBruker()))
								.withAdresse(mapAdresse(distribuerJournalpostRequestTo.getAdresse()))
								.withDokumentProdApp(distribuerJournalpostRequestTo.getDokumentProdApp())
								.withDokumenter(IntStream
										.range(0, dokumenter.size())
										.mapToObj(i -> {
											DokumentInfo dokumentInfo = dokumenter.get(i);
											return new DokumentInformasjon()
													.withDokumenttypeId(dokumenter.get(0).getBrevkode())
													.withTilknyttetSom(i == 0 ? HOVEDDOKUMENT.name() : VEDLEGG.name())
													.withVariantFormat(
															dokumentInfo.getDokumentvarianter().stream()
																	.anyMatch(dokumentvariant -> (dokumentvariant.getVariantformat() == SLADDET && dokumentvariant.isSaksbehandlerHarTilgang()))
																	? Variantformat.SLADDET.name() : Variantformat.ARKIV.name())
													.withArkivDokumentInfoId(dokumentInfo.getDokumentInfoId())
													.withRekkefolge(i + 1);
										})
										.collect(Collectors.toList()))
				);
	}

	private Adresse mapAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (adresseTo.getAdresseType().equals(NORSK_POSTADRESSE)) {
			return new NorskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withPostnummer(adresseTo.getPostnummer())
					.withPoststed(adresseTo.getPostnummer())
					.withLand(adresseTo.getLand());
		} else {
			return new UtenlandskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withLand(adresseTo.getLand());
		}
	}

	private Aktoer mapMottaker(AvsenderMottaker avsenderMottaker) {
		// todo replace when saf offers AvsenderMottakerType field
		if (avsenderMottaker.getId().length() == 11) {
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

	private Aktoer mapBruker(Bruker bruker) {
		if (bruker.getId().length() == 11) {
			return new Person()
					.withPersonidentifikator(bruker.getId());
		} else if (bruker.getId().length() == 9) {
			return new Organisasjon()
					.withOrgnummer(bruker.getId());
		} else {
			new Samhandler()
					.withSamhandleridentifikator(bruker.getId());
		}
		return null;
	}
}
