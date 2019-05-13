package no.nav.dokdistfordeling;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "DistribuerJournalpostRequestTo model")
public class DistribuerJournalpostRequestTo {
	@ApiModelProperty(name = "journalpostId", value = "Journalpost som skal distribueres", example = "343752389")
	private final String journalpostId;
	@ApiModelProperty(name = "batchId", value = "Identifiserer batch som forsendelsen inngår i. Lar bestiller identifisere forsendelser som hører sammen. Fritekst, og konsument må selv vurdere hva som er hensiktsmessige verdier", example = "54321", position = 1)
	private final String batchId;
	@ApiModelProperty(name = "bestillendeFagsystem", value = "Fagsystemet som bestiller distribusjon", example = "SYM", position = 2)
	private final String bestillendeFagsystem;
	@ApiModelProperty(name = "adresse", value = "Struktur for å beskrive postadresse. Inneholder enten norsk postadresse eller utenlandsk postadresse. Påkrevd hvis mottaker er samhandler, ellers skal dokdistsentralprint hente adresse fra fellesregistre hvis ikke satt", position = 3)
	private final AdresseTo adresse;
	@ApiModelProperty(name = "dokumentProdApp", value = "Applikasjon som har produsert hoveddokumentet (for sporing og feilsøking)", example = "ELIN_STANDARD", position = 4)
	private final String dokumentProdApp;

	@Getter
	@AllArgsConstructor
	public static class AdresseTo {
		@ApiModelProperty(name = "adresseType", value = "\"norskPostadresse\" eller \"utenlandskPostadresse\"", example = "norskPostadresse", position = 5)
		private final String adresseType;
		@ApiModelProperty(name = "postnummer", value = "Påkrevd hvis adressetype = \"norskPostadresse\"", example = "0505", position = 6)
		private final String postnummer;
		@ApiModelProperty(name = "postnummer", value = "Påkrevd hvis adressetype = \"norskPostadresse\"", example = "Oslo", position = 7)
		private final String poststed;
		@ApiModelProperty(name = "adresselinje1", value = "Påkrevd hvis adressetype = \"utenlandskPostadresse\"", example = "\"Eksempelveien 11B\"", position = 8)
		private final String adresselinje1;
		@ApiModelProperty(name = "adresselinje2", value = "Alternativ postadresse 2", example = "\"Eksempelveien 12B\"", position = 9)
		private final String adresselinje2;
		@ApiModelProperty(name = "adresselinje3", value = "Alternativ postadresse 3", example = "\"Eksempelveien 13B\"", position = 10)
		private final String adresselinje3;
		@ApiModelProperty(name = "land", value = "To-bokstavers landkode ihht iso3166-1 alfa-2", example = "NO", position = 11)
		private final String land;
	}
}
