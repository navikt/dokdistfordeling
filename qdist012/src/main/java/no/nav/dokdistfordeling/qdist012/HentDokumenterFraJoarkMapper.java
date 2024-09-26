package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.exception.functional.DistribuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AdresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AktoerTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.ArkivInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.UtenlandskPostadresseTo;
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
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
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
			throw new DistribuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist012 input-XML til domene-objekter. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private DistribusjonbestillingTo mapDokumentbestillingsinformasjon(Distribusjonbestilling distribusjonbestilling) {
		return DistribusjonbestillingTo.builder()
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
				.adresse(mapAdresse(distribusjonbestilling.getAdresse()))
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

	private String mapKanalCode(String distribusjonKanal) {
		return DITT_NAV.equals(distribusjonKanal) ? DITTNAV.name() : distribusjonKanal;
	}

	private DistribusjonstypeCode mapDistribusjonstype(String distribusjonstype) {
		return (isNotBlank(distribusjonstype) && isValidEnum(DistribusjonstypeCode.class, distribusjonstype.toUpperCase())) ?
				getEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype) : null;
	}

	private DistribusjonstidspunktCode mapDistribusjonstidspunkt(String distribusjonstidspunkt) {
		return (isNotBlank(distribusjonstidspunkt) && isValidEnum(DistribusjonstidspunktCode.class, distribusjonstidspunkt.toUpperCase())) ?
				getEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt) : null;
	}

	private ArkivInformasjonTo mapArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		return ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private AktoerTo mapAktoer(Aktoer aktoer) {
		return switch (aktoer) {
			case Person person -> mapToPerson(person);
			case Organisasjon organisasjon -> mapToOrganisasjon(organisasjon);
			case AktoerId aktoerId -> mapToAktoerTo(aktoerId);
			case Samhandler samhandler -> mapToSamhandler(samhandler);
			case null, default ->
					throw new IllegalArgumentException(format("Ugyldig type for mottaker %s", aktoer.getClass().getName()));
		};
	}

	private AktoerTo mapToPerson(Person person) {
		return AktoerTo.builder()
				.navn(person.getNavn())
				.identifikator(person.getPersonidentifikator())
				.aktoerType(PERSON)
				.identifikatorAktoerId(false)
				.build();
	}

	private AktoerTo mapToOrganisasjon(Organisasjon organisasjon) {
		return AktoerTo.builder()
				.navn(organisasjon.getNavn())
				.identifikator(organisasjon.getOrgnummer())
				.aktoerType(ORGANISASJON)
				.identifikatorAktoerId(false)
				.build();
	}

	private AktoerTo mapToAktoerTo(AktoerId aktoerId) {
		return AktoerTo.builder()
				.navn(aktoerId.getNavn())
				.identifikator(aktoerId.getAktoerId())
				.aktoerType(PERSON)
				.identifikatorAktoerId(true)
				.build();
	}

	private AktoerTo mapToSamhandler(Samhandler samhandler) {
		return AktoerTo.builder()
				.navn(samhandler.getNavn())
				.identifikator(samhandler.getSamhandleridentifikator())
				.aktoerType(mapSamhandlerKategoriToSamhandlerType(samhandler.getSamhandlerkategori()))
				.identifikatorAktoerId(false)
				.build();
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

	private AdresseTo mapAdresse(Adresse adresse) {
		if (adresse == null) {
			return null;
		}

		return switch (adresse) {
			case NorskPostadresse norskPostadresse -> mapToNorskPostadresse(norskPostadresse);
			case UtenlandskPostadresse utenlandskPostadresse -> mapToUtenlandskPostadresse(utenlandskPostadresse);
			default ->
					throw new IllegalArgumentException("Ugyldig adressetype. Adresse er ikke en gyldig NorskPostadresse eller UtenlandskPostadresse");
		};
	}

	private NorskPostadresseTo mapToNorskPostadresse(NorskPostadresse norskPostadresse) {
		return NorskPostadresseTo.builder()
				.adresselinje1(norskPostadresse.getAdresselinje1())
				.adresselinje2(norskPostadresse.getAdresselinje2())
				.adresselinje3(norskPostadresse.getAdresselinje3())
				.postnummer(norskPostadresse.getPostnummer())
				.poststed(norskPostadresse.getPoststed())
				.land(norskPostadresse.getLand())
				.build();
	}

	private UtenlandskPostadresseTo mapToUtenlandskPostadresse(UtenlandskPostadresse utenlandskPostadresse) {
		return UtenlandskPostadresseTo.builder()
				.adresselinje1(utenlandskPostadresse.getAdresselinje1())
				.adresselinje2(utenlandskPostadresse.getAdresselinje2())
				.adresselinje3(utenlandskPostadresse.getAdresselinje3())
				.land(utenlandskPostadresse.getLand())
				.build();
	}

}