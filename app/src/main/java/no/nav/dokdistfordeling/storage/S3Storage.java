package no.nav.dokdistfordeling.storage;

import static java.util.stream.Collectors.joining;
import static no.nav.dokdistfordeling.storage.config.StorageConfiguration.BUCKET_NAME;
import static no.nav.dokdistfordeling.util.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.util.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistfordeling.util.RetryConstants.MULTIPLIER_SHORT;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.DokdistfordelingTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

@Slf4j
public class S3Storage implements Storage {

	private AmazonS3 s3;
	private String encryptionPassphrase;

	@Inject
	public S3Storage(AmazonS3 s3, String encryptionPassphrase) {
		this.s3 = s3;
		this.encryptionPassphrase = encryptionPassphrase;
	}

	@Override
	@Retryable(include = DokdistfordelingTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void put(String directory, String key, String value) {
		throw new UnsupportedOperationException("dokdistfordeling støtter ikke persistering av objekter til dokdistmellomlager");
	}

	@Override
	@Retryable(include = DokdistfordelingTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public Optional<String> get(String directory, String key) {

		try {
			String encryptedValue = readString(key);
			return Optional.ofNullable(decrypt(encryptedValue, key));
		} catch (Exception e) {
			throw new DokdistfordelingTechnicalException(String.format("Feilet ved henting av dokument fra S3-bucketen dokdistmellomlager. Nøkkel=%s", key), e);
		}
	}

	@Override
	public void delete(String directory, String key) {
		throw new UnsupportedOperationException("dokdistfordeling støtter ikke sletting av objekter fra dokdistmellomlager");
	}

	private String readString(String key) {
		S3Object object;
		try {
			object = s3.getObject(BUCKET_NAME, key);
		} catch (AmazonS3Exception ex) {
			log.warn("Kunne ikke hente objekt fra dokdistmellomlager med nøkkel={}. Årsaken er sanssynligvis at objektet ikke finnes.");
			return null;
		}

		return new BufferedReader(new InputStreamReader(object.getObjectContent()))
				.lines()
				.collect(joining("\n"));
	}

	//Fixme
	private String decrypt(String encrypted, String key) {
		return null;
//		return new Crypto(encryptionPassphrase, key).decrypt(encrypted);
	}

}
