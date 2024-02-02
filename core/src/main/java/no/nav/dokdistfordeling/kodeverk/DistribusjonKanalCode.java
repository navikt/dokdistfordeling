package no.nav.dokdistfordeling.kodeverk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DistribusjonKanalCode {

	PRINT("S"),
	SDP("SDP"),
	DITTNAV("NAV_NO"),
	LOKAL_PRINT("L"),
	INGEN_DISTRIBUSJON("INGEN_DISTRIBUSJON"),
	TRYGDERETTEN("TRYGDERETTEN"),
	DPVT("DPVT");

	private final String joarkUtsendingsKanal;
}