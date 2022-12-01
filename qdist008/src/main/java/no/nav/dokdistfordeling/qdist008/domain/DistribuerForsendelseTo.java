package no.nav.dokdistfordeling.qdist008.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;

import java.util.List;

import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;

@Value
@Builder
public class DistribuerForsendelseTo {

	DistribusjonbestillingTo distribusjonbestilling;

	@Value
	@Builder
	public static class DistribusjonbestillingTo {
		String bestillingsId;
		String batchId;
		String distribusjonKanal;
		String bestillendeFagsystem;
		String tema;
		String forsendelseTittel;
		DistribusjonstypeCode distribusjonstype;
		DistribusjonstidspunktCode distribusjonstidspunkt;
		ArkivInformasjonTo arkivInformasjon;
		AktoerTo mottaker;
		AktoerTo bruker;
		AdresseTo adresse;
		String dokumentProdApp;
		List<DokumentInformasjonTo> dokumenter;
	}

	@Value
	@Builder
	public static class AktoerTo {
		String identifikator;
		String navn;
		boolean identifikatorAktoerId;
		AktoerTypeCode aktoerType;

		public boolean isSamhandler() {
			return this.getAktoerType() == SAMHANDLER_HPR
					|| this.aktoerType == SAMHANDLER_UTL_ORG
					|| this.aktoerType == SAMHANDLER_UKJENT;
		}

	}

	@Getter
	@AllArgsConstructor
	public abstract static class AdresseTo {
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String land;
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	public static class NorskPostadresseTo extends AdresseTo {
		String postnummer;
		String poststed;

		@Builder
		public NorskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land, String postnummer, String poststed) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
			this.postnummer = postnummer;
			this.poststed = poststed;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	public static class UtenlandskPostadresseTo extends AdresseTo {
		@Builder
		public UtenlandskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
		}
	}

	@Value
	@Builder
	public static class ArkivInformasjonTo {
		ArkivSystemCode arkivSystem;
		String arkivId;
	}

	@Value
	@Builder
	public static class DokumentInformasjonTo {
		String dokumenttypeId;
		String dokumentObjektReferanse;
		TilknyttetSomCode tilknyttetSom;
		String arkivDokumentInfoId;
		int rekkefolge;
	}

}
