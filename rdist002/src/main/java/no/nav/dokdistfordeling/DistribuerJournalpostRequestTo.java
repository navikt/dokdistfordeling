package no.nav.dokdistfordeling;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@Schema
public class DistribuerJournalpostRequestTo {
	@Schema(name = "journalpostId", description = "Journalpost som skal distribueres", example = "343752389")
	String journalpostId;
	@Schema(name = "batchId", description = "Identifiserer batch som forsendelsen inngår i. Lar bestiller identifisere forsendelser som hører sammen. Fritekst, og konsument må selv vurdere hva som er hensiktsmessige verdier", example = "54321")
	String batchId;
	@Schema(name = "bestillendeFagsystem", description = "Fagsystemet som bestiller distribusjon", example = "SYM")
	String bestillendeFagsystem;
	@Schema(name = "adresse", description = "Struktur for å beskrive postadresse. Inneholder enten norsk postadresse eller utenlandsk postadresse. Påkrevd hvis mottaker er samhandler, ellers skal dokdistsentralprint hente adresse fra fellesregistre hvis ikke satt")
	AdresseTo adresse;
	@Schema(name = "dokumentProdApp", description = "Applikasjon som har produsert hoveddokumentet (for sporing og feilsøking)", example = "ELIN_STANDARD")
	String dokumentProdApp;
	@Schema(name = "distribusjonstype", description = "Forteller dokumentdistribusjon hva slags dokument som distribueres. \"VEDTAK\", \"VIKTIG\" eller \"ANNET\"", example = "VEDTAK", required = true)
	String distribusjonstype;
	@Schema(name = "distribusjonstidspunkt", description = "Forteller dokumentdistribusjon når dokumentet kan distribueres. \"UMIDDELBART\" eller \"KJERNETID\"", example = "UMIDDELBART", required = true)
	String distribusjonstidspunkt;
	@Schema(name = "tvingSentralPrint", description = "Settes til TRUE dersom forsendelsen skal sendes til sentral print, uten å sjekke om mottaker er digitalt tilgjengelig", defaultValue = "false", hidden = true)
	boolean tvingSentralPrint;

	@Builder
	@Getter
	@AllArgsConstructor
	public static class AdresseTo {
		@Schema(name = "adressetype", description = "\"norskPostadresse\" eller \"utenlandskPostadresse\"", example = "norskPostadresse")
		private final String adressetype;
		@Schema(name = "postnummer", description = "Påkrevd hvis adressetype = \"norskPostadresse\"", example = "0505")
		private final String postnummer;
		@Schema(name = "poststed", description = "Påkrevd hvis adressetype = \"norskPostadresse\"", example = "Oslo")
		private final String poststed;
		@Schema(name = "adresselinje1", description = "Påkrevd hvis adressetype = \"utenlandskPostadresse\"", example = "Eksempelveien 11B")
		private final String adresselinje1;
		@Schema(name = "adresselinje2", description = "Alternativ postadresse 2", example = "Bolignummer H0101")
		private final String adresselinje2;
		@Schema(name = "adresselinje3", description = "Alternativ postadresse 3", example = "Adresselinje3")
		private final String adresselinje3;
		@Schema(name = "land", description = "To-bokstavers landkode ihht iso3166-1 alfa-2", example = "NO")
		private final String land;
	}
}
