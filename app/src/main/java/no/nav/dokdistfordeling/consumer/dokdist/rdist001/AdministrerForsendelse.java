package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo persisterForsendelseRequestTo);
}
