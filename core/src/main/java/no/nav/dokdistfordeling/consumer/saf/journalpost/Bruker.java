package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;

@Value
public class Bruker {
	private final String id;
	private final BrukerIdType type;
}
