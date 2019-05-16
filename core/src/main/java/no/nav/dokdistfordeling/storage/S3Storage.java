package no.nav.dokdistfordeling.storage;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.storage.S3Configuration.BUCKET_NAME;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.S3FailedToGetDocumentTechnicalException;
import no.nav.dokdistfordeling.exception.technical.S3FailedToPutDocumentTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Inject;


public class S3Storage implements Storage {

	private AmazonS3 s3WithStrictEncryption;

	@Inject
	public S3Storage(AmazonS3 s3Encryption) {
		this.s3WithStrictEncryption = s3Encryption;
	}

	@Override
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void put(String key, String value) {
		try {
			s3WithStrictEncryption.putObject(BUCKET_NAME, key, value);
		} catch (SdkClientException e) {
			throw new S3FailedToPutDocumentTechnicalException(String.format("Teknisk feil mot AmazonS3 ved lagring på key=%s", key), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String get(String key) {
		try {
			return s3WithStrictEncryption.getObjectAsString(BUCKET_NAME, key);
		} catch (SdkClientException e) {
			throw new S3FailedToGetDocumentTechnicalException(String.format("Teknisk feil mot AmazonS3 ved henting på key=%s. feilmelding=%s", key, e
					.getMessage()), e);
		} catch (SecurityException e) {
			throw new S3FailedToGetDocumentTechnicalException(String.format("Objektet som ble forsøkt hentet fra AmazonS3 på key=%s var ikke kryptert.", key), e);
		}
	}

	@Override
	public void delete(String key) {
		throw new UnsupportedOperationException("dokdistfordeling støtter ikke sletting av objekter fra dokdistmellomlager");
	}
}
