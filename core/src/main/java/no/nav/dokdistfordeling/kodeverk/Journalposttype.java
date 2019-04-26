package no.nav.dokdistfordeling.kodeverk;

import java.util.Arrays;
import java.util.List;

public enum Journalposttype {
	I,
	U,
	N;

	public static List<Journalposttype> asList() {
		return Arrays.asList(values());
	}
}
