package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Value
@Builder
public class AvsenderMottaker {
	private final String id;
	private final String navn;
	private final String land;
	private final boolean erLikBruker;
}
