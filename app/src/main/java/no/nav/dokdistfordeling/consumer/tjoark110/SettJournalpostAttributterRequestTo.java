package no.nav.dokdistfordeling.consumer.tjoark110;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Data
@Builder
@AllArgsConstructor
public class SettJournalpostAttributterRequestTo {
	private final String journalpostId;
	private final String utsendingskanal;
}
