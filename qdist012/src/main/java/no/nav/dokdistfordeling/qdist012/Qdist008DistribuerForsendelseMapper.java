package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.exception.functional.DistrubuerForsendelseMapFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
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
import static java.util.Objects.nonNull;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.HPR;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.UKJENT;
import static no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode.UTL_ORG;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class Qdist008DistribuerForsendelseMapper {

	public DistribuerForsendelse map(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		try {
			DistribuerForsendelse distribuerForsendelse = new DistribuerForsendelse();
			distribuerForsendelse.setDistribusjonbestilling(mapDokumentbestillingsinformasjon(hentDokumenterFraJoarkTo.getDistribusjonbestilling()));
			return distribuerForsendelse;
		} catch (Exception e) {
			throw new DistrubuerForsendelseMapFunctionalException(format("Kunne ikke mappe qdist012 output. Feilmelding=%s",
					e.getMessage()), e);
		}
	}

	private Distribusjonbestilling mapDokumentbestillingsinformasjon(HentDokumenterFraJoarkTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(distribusjonbestillingTo.getBestillingsId());
		distribusjonbestilling.setBatchId(mapBatchId(distribusjonbestillingTo.getBatchId()));
		distribusjonbestilling.setDistribusjonKanal(distribusjonbestillingTo.getDistribusjonKanal());
		distribusjonbestilling.setBestillendeFagsystem(distribusjonbestillingTo.getBestillendeFagsystem());
		distribusjonbestilling.setTema(distribusjonbestillingTo.getTema());
		distribusjonbestilling.setForsendelseTittel(distribusjonbestillingTo.getForsendelseTittel());
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
				})
				.collect(Collectors.toList()));
		return distribusjonbestilling;
	}

	private String mapBatchId(String batchId) {
		return isBlank(batchId) ? null : batchId;
	}

	private ArkivInformasjon mapArkivInformasjon(HentDokumenterFraJoarkTo.ArkivInformasjonTo arkivInformasjonTo) {
		if (arkivInformasjonTo == null) {
			return null;
		}
		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivSystem(arkivInformasjonTo.getArkivSystem());
		arkivInformasjon.setArkivId(arkivInformasjonTo.getArkivId());
		return arkivInformasjon;
	}

	private Aktoer mapAktoerTo(HentDokumenterFraJoarkTo.AktoerTo aktoer) {
		Aktoer output;
		switch (aktoer.getAktoerType()) {
			case PERSON -> {
				if (aktoer.isIdentifikatorAktoerId()) {
					AktoerId aktoerId = new AktoerId();
					aktoerId.setAktoerId(aktoer.getIdentifikator());
					aktoerId.setNavn(aktoer.getNavn());
					output = aktoerId;
				} else {
					Person person = new Person();
					person.setPersonidentifikator(aktoer.getIdentifikator());
					person.setNavn(aktoer.getNavn());
					output = person;
				}
			}
			case ORGANISASJON -> {
				Organisasjon organisasjon = new Organisasjon();
				organisasjon.setOrgnummer(aktoer.getIdentifikator());
				organisasjon.setNavn(aktoer.getNavn());
				output = organisasjon;
			}
			case SAMHANDLER_HPR -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setSamhandleridentifikator(aktoer.getIdentifikator());
				samhandler.setNavn(aktoer.getNavn());
				samhandler.setSamhandlerkategori(HPR.name());
				output = samhandler;
			}
			case SAMHANDLER_UTL_ORG -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setSamhandleridentifikator(aktoer.getIdentifikator());
				samhandler.setNavn(aktoer.getNavn());
				samhandler.setSamhandlerkategori(UTL_ORG.name());
				output = samhandler;
			}
			case SAMHANDLER_UKJENT -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setSamhandleridentifikator(aktoer.getIdentifikator());
				samhandler.setNavn(aktoer.getNavn());
				samhandler.setSamhandlerkategori(UKJENT.name());
				output = samhandler;
			}
			default -> output = null;
		}
		return output;
	}

	private Adresse mapAdresse(HentDokumenterFraJoarkTo.AdresseTo adresse) {
		if (adresse == null) {
			return null;
		} else if (adresse instanceof HentDokumenterFraJoarkTo.NorskPostadresseTo norskPostadresseTo) {
			NorskPostadresse norskPostadresse = new NorskPostadresse();
			norskPostadresse.setAdresselinje1(trimAdresselinje(norskPostadresseTo.getAdresselinje1()));
			norskPostadresse.setAdresselinje2(trimAdresselinje(norskPostadresseTo.getAdresselinje2()));
			norskPostadresse.setAdresselinje3(trimAdresselinje(norskPostadresseTo.getAdresselinje3()));
			norskPostadresse.setPostnummer(norskPostadresseTo.getPostnummer());
			norskPostadresse.setPoststed(norskPostadresseTo.getPoststed());
			norskPostadresse.setLand(norskPostadresseTo.getLand());
			return norskPostadresse;
		} else if (adresse instanceof HentDokumenterFraJoarkTo.UtenlandskPostadresseTo utenlandskPostadresseTo) {
			UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
			utenlandskPostadresse.setAdresselinje1(trimAdresselinje(utenlandskPostadresseTo.getAdresselinje1()));
			utenlandskPostadresse.setAdresselinje2(trimAdresselinje(utenlandskPostadresseTo.getAdresselinje2()));
			utenlandskPostadresse.setAdresselinje3(utenlandskPostadresseTo.getAdresselinje3());
			utenlandskPostadresse.setLand(utenlandskPostadresseTo.getLand());
			return utenlandskPostadresse;
		} else {
			return null;
		}
	}

	private String trimAdresselinje(String adresselinje) {
		return isBlank(adresselinje) ? null : adresselinje.strip();
	}

	private String mapDistribusjonstidspunkt(DistribusjonstidspunktCode distribusjonstidspunktCode) {
		return (nonNull(distribusjonstidspunktCode) && isValidEnum(DistribusjonstidspunktCode.class, distribusjonstidspunktCode.name())) ?
				distribusjonstidspunktCode.name() : null;
	}

	private String mapDistribusjonstype(DistribusjonstypeCode distribusjonstype) {
		return (nonNull(distribusjonstype) && isValidEnum(DistribusjonstypeCode.class, distribusjonstype.name())) ?
				distribusjonstype.name() : null;
	}
}
