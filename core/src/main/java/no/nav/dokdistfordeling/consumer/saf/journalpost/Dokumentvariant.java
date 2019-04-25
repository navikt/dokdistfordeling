package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.Variantformat;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Value
@Builder
public class Dokumentvariant {
	private final Variantformat variantformat;
	private final boolean saksbehandlerHarTilgang;
}
