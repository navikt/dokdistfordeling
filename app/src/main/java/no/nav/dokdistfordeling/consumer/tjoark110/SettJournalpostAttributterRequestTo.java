package no.nav.dokdistfordeling.consumer.tjoark110;

import lombok.Builder;
import lombok.Value;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Value
@Builder
public class SettJournalpostAttributterRequestTo {

	private final String journalpostId;
	private final String utsendingskanal;
}
