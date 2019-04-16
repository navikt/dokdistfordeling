package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokument;
import no.nav.dokdistfordeling.storage.S3Storage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist012Service {

	private final HentDokument hentDokument;
	private final S3Storage s3Storage;

	@Inject
	public Qdist012Service(HentDokument hentDokument,
						   S3Storage s3Storage) {
		this.hentDokument = hentDokument;
		this.s3Storage = s3Storage;
	}

	@Handler
	public void copyDocumentsFromJoarkToDokdistmellomlagerS3Storage() {

	}
}
