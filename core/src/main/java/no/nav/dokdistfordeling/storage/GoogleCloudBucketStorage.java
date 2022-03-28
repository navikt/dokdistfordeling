package no.nav.dokdistfordeling.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.crypto.tink.Aead;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.technical.FailedBucketUploadTechnicalException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static java.lang.String.format;

/**
 * Google Cloud Storage implementasjon av {@link BucketStorage}
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
public class GoogleCloudBucketStorage implements BucketStorage {

	private static final byte[] ASSOCIATED_DATA = "dokdistmellomlager".getBytes();
	private final String bucket;
	private final Storage storage;
	private final Aead aead;

	public GoogleCloudBucketStorage(String bucket,
									Storage storage,
									Aead aead) {
		this.bucket = bucket;
		this.storage = storage;
		this.aead = aead;
	}

	@Override
	public void upload(String objectName, String payload) {
		try {
			byte[] encryptedValue = aead.encrypt(payload.getBytes(), ASSOCIATED_DATA);
			BlobId blobId = BlobId.of(bucket, objectName);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
			storage.createFrom(blobInfo, new ByteArrayInputStream(encryptedValue));
			log.info("Lastet opp objectName={} til bucket={} i Google Cloud Storage.", objectName, bucket);
		} catch (IOException | GeneralSecurityException | StorageException e) {
			throw new FailedBucketUploadTechnicalException(format("Feilet å laste opp dokument til Google Cloud Storage. objectName=%s", objectName), e);
		}
	}

	@Override
	public boolean exists(String objectName) {
		BlobId blobId = BlobId.of(bucket, objectName);
		return storage.get(blobId).exists();
	}
}
