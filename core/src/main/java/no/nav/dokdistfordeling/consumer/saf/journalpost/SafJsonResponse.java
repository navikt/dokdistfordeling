package no.nav.dokdistfordeling.consumer.saf.journalpost;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class SafJsonResponse {
	private DataJournalpost data;
	private List<Error> errors;

	@Data
	public static class DataJournalpost {
		private SafJournalpostTo journalpost;
	}

	@Data
	@JsonIgnoreProperties({"locations", "path"})
	public static class Error {
		private String message;
		private Extension extensions;
	}

	@Data
	public static class Extension {
		private String code;
		private String classification;
	}
}
