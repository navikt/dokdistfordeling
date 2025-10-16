package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.exception.functional.DistribuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AktoerTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.ArkivInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.UtenlandskPostadresseTo;
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

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.HPR;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.UKJENT;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.UTL_ORG;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class Qdist008DistribuerForsendelseMapper {

	public DistribuerForsendelse map(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		try {
			DistribuerForsendelse distribuerForsendelse = new DistribuerForsendelse();
			distribuerForsendelse.setDistribusjonbestilling(mapDokumentbestillingsinformasjon(hentDokumenterFraJoarkTo.getDistribusjonbestilling()));

			return distribuerForsendelse;
		} catch (Exception e) {
			throw new DistribuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist012 output. Feilmelding=%s", e.getMessage()), e);
		}
	}

	private Distribusjonbestilling mapDokumentbestillingsinformasjon(DistribusjonbestillingTo distribusjonbestillingTo) {
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(distribusjonbestillingTo.getBestillingsId());
		distribusjonbestilling.setBatchId(mapBatchId(distribusjonbestillingTo.getBatchId()));
		distribusjonbestilling.setDistribusjonKanal(distribusjonbestillingTo.getDistribusjonKanal());
		distribusjonbestilling.setBestillendeFagsystem(distribusjonbestillingTo.getBestillendeFagsystem());
		distribusjonbestilling.setTema(distribusjonbestillingTo.getTema());
		distribusjonbestilling.setForsendelseTittel(distribusjonbestillingTo.getForsendelseTittel());
		distribusjonbestilling.setForsendelseMetadata(distribusjonbestillingTo.getForsendelseMetadata());
		distribusjonbestilling.setForsendelseMetadataType(mapForsendelseMetadataType(distribusjonbestillingTo.getForsendelseMetadataType()));
		distribusjonbestilling.setDistribusjonstype(mapDistribusjonstype(distribusjonbestillingTo.getDistribusjonstype()));
		distribusjonbestilling.setDistribusjonstidspunkt(mapDistribusjonstidspunkt(distribusjonbestillingTo.getDistribusjonstidspunkt()));
		distribusjonbestilling.setArkivInformasjon(mapArkivInformasjon(distribusjonbestillingTo.getArkivInformasjon()));
		distribusjonbestilling.setMottaker(mapAktoerTo(distribusjonbestillingTo.getMottaker()));
		distribusjonbestilling.setBruker(mapAktoerTo(distribusjonbestillingTo.getBruker()));
		distribusjonbestilling.setAdresse(mapAdresse(distribusjonbestillingTo.getAdresse()));
		distribusjonbestilling.setDokumentProdApp(distribusjonbestillingTo.getDokumentProdApp());
		distribusjonbestilling.setDokumenter(distribusjonbestillingTo.getDokumenter().stream()
				.map(dokumentInformasjonTo -> {
					DokumentInformasjon dokumentInformasjon = new DokumentInformasjon();
					dokumentInformasjon.setDokumenttypeId(dokumentInformasjonTo.getDokumenttypeId());
					dokumentInformasjon.setTilknyttetSom(dokumentInformasjonTo.getTilknyttetSom());
					dokumentInformasjon.setArkivDokumentInfoId(dokumentInformasjonTo.getArkivDokumentInfoId());
					dokumentInformasjon.setRekkefolge(dokumentInformasjonTo.getRekkefolge());
					dokumentInformasjon.setDokumentObjektReferanse(dokumentInformasjonTo.getDokumentObjektReferanse());
					return dokumentInformasjon;
				}).toList()
		);
		return distribusjonbestilling;
	}

	private String mapBatchId(String batchId) {
		return isBlank(batchId) ? null : batchId;
	}

	private String mapForsendelseMetadataType(ForsendelseMetadataType forsendelseMetadataType) {
		return nonNull(forsendelseMetadataType) ? forsendelseMetadataType.name() : null;
	}

	private String mapDistribusjonstype(DistribusjonstypeCode distribusjonstype) {
		return nonNull(distribusjonstype) ? distribusjonstype.name() : null;
	}

	private String mapDistribusjonstidspunkt(DistribusjonstidspunktCode distribusjonstidspunktCode) {
		return nonNull(distribusjonstidspunktCode) ? distribusjonstidspunktCode.name() : null;
	}

	private ArkivInformasjon mapArkivInformasjon(ArkivInformasjonTo arkivInformasjonTo) {
		if (arkivInformasjonTo == null) {
			return null;
		}
		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivSystem(arkivInformasjonTo.getArkivSystem());
		arkivInformasjon.setArkivId(arkivInformasjonTo.getArkivId());
		return arkivInformasjon;
	}

	private Aktoer mapAktoerTo(AktoerTo aktoerTo) {
		return switch (aktoerTo.getAktoerType()) {
			case PERSON -> {
				if (aktoerTo.isIdentifikatorAktoerId()) {
					yield mapToAktoerId(aktoerTo);
				} else {
					yield mapToPerson(aktoerTo);
				}
			}
			case ORGANISASJON -> mapToOrganisasjon(aktoerTo);
			case SAMHANDLER_HPR -> mapToSamhandler(aktoerTo, HPR);
			case SAMHANDLER_UTL_ORG -> mapToSamhandler(aktoerTo, UTL_ORG);
			case SAMHANDLER_UKJENT -> mapToSamhandler(aktoerTo, UKJENT);
		};
	}

	private static AktoerId mapToAktoerId(AktoerTo aktoer) {
		AktoerId aktoerId = new AktoerId();
		aktoerId.setAktoerId(aktoer.getIdentifikator());
		aktoerId.setNavn(aktoer.getNavn());
		return aktoerId;
	}

	private static Person mapToPerson(AktoerTo aktoer) {
		Person person = new Person();
		person.setPersonidentifikator(aktoer.getIdentifikator());
		person.setNavn(aktoer.getNavn());
		return person;
	}

	private static Organisasjon mapToOrganisasjon(AktoerTo aktoer) {
		Organisasjon organisasjon = new Organisasjon();
		organisasjon.setOrgnummer(aktoer.getIdentifikator());
		organisasjon.setNavn(aktoer.getNavn());
		return organisasjon;
	}

	private static Samhandler mapToSamhandler(AktoerTo aktoer, SamhandlerKategoriCode samhandlerKategoriCode) {
		Samhandler samhandler = new Samhandler();
		samhandler.setSamhandleridentifikator(aktoer.getIdentifikator());
		samhandler.setNavn(aktoer.getNavn());
		samhandler.setSamhandlerkategori(samhandlerKategoriCode.name());
		return samhandler;
	}

	private Adresse mapAdresse(HentDokumenterFraJoarkTo.AdresseTo adresse) {
		if (adresse == null) {
			return null;
		}

		return switch (adresse) {
			case NorskPostadresseTo norskPostadresseTo -> mapToNorskPostadresse(norskPostadresseTo);
			case UtenlandskPostadresseTo utenlandskPostadresseTo -> mapToUtenlandskPostadresse(utenlandskPostadresseTo);
		};
	}

	private UtenlandskPostadresse mapToUtenlandskPostadresse(UtenlandskPostadresseTo utenlandskPostadresseTo) {
		UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
		utenlandskPostadresse.setAdresselinje1(trimAdresselinje(utenlandskPostadresseTo.getAdresselinje1()));
		utenlandskPostadresse.setAdresselinje2(trimAdresselinje(utenlandskPostadresseTo.getAdresselinje2()));
		utenlandskPostadresse.setAdresselinje3(utenlandskPostadresseTo.getAdresselinje3());
		utenlandskPostadresse.setLand(utenlandskPostadresseTo.getLand());
		return utenlandskPostadresse;
	}

	private NorskPostadresse mapToNorskPostadresse(NorskPostadresseTo norskPostadresseTo) {
		NorskPostadresse norskPostadresse = new NorskPostadresse();
		norskPostadresse.setAdresselinje1(trimAdresselinje(norskPostadresseTo.getAdresselinje1()));
		norskPostadresse.setAdresselinje2(trimAdresselinje(norskPostadresseTo.getAdresselinje2()));
		norskPostadresse.setAdresselinje3(trimAdresselinje(norskPostadresseTo.getAdresselinje3()));
		norskPostadresse.setPostnummer(norskPostadresseTo.getPostnummer());
		norskPostadresse.setPoststed(norskPostadresseTo.getPoststed());
		norskPostadresse.setLand(norskPostadresseTo.getLand());
		return norskPostadresse;
	}

	private String trimAdresselinje(String adresselinje) {
		return isBlank(adresselinje) ? null : adresselinje.strip();
	}

}