package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.exception.functional.DistribuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.AdresseTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.AktoerTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.ArkivInformasjonTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.UtenlandskPostadresseTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.UtenlandskPostadresse;
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
import static no.nav.dokdistfordeling.util.MappingUtil.stringToEnum;
import static org.apache.commons.lang3.EnumUtils.getEnumIgnoreCase;
import static org.apache.commons.lang3.EnumUtils.isValidEnumIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class DistribuerForsendelseMapper {

	@Handler
	public DistribuerForsendelseTo map(DistribuerForsendelse distribuerForsendelse) {
		try {
			return DistribuerForsendelseTo.builder()
					.distribusjonbestilling(mapDokumentbestillingsinformasjon(distribuerForsendelse.getDistribusjonbestilling()))
					.build();
		} catch (IllegalArgumentException e) {
			throw new DistribuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist008 input-XML til domene-objekter. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private DistribuerForsendelseTo.DistribusjonbestillingTo mapDokumentbestillingsinformasjon(Distribusjonbestilling distribusjonbestilling) {
		return DistribuerForsendelseTo.DistribusjonbestillingTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.batchId(mapBatchId(distribusjonbestilling.getBatchId()))
				.distribusjonKanal(mapKanalCode(distribusjonbestilling.getDistribusjonKanal()))
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(distribusjonbestilling.getTema())
				.forsendelseTittel(distribusjonbestilling.getForsendelseTittel())
				.forsendelseMetadata(distribusjonbestilling.getForsendelseMetadata())
				.forsendelseMetadataType(mapForsendelseMetadataType(distribusjonbestilling.getForsendelseMetadataType()))
				.distribusjonstype(mapDistribusjonstype(distribusjonbestilling.getDistribusjonstype()))
				.distribusjonstidspunkt(mapDistribusjonstidspunkt(distribusjonbestilling.getDistribusjonstidspunkt()))
				.arkivInformasjon(distribusjonbestilling.getArkivInformasjon() == null ? null :
						mapArkivInformasjon(distribusjonbestilling.getArkivInformasjon()))
				.mottaker(mapAktoer(distribusjonbestilling.getMottaker()))
				.bruker(mapAktoer(distribusjonbestilling.getBruker()))
				.adresse(mapAdresse(distribusjonbestilling.getAdresse()))
				.dokumentProdApp(distribusjonbestilling.getDokumentProdApp())
				.dokumenter(distribusjonbestilling.getDokumenter().stream()
						.map(dokumentInformasjon -> DistribuerForsendelseTo.DokumentInformasjonTo.builder()
								.dokumenttypeId(dokumentInformasjon.getDokumenttypeId())
								.dokumentObjektReferanse(dokumentInformasjon.getDokumentObjektReferanse())
								.tilknyttetSom(stringToEnum(TilknyttetSomCode.class, dokumentInformasjon.getTilknyttetSom()))
								.arkivDokumentInfoId(dokumentInformasjon.getArkivDokumentInfoId())
								.rekkefolge(dokumentInformasjon.getRekkefolge())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	private String mapBatchId(String batchId) {
		return isBlank(batchId) ? null : batchId;
	}

	private String mapKanalCode(String distribusjonKanal) {
		return DITT_NAV.equals(distribusjonKanal) ? DITTNAV.name() : distribusjonKanal;
	}

	private ForsendelseMetadataType mapForsendelseMetadataType(String forsendelseMetadataType) {
		return (isNotBlank(forsendelseMetadataType) && isValidEnumIgnoreCase(ForsendelseMetadataType.class, forsendelseMetadataType)) ?
				getEnumIgnoreCase(ForsendelseMetadataType.class, forsendelseMetadataType) : null;
	}

	private DistribusjonstypeCode mapDistribusjonstype(String distribusjonstype) {
		return (isNotBlank(distribusjonstype) && isValidEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype)) ?
				getEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype) : null;
	}

	private DistribusjonstidspunktCode mapDistribusjonstidspunkt(String distribusjonstidspunkt) {
		return (isNotBlank(distribusjonstidspunkt) && isValidEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt)) ?
				getEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt) : null;
	}

	private ArkivInformasjonTo mapArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		return ArkivInformasjonTo.builder()
				.arkivSystem(stringToEnum(ArkivSystemCode.class, arkivInformasjon.getArkivSystem()))
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private AktoerTo mapAktoer(Aktoer aktoer) {
		return switch (aktoer) {
			case Person person -> mapToPerson(person);
			case Organisasjon organisasjon -> mapToOrganisasjon(organisasjon);
			case AktoerId aktoerId -> mapToAktoerId(aktoerId);
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

	private AktoerTo mapToAktoerId(AktoerId aktoerId) {
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

	private AdresseTo mapAdresse(Adresse adresse) {
		if (adresse == null) {
			return null;
		}

		if (adresse instanceof NorskPostadresse norskPostadresse) {
			return NorskPostadresseTo.builder()
					.adresselinje1(trimAdresse(norskPostadresse.getAdresselinje1()))
					.adresselinje2(trimAdresse(norskPostadresse.getAdresselinje2()))
					.adresselinje3(trimAdresse(norskPostadresse.getAdresselinje3()))
					.postnummer(norskPostadresse.getPostnummer())
					.poststed(norskPostadresse.getPoststed())
					.land(norskPostadresse.getLand())
					.build();
		} else if (adresse instanceof UtenlandskPostadresse utenlandskPostadresse) {
			return UtenlandskPostadresseTo.builder()
					.adresselinje1(trimAdresse(utenlandskPostadresse.getAdresselinje1()))
					.adresselinje2(trimAdresse(utenlandskPostadresse.getAdresselinje2()))
					.adresselinje3(trimAdresse(utenlandskPostadresse.getAdresselinje3()))
					.land(utenlandskPostadresse.getLand())
					.build();
		} else {
			throw new IllegalArgumentException("Ugyldig adressetype. Adresse er ikke en gyldig NorskPostadresse eller UtenlandskPostadresse");
		}
	}

	private String trimAdresse(String adresselinje) {
		return isBlank(adresselinje) ? null : adresselinje.strip();
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

}