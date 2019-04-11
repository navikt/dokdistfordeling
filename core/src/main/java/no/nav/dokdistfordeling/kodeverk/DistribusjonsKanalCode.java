package no.nav.dokdistfordeling.kodeverk;

/**
 * @author Sigurd Midttun, Visma Consulting.
 * <p>
 * Distribusjonskanaler støttet av dokdistfordeling
 */
public enum DistribusjonsKanalCode {

	PRINT("S"),
	SDP("SDP"),
	DITT_NAV("NAV_NO"),
	LOKAL_PRINT("L"),
	INGEN_DISTRIBUSJON("INGEN_DISTRIBUSJON");

	private final String joarkUtsendingsKanal;

	DistribusjonsKanalCode(String joarkUtsendingsKanal) {
		this.joarkUtsendingsKanal = joarkUtsendingsKanal;
	}

	public String getJoarkUtsendingsKanal() {
		return this.joarkUtsendingsKanal;
	}
}