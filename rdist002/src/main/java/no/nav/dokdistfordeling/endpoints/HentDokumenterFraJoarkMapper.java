package no.nav.dokdistfordeling.endpoints;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HentDokumenterFraJoarkMapper {

	public static final String NORSK_POSTADRESSE = "norskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	public HentDokumenterFraJoark map(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost, Aktoer mottaker, List<DokumentInfo> dokumenter, String bestillingsId) {
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
		if (adresseTo == null) {
			return null;
		} else if (adresseTo.getAdresseType().equals(NORSK_POSTADRESSE)) {
			return new NorskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withPostnummer(adresseTo.getPostnummer())
					.withPoststed(adresseTo.getPoststed())
					.withLand(adresseTo.getLand());
		} else {
			return new UtenlandskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withLand(adresseTo.getLand());
		}
	}

	private Aktoer mapBruker(Bruker bruker) {
		if (bruker.getId().trim().length() == 11) {
			return new Person()
					.withPersonidentifikator(bruker.getId());
		} else if (bruker.getId().length() == 9) {
			return new Organisasjon()
					.withOrgnummer(bruker.getId());
		} else {
			return new Samhandler()
					.withSamhandleridentifikator(bruker.getId());
		}
	}
}
