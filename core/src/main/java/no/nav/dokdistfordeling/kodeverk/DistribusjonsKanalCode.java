package no.nav.dokdistfordeling.kodeverk;

/**
 * @author Sigurd Midttun, Visma Consulting.
 * <p>
 * Distribusjonskanaler støttet av dokdistfordeling
 */
public enum DistribusjonsKanalCode {

	PRINT("S"),
	SDP("SDP"),
	DITTNAV("NAV_NO"),
	LOKAL_PRINT("L"),
	INGEN_DISTRIBUSJON("INGEN_DISTRIBUSJON"),
	TRYGDERETTEN("TRYGDERETTEN");

	private final String joarkUtsendingsKanal;

	DistribusjonsKanalCode(String joarkUtsendingsKanal) {
		this.joarkUtsendingsKanal = joarkUtsendingsKanal;
	}

	public String getJoarkUtsendingsKanal() {
		return this.joarkUtsendingsKanal;
	}
}