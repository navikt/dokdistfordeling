package no.nav.dokdistfordeling.consumer.rdist001;

public interface AdministrerForsendelse {

	String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequestTo);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId);
}
