package no.nav.dokdistfordeling.qdist008.domain;

import no.nav.dokdistfordeling.consumer.rdist001.OpprettForsendelseRequestTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Component
public class OpprettForsendelseToRequestMapper {

	public OpprettForsendelseRequestTo map(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling,
										   DokumenttypeInfoTo dokumenttypeInfoTo,
										   String fnrMottaker,
										   DistribusjonKanalCode distribusjonsKanal) {
		final DistribuerForsendelseTo.AktoerTo mottaker = distribusjonbestilling.getMottaker();
		final DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon = distribusjonbestilling.getArkivInformasjon();
		final DistribuerForsendelseTo.AdresseTo adresse = distribusjonbestilling.getAdresse();
		final List<DistribuerForsendelseTo.DokumentInformasjonTo> dokumentInformasjonToList = distribusjonbestilling.getDokumenter();

		return OpprettForsendelseRequestTo.builder()
				.bestillingsId(distribusjonbestilling.getBestillingsId())
				.distribusjonsKanal(distribusjonsKanal)
				.bestillendeFagsystem(distribusjonbestilling.getBestillendeFagsystem())
				.tema(distribusjonbestilling.getTema())
				.forsendelseTittel(getForsendelseTittel(distribusjonbestilling, dokumenttypeInfoTo))
				.batchId(distribusjonbestilling.getBatchId())
				.dokumentProdApp(distribusjonbestilling.getDokumentProdApp())
				.mottaker(mapMottaker(mottaker, fnrMottaker))
				.arkivInformasjon(mapArkivInformasjon(arkivInformasjon))
				.distribusjonstype(isNull(distribusjonbestilling.getDistribusjonstype()) ? null : distribusjonbestilling.getDistribusjonstype().name())
				.distribusjonstidspunkt(isNull(distribusjonbestilling.getDistribusjonstidspunkt()) ? null : distribusjonbestilling.getDistribusjonstidspunkt().name())
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

	private OpprettForsendelseRequestTo.DokumentTo mapDokument(DistribuerForsendelseTo.DokumentInformasjonTo dokumentInformasjon) {
		return OpprettForsendelseRequestTo.DokumentTo.builder()
				.tilknyttetSom(dokumentInformasjon.getTilknyttetSom())
				.dokumentObjektReferanse(dokumentInformasjon.getDokumentObjektReferanse())
				.rekkefolge(dokumentInformasjon.getRekkefolge())
				.arkivDokumentInfoId(dokumentInformasjon.getArkivDokumentInfoId())
				.dokumenttypeId(dokumentInformasjon.getDokumenttypeId())
				.build();
	}

	private OpprettForsendelseRequestTo.MottakerTo mapMottaker(DistribuerForsendelseTo.AktoerTo mottaker, String fnrMottaker) {
		return OpprettForsendelseRequestTo.MottakerTo.builder()
				.mottakerId(getMottakerId(mottaker, fnrMottaker))
				.mottakerNavn(mottaker.getNavn())
				.mottakerType(mottaker.getAktoerType())
				.build();
	}

	private OpprettForsendelseRequestTo.ArkivInformasjonTo mapArkivInformasjon(DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon) {
		return arkivInformasjon == null ? null : OpprettForsendelseRequestTo.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private OpprettForsendelseRequestTo.PostadresseTo mapPostadresse(DistribuerForsendelseTo.AdresseTo adresse) {
		return adresse == null ? null : OpprettForsendelseRequestTo.PostadresseTo.builder()
				.adresselinje1(adresse.getAdresselinje1())
				.adresselinje2(adresse.getAdresselinje2())
				.adresselinje3(adresse.getAdresselinje3())
				.postnummer(getPostnummer(adresse))
				.poststed(getPoststed(adresse))
				.landkode(adresse.getLand())
				.build();
	}

	private String getMottakerId(DistribuerForsendelseTo.AktoerTo mottaker, String fnrMottaker) {
		return mottaker.isIdentifikatorAktoerId() ? fnrMottaker : mottaker.getIdentifikator();
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
