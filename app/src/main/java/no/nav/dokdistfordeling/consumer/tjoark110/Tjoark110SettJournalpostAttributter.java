package no.nav.dokdistfordeling.consumer.tjoark110;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;

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
public class Tjoark110SettJournalpostAttributter {
	private final ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1;
	private final static int RETRIES = 3;

	@Inject
	public Tjoark110SettJournalpostAttributter(ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1) {
		this.arkiverDokumentproduksjonV1 = arkiverDokumentproduksjonV1;
	}

	@Retryable(value = DokdistfordelingTechnicalException.class, maxAttempts = RETRIES, backoff = @Backoff(delay = 500))
	public void settJournalpostAttributter(SettJournalpostAttributterRequestTo settJournalpostAttributterRequestTo) {
		try {
			arkiverDokumentproduksjonV1.settJournalpostAttributter(mapRequest(settJournalpostAttributterRequestTo));
		} catch (Exception e) {
			throw new DokdistfordelingTechnicalException("teknisk feil ved kall mot arkiverDokumentproduksjonV1:settJournalpostAttributter. Antall retries=" + RETRIES + ", journalpostId=" + settJournalpostAttributterRequestTo
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
