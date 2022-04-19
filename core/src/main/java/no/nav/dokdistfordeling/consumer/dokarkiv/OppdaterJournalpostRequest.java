package no.nav.dokdistfordeling.consumer.dokarkiv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OppdaterJournalpostRequest {

	private List<Tilleggsopplysning> tilleggsopplysninger;

	@Builder
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Tilleggsopplysning {
		private String nokkel;
		private String verdi;
	}
}