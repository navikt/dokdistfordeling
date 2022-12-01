package no.nav.dokdistfordeling.consumer.rdist001;

public interface AdministrerForsendelse {

	PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo persisterForsendelseRequestTo);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId);
}
