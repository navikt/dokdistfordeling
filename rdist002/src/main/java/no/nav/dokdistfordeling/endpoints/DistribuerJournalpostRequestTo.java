package no.nav.dokdistfordeling.endpoints;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
public class DistribuerJournalpostRequestTo {
	private final String journalpostId;
	private final String batchId;
	private final String bestillendeFagsystem;
	private final AdresseTo adresse;
	private final String dokumentProdApp;

	@Getter
	@AllArgsConstructor
	public static class AdresseTo {
		private final String adresseType;
		private final String postnummer;
		private final String poststed;
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String land;
	}
}
