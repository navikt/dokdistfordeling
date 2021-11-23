package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.exception.functional.DistrubuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class HentDokumenterFraJoarkMapper {

	@Handler
	public HentDokumenterFraJoarkTo map(HentDokumenterFraJoark hentDokumenterFraJoark) {
		try {
			return HentDokumenterFraJoarkTo.builder()
					.distribusjonbestilling(mapDokumentbestillingsinformasjon(hentDokumenterFraJoark.getDistribusjonbestilling()))
					.build();
		} catch (IllegalArgumentException e) {
			throw new DistrubuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist012 input-XML til domene-objekter. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private HentDokumenterFraJoarkTo.DistribusjonbestillingTo mapDokumentbestillingsinformasjon(Distribusjonbestilling distribusjonbestilling) {
		HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder distribusjonbestillingToBuilder = HentDokumenterFraJoarkTo.DistribusjonbestillingTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.batchId(distribusjonbestilling.getBatchId())
				.distribusjonKanal(mapKanalCode(distribusjonbestilling.getDistribusjonKanal()))
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(distribusjonbestilling.getTema())
				.forsendelseTittel(distribusjonbestilling.getForsendelseTittel())
				.arkivInformasjon(distribusjonbestilling.getArkivInformasjon() == null ? null :
						mapArkivInformasjon(distribusjonbestilling.getArkivInformasjon()))
				.mottaker(mapAktoer(distribusjonbestilling.getMottaker()))
				.bruker(mapAktoer(distribusjonbestilling.getBruker()))
				.adresse(PRINT.name().equals(distribusjonbestilling.getDistribusjonKanal()) ? mapAdresse(distribusjonbestilling.getAdresse()) : null)
				.dokumentProdApp(distribusjonbestilling.getDokumentProdApp())
				.dokumenter(distribusjonbestilling.getDokumenter().stream()
						.map(dokumentInformasjon -> HentDokumenterFraJoarkTo.DokumentInformasjonTo.builder()
								.dokumenttypeId(dokumentInformasjon.getDokumenttypeId())
								.tilknyttetSom(dokumentInformasjon.getTilknyttetSom())
								.arkivDokumentInfoId(dokumentInformasjon.getArkivDokumentInfoId())
								.rekkefolge(dokumentInformasjon.getRekkefolge())
								.variantFormat(dokumentInformasjon.getVariantFormat())
								.build())
						.collect(Collectors.toList()));

		setDistribusjonstype(distribusjonbestillingToBuilder, distribusjonbestilling.getDistribusjonstype());
		setDistribusjonstidspunkt(distribusjonbestillingToBuilder, distribusjonbestilling.getDistribusjonstidspunkt());

		return distribusjonbestillingToBuilder.build();
	}

	private HentDokumenterFraJoarkTo.ArkivInformasjonTo mapArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		return HentDokumenterFraJoarkTo.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private HentDokumenterFraJoarkTo.AktoerTo mapAktoer(Aktoer aktoer) {
		if (aktoer instanceof Person) {
			Person person = (Person) aktoer;
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(person.getNavn())
					.identifikator(person.getPersonidentifikator())
					.aktoerType(AktoerTypeCode.PERSON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof Organisasjon) {
			Organisasjon organisasjon = (Organisasjon) aktoer;
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(organisasjon.getNavn())
					.identifikator(organisasjon.getOrgnummer())
					.aktoerType(AktoerTypeCode.ORGANISASJON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof AktoerId) {
			AktoerId aktoerId = (AktoerId) aktoer;
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(aktoerId.getNavn())
					.identifikator(aktoerId.getAktoerId())
					.aktoerType(AktoerTypeCode.PERSON)
					.identifikatorAktoerId(true)
					.build();
		} else if (aktoer instanceof Samhandler) {
			Samhandler samhandler = (Samhandler) aktoer;
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(samhandler.getNavn())
					.identifikator(samhandler.getSamhandleridentifikator())
					.aktoerType(mapSamhandlerKategoriToSamhandlerType(samhandler.getSamhandlerkategori()))
					.identifikatorAktoerId(false)
					.build();
		} else {
			throw new IllegalArgumentException(format("Ugyldig type for mottaker %s", aktoer.getClass().getName()));
		}
	}

	private HentDokumenterFraJoarkTo.AdresseTo mapAdresse(Adresse adresse) {
		if (adresse == null) {
			throw new ValidationException("Adresse kan ikke være null");
		} else if (adresse instanceof NorskPostadresse) {
			NorskPostadresse norskPostadresse = (NorskPostadresse) adresse;
			return HentDokumenterFraJoarkTo.NorskPostadresseTo.builder()
					.adresselinje1(norskPostadresse.getAdresselinje1())
					.adresselinje2(norskPostadresse.getAdresselinje2())
					.adresselinje3(norskPostadresse.getAdresselinje3())
					.postnummer(norskPostadresse.getPostnummer())
					.poststed(norskPostadresse.getPoststed())
					.land(norskPostadresse.getLand())
					.build();
		} else if (adresse instanceof UtenlandskPostadresse) {
			UtenlandskPostadresse utenlandskPostadresse = (UtenlandskPostadresse) adresse;
			return HentDokumenterFraJoarkTo.UtenlandskPostadresseTo.builder()
					.adresselinje1(utenlandskPostadresse.getAdresselinje1())
					.adresselinje2(utenlandskPostadresse.getAdresselinje2())
					.adresselinje3(utenlandskPostadresse.getAdresselinje3())
					.land(utenlandskPostadresse.getLand())
					.build();
		} else {
			throw new IllegalArgumentException("Ugyldig adressetype. Adresse er ikke en gyldig NorskPostadresse eller UtenlandskPostadresse");
		}
	}

	private AktoerTypeCode mapSamhandlerKategoriToSamhandlerType(String samhandlerKategori) {
		if (samhandlerKategori == null) {
			throw new ValidationException("Ugyldig input: samhandlerkategori kan ikke være null");
		} else if (SamhandlerKategoriCode.HPR.name().equals(samhandlerKategori)) {
			return AktoerTypeCode.SAMHANDLER_HPR;
		} else if (SamhandlerKategoriCode.UTL_ORG.name().equals(samhandlerKategori)) {
			return AktoerTypeCode.SAMHANDLER_UTL_ORG;
		} else {
			throw new IllegalArgumentException(format("Ugyldig input: Kun samhandlerkategori=HPR og UTL_ORG støttes støttes. Fikk samhandlerkategori=%s", samhandlerKategori));
		}
	}

	private String mapKanalCode(String distribusjonKanal) {
		return DITT_NAV.equals(distribusjonKanal) ? DITTNAV.name() : distribusjonKanal;
	}

	private void setDistribusjonstidspunkt(HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder distribusjonbestilling, String distribusjonstidspunkt) {
		if (isNotBlank(distribusjonstidspunkt) && isValidEnum(DistribusjonstidspunktCode.class, distribusjonstidspunkt.toUpperCase())) {
			distribusjonbestilling.distribusjonstidspunkt(distribusjonstidspunkt.toUpperCase());
		}
	}

	private void setDistribusjonstype(HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder distribusjonbestilling, String distribusjonstype) {
		if (isNotBlank(distribusjonstype) && isValidEnum(DistribusjonstypeCode.class, distribusjonstype.toUpperCase())) {
			distribusjonbestilling.distribusjonstype(distribusjonstype.toUpperCase());
		}
	}
}


