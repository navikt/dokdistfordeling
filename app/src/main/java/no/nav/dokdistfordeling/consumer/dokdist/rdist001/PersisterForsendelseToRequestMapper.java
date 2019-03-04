package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.qdist008.DistribuerForsendelseTo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class PersisterForsendelseToRequestMapper {

	public PersisterForsendelseRequestTo map(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling,
											 DokumenttypeInfoTo dokumenttypeInfoTo,
											 HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo,
											 DistribusjonsKanalCode distribusjonsKanal) {
		final DistribuerForsendelseTo.MottakerTo mottaker = distribusjonbestilling.getMottaker();
		final DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon = distribusjonbestilling.getArkivInformasjon();
		final DistribuerForsendelseTo.AdresseTo adresse = distribusjonbestilling.getAdresse();
		final List<DistribuerForsendelseTo.DokumentInformasjonTo> dokumentInformasjonToList = distribusjonbestilling.getDokumenter();

		return PersisterForsendelseRequestTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.distribusjonsKanal(distribusjonsKanal)
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(distribusjonbestilling.getTema())
				.forsendelseTittel(getForsendelseTittel(distribusjonbestilling, dokumenttypeInfoTo))
				.batchId(distribusjonbestilling.getBatchId())
				.dokumentProdApp(distribusjonbestilling.getDokumentProdApp())
				.mottaker(mapMottaker(mottaker, hentIdentForAktoerIdResponseTo))
				.arkivInformasjon(mapArkivInformasjon(arkivInformasjon))
				.postadresse(mapPostadresse(adresse))
				.dokumenter(dokumentInformasjonToList.stream()
						.map(this::mapDokument)
						.collect(Collectors.toList()))
				.build();
	}

	private String getForsendelseTittel(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling, DokumenttypeInfoTo dokumenttypeInfoTo) {
		return distribusjonbestilling.getForsendelseTittel() == null ? dokumenttypeInfoTo.getDokumentTittel() : distribusjonbestilling
				.getForsendelseTittel();
	}

	private PersisterForsendelseRequestTo.DokumentTo mapDokument(DistribuerForsendelseTo.DokumentInformasjonTo dokumentInformasjon) {
		return PersisterForsendelseRequestTo.DokumentTo.builder()
				.tilknyttetSom(dokumentInformasjon.getTilknyttetSom())
				.dokumentObjektReferanse(dokumentInformasjon.getDokumentObjektReferanse())
				.rekkefolge(dokumentInformasjon.getRekkefolge())
				.arkivDokumentInfoId(dokumentInformasjon.getArkivDokumentInfoId())
				.dokumenttypeId(dokumentInformasjon.getDokumenttypeId())
				.build();
	}

	private PersisterForsendelseRequestTo.MottakerTo mapMottaker(DistribuerForsendelseTo.MottakerTo mottaker, HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo) {
		return PersisterForsendelseRequestTo.MottakerTo.builder()
				.mottakerId(getMottakerId(mottaker, hentIdentForAktoerIdResponseTo))
				.mottakerNavn(mottaker.getNavn())
				.mottakerType(mottaker.getMottakerType())
				.build();
	}

	private PersisterForsendelseRequestTo.ArkivInformasjonTo mapArkivInformasjon(DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon) {
		return arkivInformasjon == null ? null : PersisterForsendelseRequestTo.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private PersisterForsendelseRequestTo.PostadresseTo mapPostadresse(DistribuerForsendelseTo.AdresseTo adresse) {
		return adresse == null ? null : PersisterForsendelseRequestTo.PostadresseTo.builder()
				.adresselinje1(adresse.getAdresselinje1())
				.adresselinje2(adresse.getAdresselinje2())
				.adresselinje3(adresse.getAdresselinje3())
				.postnummer(getPostnummer(adresse))
				.poststed(getPoststed(adresse))
				.landkode(adresse.getLand())
				.build();
	}

	private String getMottakerId(DistribuerForsendelseTo.MottakerTo mottaker, HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo) {
		return mottaker.isIdentifikatorAktoerId() ? hentIdentForAktoerIdResponseTo.getFoedselsnr() : mottaker.getIdentifikator();
	}

	private String getPostnummer(DistribuerForsendelseTo.AdresseTo adresse) {
		if (adresse instanceof DistribuerForsendelseTo.NorskPostadresseTo) {
			return ((DistribuerForsendelseTo.NorskPostadresseTo) adresse).getPostnummer();
		} else {
			return null;
		}
	}

	private String getPoststed(DistribuerForsendelseTo.AdresseTo adresse) {
		if (adresse instanceof DistribuerForsendelseTo.NorskPostadresseTo) {
			return ((DistribuerForsendelseTo.NorskPostadresseTo) adresse).getPoststed();
		} else {
			return null;
		}
	}

}
