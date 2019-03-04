package no.nav.dokdistfordeling.consumer.aktoerv2;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.exception.DokdistfordelingTechnicalException;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@Component
@Slf4j
public class AktoerV2Consumer implements Aktoer {

	private final AktoerV2 aktoerV2;

	public AktoerV2Consumer(AktoerV2 aktoerV2) {
		this.aktoerV2 = aktoerV2;
	}

	@Retryable(include = DokdistfordelingTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentIdentForAktoerIdResponseTo hentIdentForAktoerId(final String aktoerId) {
		if (log.isDebugEnabled()) {
			log.debug("henter ident for identifikatorAktoerId={}", aktoerId);
		}
		HentIdentForAktoerIdRequest request = new HentIdentForAktoerIdRequest();
		request.setAktoerId(aktoerId);
		try {
			HentIdentForAktoerIdResponse response = aktoerV2.hentIdentForAktoerId(request);
			return HentIdentForAktoerIdResponseTo.builder()
					.foedselsnr(response.getIdent())
					.build();
		} catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
			throw new DokdistfordelingFunctionalException(String.format("Ident ikke funnet for aktoerId=%s", aktoerId), e);
		} catch (Exception e) {
			throw new DokdistfordelingTechnicalException(String.format("Teknisk feil mot aktoerv2:HentIdentForAktoerId. AktoerId=%s. Feilmelding=%s", aktoerId, e
					.getMessage()), e);
		}
	}
}
