package no.nav.dokdistfordeling.consumer.aktoerv2;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AktoerV2 {

	HentIdentForAktoerIdResponseTo hentIdentForAktoerId(final String aktoerId);
}
