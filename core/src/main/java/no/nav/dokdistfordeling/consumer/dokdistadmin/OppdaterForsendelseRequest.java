package no.nav.dokdistfordeling.consumer.dokdistadmin;

public record OppdaterForsendelseRequest(
		Long forsendelseId,
		String forsendelseStatus
) {
}