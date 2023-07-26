package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
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
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.AKTOERID;
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.FNR;
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.ORGNR;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.apache.commons.lang3.EnumUtils.getEnumIgnoreCase;
import static org.apache.commons.lang3.EnumUtils.isValidEnumIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class HentDokumenterFraJoarkMapper {

	public static final String NORSK_POSTADRESSE = "norskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	public HentDokumenterFraJoark map(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost,
									  Aktoer mottaker, String bestillingsId, DistribusjonsKanalCode distribusjonsKanal) {
		List<Journalpost.DokumentInfo> dokumenter = journalpost.getDokumenter();
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(bestillingsId);
		distribusjonbestilling.setBatchId(mapBatchId(distribuerJournalpostRequestTo.getBatchId()));
		distribusjonbestilling.setDistribusjonKanal(distribusjonsKanal.name());
		distribusjonbestilling.setBestillendeFagsystem(distribuerJournalpostRequestTo.getBestillendeFagsystem());
		distribusjonbestilling.setTema(journalpost.getTema());
		distribusjonbestilling.setForsendelseTittel(journalpost.getTittel());
		distribusjonbestilling.setDistribusjonstype(mapDistribusjonstype(distribuerJournalpostRequestTo.getDistribusjonstype()));
		distribusjonbestilling.setDistribusjonstidspunkt(mapDistribusjonstidspunkt(distribuerJournalpostRequestTo.getDistribusjonstidspunkt()));
		distribusjonbestilling.setArkivInformasjon(mapArkivInformasjon(distribuerJournalpostRequestTo));
		distribusjonbestilling.setMottaker(mottaker);
		distribusjonbestilling.setBruker(mapBruker(journalpost.getBruker()));
		distribusjonbestilling.setAdresse(PRINT.name().equals(distribusjonsKanal.name()) ? mapAdresse(distribuerJournalpostRequestTo.getAdresse()) : null);
		distribusjonbestilling.setDokumentProdApp(distribuerJournalpostRequestTo.getDokumentProdApp());
		distribusjonbestilling.setDokumenter(IntStream
				.range(0, dokumenter.size())
				.mapToObj(i -> mapDokumentInformasjon(dokumenter, i))
				.collect(Collectors.toList()));

		HentDokumenterFraJoark hentDokumenterFraJoark = new HentDokumenterFraJoark();
		hentDokumenterFraJoark.setDistribusjonbestilling(distribusjonbestilling);
		return hentDokumenterFraJoark;
	}

	private static DokumentInformasjon mapDokumentInformasjon(List<Journalpost.DokumentInfo> dokumenter, int i) {
		Journalpost.DokumentInfo dokumentInfo = dokumenter.get(i);
		DokumentInformasjon dokumentInformasjon = new DokumentInformasjon();
		dokumentInformasjon.setDokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID);
		dokumentInformasjon.setTilknyttetSom(i == 0 ? HOVEDDOKUMENT.name() : VEDLEGG.name());
		dokumentInformasjon.setVariantFormat(dokumentInfo.getDokumentvarianter().stream()
				.anyMatch(dokumentvariant -> (dokumentvariant.getVariantformat().equals(Variantformat.SLADDET) && dokumentvariant.isSaksbehandlerHarTilgang())) ? SLADDET : ARKIV);
		dokumentInformasjon.setArkivDokumentInfoId(dokumentInfo.getDokumentInfoId());
		dokumentInformasjon.setRekkefolge(i + 1);
		return dokumentInformasjon;
	}

	private static ArkivInformasjon mapArkivInformasjon(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivId(distribuerJournalpostRequestTo.getJournalpostId());
		arkivInformasjon.setArkivSystem(ArkivSystemCode.JOARK.name());
		return arkivInformasjon;
	}

	private String mapBatchId(String batchId) {
		return isBlank(batchId) ? null : batchId;
	}

	private Adresse mapAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (isNull(adresseTo)) {
			throw new ValidationException("Adresse kan ikke være null");
		} else if (adresseTo.getAdressetype().equals(NORSK_POSTADRESSE)) {
			NorskPostadresse norskPostadresse = new NorskPostadresse();
			norskPostadresse.setAdresselinje1(trimAdresselinje(adresseTo.getAdresselinje1()));
			norskPostadresse.setAdresselinje2(trimAdresselinje(adresseTo.getAdresselinje2()));
			norskPostadresse.setAdresselinje3(trimAdresselinje(adresseTo.getAdresselinje3()));
			norskPostadresse.setPostnummer(adresseTo.getPostnummer());
			norskPostadresse.setPoststed(adresseTo.getPoststed());
			norskPostadresse.setLand(adresseTo.getLand());
			return norskPostadresse;
		} else {
			UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
			utenlandskPostadresse.setAdresselinje1(adresseTo.getAdresselinje1());
			utenlandskPostadresse.setAdresselinje2(isBlank(adresseTo.getAdresselinje2()) ? null : adresseTo.getAdresselinje2());
			utenlandskPostadresse.setAdresselinje3(adresseTo.getAdresselinje3());
			utenlandskPostadresse.setLand(adresseTo.getLand());
			return utenlandskPostadresse;
		}
	}

	private String trimAdresselinje(String adresselinje) {
		return isBlank(adresselinje) ? null : adresselinje.strip();
	}

	private Aktoer mapBruker(Journalpost.Bruker bruker) {
		return switch (bruker.getType()) {
			case AKTOERID -> {
				AktoerId aktoerId = new AktoerId();
				aktoerId.setAktoerId(bruker.getId());
				yield aktoerId;
			}
			case FNR -> {
				Person person = new Person();
				person.setPersonidentifikator(bruker.getId());
				yield person;
			}
			case ORGNR -> {
				Organisasjon organisasjon = new Organisasjon();
				organisasjon.setOrgnummer(bruker.getId());
				yield organisasjon;
			}
		};
	}

	private String mapDistribusjonstidspunkt(String distribusjonstidspunkt) {
		return (isNotBlank(distribusjonstidspunkt) && isValidEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt)) ?
				getEnumIgnoreCase(DistribusjonstidspunktCode.class, distribusjonstidspunkt).name() : null;
	}

	private String mapDistribusjonstype(String distribusjonstype) {
		return (isNotBlank(distribusjonstype) && isValidEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype)) ?
				getEnumIgnoreCase(DistribusjonstypeCode.class, distribusjonstype).name() : null;
	}
}
