package no.nav.dokdistfordeling.constants;

import java.util.Set;

public final class Constants {

	public static final String CALL_ID = "callId";
	public static final String CONSUMER_ID = "consumerId";
	public static final String USER_ID = "userId";
	public static final String BESTILLINGS_ID = "bestillingsId";
	public static final String JOURNALPOST_ID = "journalpostId";
	public static final String DITT_NAV = "DITT_NAV";
	public static final String DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID = "U000001";
	public static final String DOKDISTBESTILLINGS_ID = "dokdistBestillingsId";
	public static final String HEADER_NAV_CALLID = "Nav-CallId";
	public static final String BEARER_PREFIX = "Bearer";

	public static final Set<String> ALL_MDC_KEYS = Set.of(CALL_ID);

	private Constants() {
	}
}
