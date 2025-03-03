package no.nav.dokdistfordeling.domain;

import lombok.Builder;

@Builder
public record Adresse(String adressetype,
					  String postnummer,
					  String poststed,
					  String adresselinje1,
					  String adresselinje2,
					  String adresselinje3,
					  String land) {
}
