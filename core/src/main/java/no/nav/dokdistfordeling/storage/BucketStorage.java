package no.nav.dokdistfordeling.storage;

/**
 * Interaksjon med ekstern bucket lagring.
 */
public interface BucketStorage {
	/**
	 * Laster opp kryptert payload til ekstern bucket lagring.
	 *
	 * @param objectName Navn på objektet
	 * @param payload Objekt i klartekst
	 */
	void upload(String objectName, String payload);

	/**
	 * Sjekker om objectName finnes i bucket.
	 *
	 * @param objectName Navn på objektet
	 * @return true hvis objektet finnes i bucket. Ellers false.
	 */
	boolean exists(String objectName);
}
