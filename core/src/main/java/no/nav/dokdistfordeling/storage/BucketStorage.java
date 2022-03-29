package no.nav.dokdistfordeling.storage;

/**
 * Interaksjon med ekstern bucket lagring.
 */
public interface BucketStorage {
	/**
	 * Laster opp kryptert payload til ekstern bucket lagring.
	 *
	 * @param objectName     Navn på objektet. GUID eller annen unik ID.
	 * @param payload        Objekt i klartekst. JSON string, base64 representert binærfil osv.
	 * @param associatedData Data som knyttes til objektet for å unngå manipulering. F.eks journalpostId, bestillingsId.
	 */
	void upload(String objectName, String payload, String associatedData);

	/**
	 * Sjekker om objectName finnes i bucket.
	 *
	 * @param objectName Navn på objektet
	 * @return true hvis objektet finnes i bucket. Ellers false.
	 */
	boolean exists(String objectName);
}
