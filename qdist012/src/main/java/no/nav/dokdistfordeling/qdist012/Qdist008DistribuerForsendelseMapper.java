package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.exception.functional.DistrubuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.UtenlandskPostadresse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.HPR;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.UTL_ORG;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist008DistribuerForsendelseMapper {

	public DistribuerForsendelse map(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		try {
			return new DistribuerForsendelse()
					.withDistribusjonbestilling(mapDokumentbestillingsinformasjon(hentDokumenterFraJoarkTo.getDistribusjonbestilling()));
		} catch (Exception e) {
			throw new DistrubuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist012 output. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private Distribusjonbestilling mapDokumentbestillingsinformasjon(HentDokumenterFraJoarkTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		return new Distribusjonbestilling()
				.withBestillingsId(distribusjonbestillingTo.getBestillingsId())
				.withBatchId(distribusjonbestillingTo.getBatchId())
				.withDistribusjonKanal(distribusjonbestillingTo.getDistribusjonKanal())
				.withBestillendeFagsystem(distribusjonbestillingTo.getBestillendeFagsystem())
				.withTema(distribusjonbestillingTo.getTema())
				.withForsendelseTittel(distribusjonbestillingTo.getForsendelseTittel())
				.withArkivInformasjon(distribusjonbestillingTo.getArkivInformasjon() == null ? null :
						mapArkivInformasjon(distribusjonbestillingTo.getArkivInformasjon()))
				.withMottaker(mapAktoerTo(distribusjonbestillingTo.getMottaker()))
				.withBruker(mapAktoerTo(distribusjonbestillingTo.getBruker()))
				.withAdresse(PRINT.name().equals(distribusjonbestillingTo.getDistribusjonKanal()) ? mapAdresse(distribusjonbestillingTo.getAdresse()) : null)
				.withDokumentProdApp(distribusjonbestillingTo.getDokumentProdApp())
				.withDokumenter(distribusjonbestillingTo.getDokumenter().stream()
						.map(dokumentInformasjon -> new DokumentInformasjon()
								.withDokumenttypeId(dokumentInformasjon.getDokumenttypeId())
								.withTilknyttetSom(dokumentInformasjon.getTilknyttetSom())
								.withArkivDokumentInfoId(dokumentInformasjon.getArkivDokumentInfoId())
								.withRekkefolge(dokumentInformasjon.getRekkefolge())
								.withDokumentObjektReferanse(dokumentInformasjon.getDokumentObjektReferanse()))
						.collect(Collectors.toList()));
	}

	private ArkivInformasjon mapArkivInformasjon(HentDokumenterFraJoarkTo.ArkivInformasjonTo arkivInformasjonTo) {
		return new ArkivInformasjon()
				.withArkivSystem(arkivInformasjonTo.getArkivSystem())
				.withArkivId(arkivInformasjonTo.getArkivId());
	}

	private Aktoer mapAktoerTo(HentDokumenterFraJoarkTo.AktoerTo aktoer) {
		Aktoer output;
		switch (aktoer.getAktoerType()) {
			case PERSON:
				if (aktoer.isIdentifikatorAktoerId()) {
					output = new AktoerId()
							.withAktoerId(aktoer.getIdentifikator())
							.withNavn(aktoer.getNavn());
				} else {
					output = new Person()
							.withPersonidentifikator(aktoer.getIdentifikator())
							.withNavn(aktoer.getNavn());
				}
				break;
			case ORGANISASJON:
				output = new Organisasjon()
						.withOrgnummer(aktoer.getIdentifikator())
						.withNavn(aktoer.getNavn());
				break;
			case SAMHANDLER_HPR:
				output = new Samhandler()
						.withSamhandleridentifikator(aktoer.getIdentifikator())
						.withNavn(aktoer.getNavn())
						.withSamhandlerkategori(HPR.name());
				break;
			case SAMHANDLER_UTL_ORG:
				output = new Samhandler()
						.withSamhandleridentifikator(aktoer.getIdentifikator())
						.withNavn(aktoer.getNavn())
						.withSamhandlerkategori(UTL_ORG.name());
				break;
			default:
				output = null;
				break;
		}
		return output;
	}

	private Adresse mapAdresse(HentDokumenterFraJoarkTo.AdresseTo adresse) {
		if (adresse == null) {
			throw new ValidationException("Adresse kan ikke være null");
		} else if (adresse instanceof HentDokumenterFraJoarkTo.NorskPostadresseTo) {
			HentDokumenterFraJoarkTo.NorskPostadresseTo norskPostadresse = (HentDokumenterFraJoarkTo.NorskPostadresseTo) adresse;
			return new NorskPostadresse()
					.withAdresselinje1(norskPostadresse.getAdresselinje1())
					.withAdresselinje2(norskPostadresse.getAdresselinje2())
					.withAdresselinje3(norskPostadresse.getAdresselinje3())
					.withPostnummer(norskPostadresse.getPostnummer())
					.withPoststed(norskPostadresse.getPoststed())
					.withLand(norskPostadresse.getLand());
		} else if (adresse instanceof HentDokumenterFraJoarkTo.UtenlandskPostadresseTo) {
			HentDokumenterFraJoarkTo.UtenlandskPostadresseTo utenlandskPostadresse = (HentDokumenterFraJoarkTo.UtenlandskPostadresseTo) adresse;
			return new UtenlandskPostadresse()
					.withAdresselinje1(utenlandskPostadresse.getAdresselinje1())
					.withAdresselinje2(utenlandskPostadresse.getAdresselinje2())
					.withAdresselinje3(utenlandskPostadresse.getAdresselinje3())
					.withLand(utenlandskPostadresse.getLand());
		} else {
			return null;
		}
	}

}
