package no.nav.dokdistfordeling.consumer.dokdistadmin;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;

import java.util.List;

@Value
@Builder
public class OpprettForsendelseRequestTo {
	String bestillingsId;
	DistribusjonKanalCode distribusjonsKanal;
	String bestillendeFagsystem;
	String tema;
	String forsendelseTittel;
	String batchId;
	String dokumentProdApp;
	MottakerTo mottaker;
	ArkivInformasjonTo arkivInformasjon;
	PostadresseTo postadresse;
	List<DokumentTo> dokumenter;
	String distribusjonstype;
	String distribusjonstidspunkt;
	String forsendelseMetadata;
	ForsendelseMetadataType forsendelseMetadataType;


	@Value
	@Builder
	public static class MottakerTo {
		String mottakerId;
		String mottakerNavn;
		AktoerTypeCode mottakerType;
	}

	@Value
	@Builder
	public static class ArkivInformasjonTo {
		ArkivSystemCode arkivSystem;
		String arkivId;
	}

	@Value
	@Builder
	public static class PostadresseTo {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}

	@Value
	@Builder
	public static class DokumentTo {
		TilknyttetSomCode tilknyttetSom;
		String dokumentObjektReferanse;
		int rekkefolge;
		String arkivDokumentInfoId;
		String dokumenttypeId;
	}
}
