package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.DokumentInfo;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
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
import java.util.stream.IntStream;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class HentDokumenterFraJoarkMapper {

	public static final String NORSK_POSTADRESSE = "norskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	private HentDokumenterFraJoarkMapper() {
	}

	public static HentDokumenterFraJoark map(DistribuerJournalpost distribuerJournalpost,
									  no.nav.dokdistfordeling.domain.Adresse adresse,
									  Journalpost journalpost,
									  Aktoer mottaker,
									  String bestillingsId,
									  DistribusjonKanalCode distribusjonsKanal) {

		List<DokumentInfo> dokumenter = journalpost.getDokumenter();

		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(bestillingsId);
		distribusjonbestilling.setBatchId(mapBatchId(distribuerJournalpost.batchId()));
		distribusjonbestilling.setDistribusjonKanal(distribusjonsKanal.name());
		distribusjonbestilling.setBestillendeFagsystem(distribuerJournalpost.bestillendeFagsystem());
		distribusjonbestilling.setTema(journalpost.getTema());
		distribusjonbestilling.setForsendelseTittel(journalpost.getTittel());
		distribusjonbestilling.setDistribusjonstype(distribuerJournalpost.distribusjonstype().name());
		distribusjonbestilling.setDistribusjonstidspunkt(distribuerJournalpost.distribusjonstidspunkt().name());
		distribusjonbestilling.setArkivInformasjon(mapArkivInformasjon(distribuerJournalpost));
		distribusjonbestilling.setMottaker(mottaker);
		distribusjonbestilling.setBruker(mapBruker(journalpost.getBruker()));
		distribusjonbestilling.setAdresse(mapAdresse(adresse));
		distribusjonbestilling.setDokumentProdApp(distribuerJournalpost.dokumentProdApp());

		distribusjonbestilling.setDokumenter(IntStream
				.range(0, dokumenter.size())
				.mapToObj(i -> mapDokumentInformasjon(dokumenter, i))
				.toList());

		HentDokumenterFraJoark hentDokumenterFraJoark = new HentDokumenterFraJoark();
		hentDokumenterFraJoark.setDistribusjonbestilling(distribusjonbestilling);
		return hentDokumenterFraJoark;
	}

	private static DokumentInformasjon mapDokumentInformasjon(List<DokumentInfo> dokumenter, int i) {
		DokumentInfo dokumentInfo = dokumenter.get(i);
		DokumentInformasjon dokumentInformasjon = new DokumentInformasjon();
		dokumentInformasjon.setDokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID);
		dokumentInformasjon.setTilknyttetSom(i == 0 ? HOVEDDOKUMENT.name() : VEDLEGG.name());
		dokumentInformasjon.setVariantFormat(dokumentInfo.getDokumentvarianter().stream()
				.anyMatch(dokumentvariant -> (dokumentvariant.getVariantformat().equals(Variantformat.SLADDET) && dokumentvariant.isSaksbehandlerHarTilgang())) ? SLADDET : ARKIV);
		dokumentInformasjon.setArkivDokumentInfoId(dokumentInfo.getDokumentInfoId());
		dokumentInformasjon.setRekkefolge(i + 1);
		return dokumentInformasjon;
	}

	private static ArkivInformasjon mapArkivInformasjon(DistribuerJournalpost distribuerJournalpost) {
		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivId(distribuerJournalpost.journalpostId());
		arkivInformasjon.setArkivSystem(ArkivSystemCode.JOARK.name());
		return arkivInformasjon;
	}

	private static String mapBatchId(String batchId) {
		return isBlank(batchId) ? null : batchId;
	}

	private static Adresse mapAdresse(no.nav.dokdistfordeling.domain.Adresse adresse) {
		if (adresse == null) {
			return null;
		} else if (adresse.adressetype().equals(NORSK_POSTADRESSE)) {
			NorskPostadresse norskPostadresse = new NorskPostadresse();
			norskPostadresse.setAdresselinje1(adresse.adresselinje1());
			norskPostadresse.setAdresselinje2(adresse.adresselinje2());
			norskPostadresse.setAdresselinje3(adresse.adresselinje3());
			norskPostadresse.setPostnummer(adresse.postnummer());
			norskPostadresse.setPoststed(adresse.poststed());
			norskPostadresse.setLand(adresse.land());
			return norskPostadresse;
		} else {
			UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
			utenlandskPostadresse.setAdresselinje1(adresse.adresselinje1());
			utenlandskPostadresse.setAdresselinje2(adresse.adresselinje2());
			utenlandskPostadresse.setAdresselinje3(adresse.adresselinje3());
			utenlandskPostadresse.setLand(adresse.land());
			return utenlandskPostadresse;
		}
	}

	private static Aktoer mapBruker(Journalpost.Bruker bruker) {
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
}
