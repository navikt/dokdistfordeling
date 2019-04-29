package no.nav.dokdistfordeling.qdist012;

import static no.nav.dokdistfordeling.qdist012.Qdist012Route.PROPERTY_BESTILLINGS_ID;

import no.nav.dokdistfordeling.crypto.Crypto;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class HentDokumenterFraJoarkDecrypter {

	private final String encryptionPassphrase;

	public HentDokumenterFraJoarkDecrypter(@Value("${hentdokumenter_fra_joark_crypto_password}") String encryptionPassphrase) {
		this.encryptionPassphrase = encryptionPassphrase;
	}

	@Handler
	public String decrypt(String hentDokumenterFraJoarkEncrypted, Exchange exchangeProperty) {
		return new Crypto(encryptionPassphrase, exchangeProperty.getProperty(PROPERTY_BESTILLINGS_ID, String.class))
				.decrypt(hentDokumenterFraJoarkEncrypted);
	}
}
