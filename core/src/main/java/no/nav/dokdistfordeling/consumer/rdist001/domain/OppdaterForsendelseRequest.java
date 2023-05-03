package no.nav.dokdistfordeling.consumer.rdist001.domain;


public record OppdaterForsendelseRequest (
	Long forsendelseId,
	String forsendelseStatus
) {}