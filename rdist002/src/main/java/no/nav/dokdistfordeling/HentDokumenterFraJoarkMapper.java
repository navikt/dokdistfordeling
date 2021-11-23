package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class HentDokumenterFraJoarkMapper {

	public static final String NORSK_POSTADRESSE = "norskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	public HentDokumenterFraJoark map(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost,
									  Aktoer mottaker, String bestillingsId, DistribusjonsKanalCode distribusjonsKanal) {
		List<Journalpost.DokumentInfo> dokumenter = journalpost.getDokumenter();
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling()
				.withBestillingsId(bestillingsId)
				.withBatchId(distribuerJournalpostRequestTo.getBatchId())
				.withDistribusjonKanal(distribusjonsKanal.name())
				.withBestillendeFagsystem(distribuerJournalpostRequestTo.getBestillendeFagsystem())
				.withTema(journalpost.getTema())
				.withForsendelseTittel(journalpost.getTittel())
				.withArkivInformasjon(
						new ArkivInformasjon()
								.withArkivId(distribuerJournalpostRequestTo.getJournalpostId())
								.withArkivSystem(ArkivSystemCode.JOARK.name())
				)
				.withMottaker(mottaker)
				.withBruker(mapBruker(journalpost.getBruker()))
				.withAdresse(PRINT.name().equals(distribusjonsKanal.name()) ? mapAdresse(distribuerJournalpostRequestTo.getAdresse()) : null)
				.withDokumentProdApp(distribuerJournalpostRequestTo.getDokumentProdApp())
				.withDokumenter(IntStream
						.range(0, dokumenter.size())
						.mapToObj(i -> {
							Journalpost.DokumentInfo dokumentInfo = dokumenter.get(i);
							return new DokumentInformasjon()
									.withDokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
									.withTilknyttetSom(i == 0 ? HOVEDDOKUMENT.name() : VEDLEGG.name())
									.withVariantFormat(
											dokumentInfo.getDokumentvarianter().stream()
													.anyMatch(dokumentvariant -> (dokumentvariant.getVariantformat().equals(Variantformat.SLADDET) && dokumentvariant.isSaksbehandlerHarTilgang())) ? SLADDET : ARKIV)
									.withArkivDokumentInfoId(dokumentInfo.getDokumentInfoId())
									.withRekkefolge(i + 1);
						})
						.collect(Collectors.toList()));

		setDistribusjonstidspunkt(distribusjonbestilling, distribuerJournalpostRequestTo.getDistribusjonstidspunkt());
		setDistribusjonstype(distribusjonbestilling, distribuerJournalpostRequestTo.getDistribusjonstype());

		return new HentDokumenterFraJoark()
				.withDistribusjonbestilling(distribusjonbestilling);
	}

	private Adresse mapAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (isNull(adresseTo)) {
			throw new ValidationException("Adresse kan ikke være null");
		} else if (adresseTo.getAdressetype().equals(NORSK_POSTADRESSE)) {
			return new NorskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(isNotBlank(adresseTo.getAdresselinje2()) ? adresseTo.getAdresselinje2() : null)
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withPostnummer(adresseTo.getPostnummer())
					.withPoststed(adresseTo.getPoststed())
					.withLand(adresseTo.getLand());
		} else {
			return new UtenlandskPostadresse()
					.withAdresselinje1(adresseTo.getAdresselinje1())
					.withAdresselinje2(isBlank(adresseTo.getAdresselinje2()) ? null : adresseTo.getAdresselinje2())
					.withAdresselinje3(adresseTo.getAdresselinje3())
					.withLand(adresseTo.getLand());
		}
	}

	private Aktoer mapBruker(Journalpost.Bruker bruker) {
		if (BrukerIdType.FNR.equals(bruker.getType())) {
			return new Person()
					.withPersonidentifikator(bruker.getId());
		} else if (BrukerIdType.AKTOERID.equals(bruker.getType())) {
			return new AktoerId()
					.withAktoerId(bruker.getId());
		} else if (BrukerIdType.ORGNR.equals(bruker.getType())) {
			return new Organisasjon()
					.withOrgnummer(bruker.getId());
		} else {
			throw new ValidationException(String.format("BrukerIdType var ikke som forventet, fikk brukerIdType=%s, men forventet FNR, AKTOERID eller ORGNR", bruker.getType().name()));
		}
	}

	private void setDistribusjonstidspunkt(Distribusjonbestilling distribusjonbestilling, String distribusjonstidspunkt) {
		if (isNotBlank(distribusjonstidspunkt) && isValidEnum(DistribusjonstidspunktCode.class, distribusjonstidspunkt.toUpperCase())) {
			distribusjonbestilling.setDistribusjonstidspunkt(distribusjonstidspunkt.toUpperCase());
		}
	}

	private void setDistribusjonstype(Distribusjonbestilling distribusjonbestilling, String distribusjonstype) {
		if (isNotBlank(distribusjonstype) && isValidEnum(DistribusjonstypeCode.class, distribusjonstype.toUpperCase())) {
			distribusjonbestilling.setDistribusjonstype(distribusjonstype.toUpperCase());
		}
	}
}
