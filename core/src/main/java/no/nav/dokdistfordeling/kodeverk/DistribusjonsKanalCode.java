package no.nav.dokdistfordeling.kodeverk;

public enum DistribusjonsKanalCode {

	PRINT("S"),
	SDP("SDP"),
	DITTNAV("NAV_NO"),
	LOKAL_PRINT("L"),
	INGEN_DISTRIBUSJON("INGEN_DISTRIBUSJON"),
	TRYGDERETTEN("TRYGDERETTEN"),
	DPVT("DPVT");

	private final String joarkUtsendingsKanal;

	DistribusjonsKanalCode(String joarkUtsendingsKanal) {
		this.joarkUtsendingsKanal = joarkUtsendingsKanal;
	}

	public String getJoarkUtsendingsKanal() {
		return this.joarkUtsendingsKanal;
	}
}