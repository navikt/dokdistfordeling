package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface Forsendelse {

	ForsendelseResponseTo persisterForsendelse(final ForsendelseRequestTo forsendelseRequestTo);
}
