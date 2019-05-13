package no.nav.dokdistfordeling.consumer.rdist001;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;

import java.util.List;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class PersisterForsendelseRequestTo {
	private final String bestillingsId;
	private final DistribusjonsKanalCode distribusjonsKanal;
	private final String bestillendeFagsystem;
	private final String tema;
	private final String forsendelseTittel;
	private final String batchId;
	private final String dokumentProdApp;
	private final MottakerTo mottaker;
	private final ArkivInformasjonTo arkivInformasjon;
	private final PostadresseTo postadresse;
	private final List<DokumentTo> dokumenter;


	@Value
	@Builder
	public static class MottakerTo {
		private final String mottakerId;
		private final String mottakerNavn;
		private final AktoerTypeCode mottakerType;
	}

	@Value
	@Builder
	public static class ArkivInformasjonTo {
		private final ArkivSystemCode arkivSystem;
		private final String arkivId;
	}

	@Value
	@Builder
	public static class PostadresseTo {
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String postnummer;
		private final String poststed;
		private final String landkode;
	}

	@Value
	@Builder
	public static class DokumentTo {
		private final TilknyttetSomCode tilknyttetSom;
		private final String dokumentObjektReferanse;
		private final int rekkefolge;
		private final String arkivDokumentInfoId;
		private final String dokumenttypeId;
	}
}
