package no.nav.dokdistfordeling.consumer.tjoark110;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;
import static no.nav.dokdistfordeling.util.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.util.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistfordeling.util.RetryConstants.MULTIPLIER_SHORT;

import no.nav.dokdistfordeling.exception.DokdistfordelingTechnicalException;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.ArkiverDokumentproduksjonV1;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.meldinger.SettJournalpostAttributterRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class SettJournalpostAttributterConsumer implements ArkiverDokumentproduksjon {
	private final ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1;

	@Inject
	public SettJournalpostAttributterConsumer(ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1) {
		this.arkiverDokumentproduksjonV1 = arkiverDokumentproduksjonV1;
	}

	@Retryable(include = DokdistfordelingTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void settJournalpostAttributter(final SettJournalpostAttributterRequestTo settJournalpostAttributterRequestTo) {
		try {
			arkiverDokumentproduksjonV1.settJournalpostAttributter(mapRequest(settJournalpostAttributterRequestTo));
		} catch (Exception e) {
			throw new DokdistfordelingTechnicalException("teknisk feil ved kall mot arkiverDokumentproduksjonV1:settJournalpostAttributter. Antall retries=" + MAX_ATTEMPTS_SHORT + ", journalpostId=" + settJournalpostAttributterRequestTo
					.getJournalpostId(), e);
		}
	}

	private SettJournalpostAttributterRequest mapRequest(SettJournalpostAttributterRequestTo settJournalpostAttributterRequestTo) {
		return new SettJournalpostAttributterRequest()
				.withJournalpostIdListe(Long.valueOf(settJournalpostAttributterRequestTo.getJournalpostId()))
				.withEndretAvNavn(SERVICE_ID)
				.withUtsendingskanal(settJournalpostAttributterRequestTo.getUtsendingskanal());
	}
}
