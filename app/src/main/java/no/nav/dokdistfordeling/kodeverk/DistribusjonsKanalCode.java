package no.nav.dokdistfordeling.kodeverk;

/**
 * @author Sigurd Midttun, Visma Consulting.
 * <p>
 * Distribusjonskanaler støttet av dokdistfordeling
 */
public enum DistribusjonsKanalCode {

	/**
	 * Sentralprint
	 */
	PRINT("S");

	private final String joarkUtsendingsKanal;

	DistribusjonsKanalCode(String joarkUtsendingsKanal) {
		this.joarkUtsendingsKanal = joarkUtsendingsKanal;
	}

	public String getJoarkUtsendingsKanal() {
		return this.joarkUtsendingsKanal;
	}
}
