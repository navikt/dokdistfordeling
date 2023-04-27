package no.nav.dokdistfordeling.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
public class OppdaterForsendelseRequest {
	@PositiveOrZero(message = "forsendelseId må ha en verdi")
	private Long forsendelseId;
	private String forsendelseStatus;
	private String konversasjonId;
}