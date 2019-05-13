package no.nav.dokdistfordeling.storageaws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import no.nav.dokdistfordeling.exception.functional.CryptoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;


@Configuration
@Profile("nais")
public class AwsS3Configuration {

	private final static String REGION_TO_USE_FOR_S3_TO_WORK_ONPREM = "us-east-1";
	public static final String BUCKET_NAME = "dokdistmellomlager";

	private static SecretKey secretKey;

	@Value("${dokdistfordeling_s3_creds_username}")
	private String credsUsername;

	@Value("${dokdistfordeling_s3_creds_password}")
	private String credsPass;

	@Value("${storage_s3_url}")
	private String s3Endpoint;

	@Value("${dokdistmellomlager_s3_storage_crypto_password}")
	private String encryptionPassphrase;

	@Bean
	public AwsStorage awsStorage() {
		secretKey = key(credsPass, "someRandomSaltWhichIsSupposedMightComeFromDate?IDon'tReallyKnow");

		AmazonS3 s3 = s3(secretKey);

		ensureBucketExists(s3);
		configureBucketLifecycle(s3);
		configureBucketAccessPolicy(s3);

		return new AwsS3Storage(s3);
	}

	private AmazonS3 s3(SecretKey secretKey) {
		AWSCredentials credentials = new BasicAWSCredentials(credsUsername, credsPass);

		return AmazonS3EncryptionClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, REGION_TO_USE_FOR_S3_TO_WORK_ONPREM))
				.enablePathStyleAccess()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withCryptoConfiguration(new CryptoConfiguration(CryptoMode.StrictAuthenticatedEncryption))
				.withEncryptionMaterials(new StaticEncryptionMaterialsProvider(new EncryptionMaterials(secretKey)))
				.build();
	}

	private void ensureBucketExists(AmazonS3 s3) {
		boolean bucketExists = s3.listBuckets().stream()
				.anyMatch(b -> b.getName().equals(BUCKET_NAME));
		if (!bucketExists) {
			createBucket(s3);
		}
	}

	private void configureBucketLifecycle(AmazonS3 s3) {
		List<BucketLifecycleConfiguration.Rule> ruleList = new ArrayList<>();
		ruleList.add(new BucketLifecycleConfiguration.Rule()
				.withId("Object lifecycle rule")
				.withFilter(new LifecycleFilter())
				.withStatus(BucketLifecycleConfiguration.ENABLED)
				.withExpirationInDays(60));

		BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration();
		configuration.setRules(ruleList);

		s3.setBucketLifecycleConfiguration(BUCKET_NAME, configuration);
	}

	private void configureBucketAccessPolicy(AmazonS3 s3) {
		Statement allowDokarkivRead = new Statement(Statement.Effect.Allow)
				.withPrincipals(new Principal("arn:aws:iam:::user/dokdistsentralprint"))
				.withActions(S3Actions.GetObject)
				.withResources(new S3ObjectResource(BUCKET_NAME, "*"));
		Policy accessPolicy = new Policy().withStatements(allowDokarkivRead);
		s3.setBucketPolicy(BUCKET_NAME, accessPolicy.toJson());
	}

	private void createBucket(AmazonS3 s3) {
		s3.createBucket(new CreateBucketRequest(BUCKET_NAME)
				.withCannedAcl(CannedAccessControlList.Private));
	}

	private SecretKey key(String passphrase, String salt) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			char[] ***passord=gammelt_passord***();
			KeySpec spec = new PBEKeySpec(passwordChars, salt.getBytes(), 10000, 128);
			SecretKey secretKey = factory.generateSecret(spec);
			return new SecretKeySpec(secretKey.getEncoded(), "AES");
		} catch (Exception ex) {
			throw new CryptoException("Feilet ved generering av krypteringsnøkkel", ex);
		}
	}

	private boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
}
