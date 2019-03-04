package no.nav.dokdistfordeling.qdist008;

import static java.lang.String.format;

import no.nav.dokdistfordeling.exception.DistribuerForsendelseMapperIllegalArgumentException;
import no.nav.dokdistfordeling.exception.ValidationException;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.MottakerTypeCode;
import no.nav.dokdistfordeling.kodeverk.TemaCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.Adresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.UtenlandskPostadresse;
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
			throw new DistribuerForsendelseMapperIllegalArgumentException("Kunne ikke mappe qdist008-XML til domene-objekter for bestillingsId=" +
					distribuerForsendelse.getDistribusjonbestilling().getBestillingsId(), e);
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

	private DistribuerForsendelseTo.MottakerTo mapAktoer(Aktoer mottaker) {
		if (mottaker instanceof Person) {
			Person person = (Person) mottaker;
			return DistribuerForsendelseTo.MottakerTo.builder()
					.navn(person.getNavn())
					.identifikator(person.getPersonidentifikator())
					.mottakerType(MottakerTypeCode.PERSON)
					.identifikatorAktoerId(false)
					.build();
		} else if (mottaker instanceof Organisasjon) {
			Organisasjon organisasjon = (Organisasjon) mottaker;
			return DistribuerForsendelseTo.MottakerTo.builder()
					.navn(organisasjon.getNavn())
					.identifikator(organisasjon.getOrgnummer())
					.mottakerType(MottakerTypeCode.ORGANISASJON)
					.identifikatorAktoerId(false)
					.build();
		} else if (mottaker instanceof AktoerId) {
			AktoerId aktoerId = (AktoerId) mottaker;
			return DistribuerForsendelseTo.MottakerTo.builder()
					.navn(aktoerId.getNavn())
					.identifikator(aktoerId.getAktoerId())
					.mottakerType(MottakerTypeCode.PERSON)
					.identifikatorAktoerId(true)
					.build();
		} else if (mottaker instanceof Samhandler) {
			Samhandler samhandler = (Samhandler) mottaker;
			return DistribuerForsendelseTo.MottakerTo.builder()
					.navn(samhandler.getNavn())
					.identifikator(samhandler.getSamhandleridentifikator())
					.mottakerType(mapSamhandlerKategoriToSamhandlerType(samhandler.getSamhandlerkategori()))
					.identifikatorAktoerId(false)
					.build();
		} else {
			throw new IllegalArgumentException(format("Ugyldig type for mottaker %s", mottaker.getClass().getName()));
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

	private MottakerTypeCode mapSamhandlerKategoriToSamhandlerType(String samhandlerKategori) {
		if (samhandlerKategori == null) {
			throw new ValidationException("Ugyldig input: samhandlerkategori kan ikke være null");
		} else if (SamhandlerKategoriCode.HPR.name().equals(samhandlerKategori)) {
			return MottakerTypeCode.SAMHANDLER_HPR;
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

	private enum SamhandlerKategoriCode {
		HPR
	}
}
