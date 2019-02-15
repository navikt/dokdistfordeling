package no.nav.dokdistfordeling.qdist008;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.dokdistfordeling.kodeverk.TemaCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;

import java.util.List;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */

@Value
@Builder
class DistribuerForsendelseTo {

	private final DistribusjonbestillingTo distribusjonbestilling;

	@Value
	@Builder
	static class DistribusjonbestillingTo {
		private final String bestillingsId;
		private final String batchId;
		private final String bestillendeFagsystem;
		private final TemaCode tema;
		private final String forsendelseTittel;
		private final ArkivInformasjonTo arkivInformasjon;
		private final AktoerTo mottaker;
		private final AdresseTo adresse;
		private final String dokumentProdApp;
		private final List<DokumentInformasjonTo> dokumenter;
	}

	@Getter
	@AllArgsConstructor
	abstract static class AktoerTo {
		private final String navn;
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	static class OrganisasjonTo extends AktoerTo {
		private final String orgnummer;

		@Builder
		public OrganisasjonTo(String navn, String orgnummer) {
			super(navn);
			this.orgnummer = orgnummer;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(exclude = "personidentifikator")
	@Value
	static class PersonTo extends AktoerTo {
		private final String personidentifikator;

		@Builder
		public PersonTo(String navn, String personidentifikator) {
			super(navn);
			this.personidentifikator = personidentifikator;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	static class AktoerIdTo extends AktoerTo {
		private final String identifikator;

		@Builder
		public AktoerIdTo(String navn, String identifikator) {
			super(navn);
			this.identifikator = identifikator;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	static class SamhandlerTo extends AktoerTo {
		private final String samhandleridentifikator;
		private final SamhandlerKategoriCode samhandlerkategori;

		@Builder
		public SamhandlerTo(String navn, String samhandleridentifikator, SamhandlerKategoriCode samhandlerkategori) {
			super(navn);
			this.samhandleridentifikator = samhandleridentifikator;
			this.samhandlerkategori = samhandlerkategori;
		}
	}


	@Getter
	@AllArgsConstructor
	abstract static class AdresseTo {
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String land;
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	static class NorskPostadresseTo extends AdresseTo {
		private final String postnummer;
		private final String poststed;

		@Builder
		public NorskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land, String postnummer, String poststed) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
			this.postnummer = postnummer;
			this.poststed = poststed;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	static class UtenlandskPostadresseTo extends AdresseTo {
		@Builder
		public UtenlandskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
		}
	}

	@Value
	@Builder
	static class ArkivInformasjonTo {
		private final ArkivSystemCode arkivSystem;
		private final String arkivKode;
	}

	@Value
	@Builder
	static class DokumentInformasjonTo {
		private final String dokumenttypeId;
		private final String dokumentObjektReferanse;
		private final TilknyttetSomCode tilknyttetSom;
		private final String arkivDokumentInfoId;
		private final int rekkefolge;
	}

}
