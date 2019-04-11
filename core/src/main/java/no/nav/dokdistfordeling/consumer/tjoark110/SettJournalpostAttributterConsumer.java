package no.nav.dokdistfordeling.consumer.tjoark110;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SettJournalpostAttributterTechnicalException;
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

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void settJournalpostAttributter(final SettJournalpostAttributterRequestTo settJournalpostAttributterRequestTo, String endretAvNavn) {
		try {
			arkiverDokumentproduksjonV1.settJournalpostAttributter(mapRequest(settJournalpostAttributterRequestTo, endretAvNavn));
		} catch (Exception e) {
			throw new SettJournalpostAttributterTechnicalException("teknisk feil ved kall mot arkiverDokumentproduksjonV1:settJournalpostAttributter. Antall retries=" + MAX_ATTEMPTS_SHORT + ", journalpostId=" + settJournalpostAttributterRequestTo.getJournalpostId(), e);
		}
	}

	private SettJournalpostAttributterRequest mapRequest(SettJournalpostAttributterRequestTo settJournalpostAttributterRequestTo, String endretAvNavn) {
		return new SettJournalpostAttributterRequest()
				.withJournalpostIdListe(Long.valueOf(settJournalpostAttributterRequestTo.getJournalpostId()))
				.withEndretAvNavn(endretAvNavn)
				.withUtsendingskanal(settJournalpostAttributterRequestTo.getUtsendingskanal());
	}
}
