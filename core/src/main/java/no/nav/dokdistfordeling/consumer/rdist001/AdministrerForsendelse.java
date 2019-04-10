package no.nav.dokdistfordeling.consumer.rdist001;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo persisterForsendelseRequestTo);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId);
}
