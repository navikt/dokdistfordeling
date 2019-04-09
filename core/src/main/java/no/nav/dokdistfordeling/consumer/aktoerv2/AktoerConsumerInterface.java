package no.nav.dokdistfordeling.consumer.aktoerv2;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AktoerConsumerInterface {

	HentIdentForAktoerIdResponseTo hentIdentForAktoerId(final String aktoerId);
}
