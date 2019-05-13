package no.nav.dokdistfordeling.storageaws;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.storageaws.AwsS3Configuration.BUCKET_NAME;

import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Inject;
import java.util.Optional;


public class AwsS3Storage implements AwsStorage {

	private AmazonS3 s3WithStrictEncryption;

	@Inject
	public AwsS3Storage(AmazonS3 s3Encryption) {
		this.s3WithStrictEncryption = s3Encryption;
	}


	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void put(String key, String value) {
		s3WithStrictEncryption.putObject(BUCKET_NAME, key, value);
	}

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public Optional<String> get(String key) {
		return Optional.of(s3WithStrictEncryption.getObjectAsString(BUCKET_NAME, key));
	}

	public void delete(String key) {
		throw new UnsupportedOperationException("dokdistfordeling støtter ikke sletting av objekter fra dokdistmellomlager");
	}

}
