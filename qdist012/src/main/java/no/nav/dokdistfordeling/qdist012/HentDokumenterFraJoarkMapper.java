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
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.ORGANISASJON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.PERSON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static org.apache.commons.lang3.EnumUtils.getEnumIgnoreCase;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
		return HentDokumenterFraJoarkTo.DistribusjonbestillingTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.batchId(distribusjonbestilling.getBatchId())
				.distribusjonKanal(mapKanalCode(distribusjonbestilling.getDistribusjonKanal()))
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(distribusjonbestilling.getTema())
				.forsendelseTittel(distribusjonbestilling.getForsendelseTittel())
				.distribusjonstype(mapDistribusjonstype(distribusjonbestilling.getDistribusjonstype()))
				.distribusjonstidspunkt(mapDistribusjonstidspunkt(distribusjonbestilling.getDistribusjonstidspunkt()))
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
						.collect(Collectors.toList()))
				.build();
	}

	private HentDokumenterFraJoarkTo.ArkivInformasjonTo mapArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		return HentDokumenterFraJoarkTo.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private HentDokumenterFraJoarkTo.AktoerTo mapAktoer(Aktoer aktoer) {
		if (aktoer instanceof Person person) {
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(person.getNavn())
					.identifikator(person.getPersonidentifikator())
					.aktoerType(PERSON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof Organisasjon organisasjon) {
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(organisasjon.getNavn())
					.identifikator(organisasjon.getOrgnummer())
					.aktoerType(ORGANISASJON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof AktoerId aktoerId) {
			return HentDokumenterFraJoarkTo.AktoerTo.builder()
					.navn(aktoerId.getNavn())
					.identifikator(aktoerId.getAktoerId())
					.aktoerType(PERSON)
					.identifikatorAktoerId(true)
					.build();
		} else if (aktoer instanceof Samhandler samhandler) {
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
		} else if (adresse instanceof NorskPostadresse norskPostadresse) {
			return HentDokumenterFraJoarkTo.NorskPostadresseTo.builder()
					.adresselinje1(norskPostadresse.getAdresselinje1())
					.adresselinje2(norskPostadresse.getAdresselinje2())
					.adresselinje3(norskPostadresse.getAdresselinje3())
					.postnummer(norskPostadresse.getPostnummer())
					.poststed(norskPostadresse.getPoststed())
					.land(norskPostadresse.getLand())
					.build();
		} else if (adresse instanceof UtenlandskPostadresse utenlandskPostadresse) {
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
		}
		return switch (SamhandlerKategoriCode.valueOf(samhandlerKategori)) {
			case HPR -> SAMHANDLER_HPR;
			case UTL_ORG -> SAMHANDLER_UTL_ORG;
			case UKJENT -> SAMHANDLER_UKJENT;
		};
	}

	private String mapKanalCode(String distribusjonKanal) {
		return DITT_NAV.equals(distribusjonKanal) ? DITTNAV.name() : distribusjonKanal;
	}

	private DistribusjonstidspunktCode mapDistribusjonstidspunkt(String distribusjonstidspunkt) {
		return (isNotBlank(distribusjonstidspunkt) && isValidEnum(DistribusjonstidspunktCode.class, distribusjonstidspunkt.toUpperCase())) ?
				getEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt) : null;
	}

	private DistribusjonstypeCode mapDistribusjonstype(String distribusjonstype) {
		return (isNotBlank(distribusjonstype) && isValidEnum(DistribusjonstypeCode.class, distribusjonstype.toUpperCase())) ?
				getEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype) : null;
	}

}


