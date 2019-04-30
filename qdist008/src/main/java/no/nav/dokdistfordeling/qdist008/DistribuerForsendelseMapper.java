package no.nav.dokdistfordeling.qdist008;

import static java.lang.String.format;

import no.nav.dokdistfordeling.exception.functional.DistrubuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.dokdistfordeling.kodeverk.TemaCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
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

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DistribuerForsendelseMapper {

	@Handler
	public DistribuerForsendelseTo map(DistribuerForsendelse distribuerForsendelse) {
		try {
			return DistribuerForsendelseTo.builder()
					.distribusjonbestilling(mapDokumentbestillingsinformasjon(distribuerForsendelse.getDistribusjonbestilling()))
					.build();
		} catch (IllegalArgumentException e) {
			throw new DistrubuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist008 input-XML til domene-objekter. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private DistribuerForsendelseTo.DistribusjonbestillingTo mapDokumentbestillingsinformasjon(Distribusjonbestilling distribusjonbestilling) {
		return DistribuerForsendelseTo.DistribusjonbestillingTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.batchId(distribusjonbestilling.getBatchId())
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(stringToEnum(TemaCode.class, distribusjonbestilling.getTema()))
				.forsendelseTittel(distribusjonbestilling.getForsendelseTittel())
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

	private DistribuerForsendelseTo.ArkivInformasjonTo mapArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		return DistribuerForsendelseTo.ArkivInformasjonTo.builder()
				.arkivSystem(stringToEnum(ArkivSystemCode.class, arkivInformasjon.getArkivSystem()))
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private DistribuerForsendelseTo.AktoerTo mapAktoer(Aktoer aktoer) {
		if (aktoer instanceof Person) {
			Person person = (Person) aktoer;
			return DistribuerForsendelseTo.AktoerTo.builder()
					.navn(person.getNavn())
					.identifikator(person.getPersonidentifikator())
					.aktoerType(AktoerTypeCode.PERSON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof Organisasjon) {
			Organisasjon organisasjon = (Organisasjon) aktoer;
			return DistribuerForsendelseTo.AktoerTo.builder()
					.navn(organisasjon.getNavn())
					.identifikator(organisasjon.getOrgnummer())
					.aktoerType(AktoerTypeCode.ORGANISASJON)
					.identifikatorAktoerId(false)
					.build();
		} else if (aktoer instanceof AktoerId) {
			AktoerId aktoerId = (AktoerId) aktoer;
			return DistribuerForsendelseTo.AktoerTo.builder()
					.navn(aktoerId.getNavn())
					.identifikator(aktoerId.getAktoerId())
					.aktoerType(AktoerTypeCode.PERSON)
					.identifikatorAktoerId(true)
					.build();
		} else if (aktoer instanceof Samhandler) {
			Samhandler samhandler = (Samhandler) aktoer;
			return DistribuerForsendelseTo.AktoerTo.builder()
					.navn(samhandler.getNavn())
					.identifikator(samhandler.getSamhandleridentifikator())
					.aktoerType(mapSamhandlerKategoriToSamhandlerType(samhandler.getSamhandlerkategori()))
					.identifikatorAktoerId(false)
					.build();
		} else {
			throw new IllegalArgumentException(format("Ugyldig type for mottaker %s", aktoer.getClass().getName()));
		}
	}

	private DistribuerForsendelseTo.AdresseTo mapAdresse(Adresse adresse) {
		if (adresse == null) {
			return null;
		} else if (adresse instanceof NorskPostadresse) {
			NorskPostadresse norskPostadresse = (NorskPostadresse) adresse;
			return DistribuerForsendelseTo.NorskPostadresseTo.builder()
					.adresselinje1(norskPostadresse.getAdresselinje1())
					.adresselinje2(norskPostadresse.getAdresselinje2())
					.adresselinje3(norskPostadresse.getAdresselinje3())
					.postnummer(norskPostadresse.getPostnummer())
					.poststed(norskPostadresse.getPoststed())
					.land(norskPostadresse.getLand())
					.build();
		} else if (adresse instanceof UtenlandskPostadresse) {
			UtenlandskPostadresse utenlandskPostadresse = (UtenlandskPostadresse) adresse;
			return DistribuerForsendelseTo.UtenlandskPostadresseTo.builder()
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
		} else {
			throw new IllegalArgumentException(format("Ugyldig input: Kun samhandlerkategori=HPR støttes. Fikk samhandlerkategori=%s", samhandlerKategori));
		}
	}

	private static <E extends Enum<E>> E stringToEnum(Class<E> enumClass, String enumName) {
		try {
			return enumName == null ? null : Enum.valueOf(enumClass, enumName);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(format("%s er ikke en gyldig kodeverdi for %s", enumName, enumClass));
		}
	}

}
