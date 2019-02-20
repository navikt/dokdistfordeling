package no.nav.dokdistfordeling.qdist008;

import no.nav.meldinger.virksomhet.dokdistfordeling.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.Person;
import org.junit.jupiter.api.Test;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
//Todo Implement me
public class DistribuerForsendelseMapperTest {

	public static final String BESTILLINGS_ID = "bestillingsId";
	public static final String BATCH_ID = "batchId";
	public static final String BESTILLENDE_FAGSÆYSEM = "bestillendeFagsystem";
	public static final String TEMA = "tema";
	public static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	public static final String ARKIV_SYSTEM = "arkivsystem";
	public static final String ARKIV_ID = "arkivId";
	public static final String PERSON_NAVN = "personNavn";
	public static final String PERSON_IDENTIFIKATOR = "personId";

	public static final String DOKUMENT_PROD_APP = "dokumentProdApp";

	private DistribuerForsendelseMapper distribuerForsendelseMapper = new DistribuerForsendelseMapper();

	@Test
	public void shouldMap() {
//		distribuerForsendelseMapper.map()
	}

	private DistribuerForsendelse createDistribuerForsendelse() {
		return new DistribuerForsendelse()
				.withDistribusjonbestilling(new Distribusjonbestilling()
						.withBestillingsId(BESTILLINGS_ID)
						.withBatchId(BATCH_ID)
						.withBestillendeFagsystem(BESTILLENDE_FAGSÆYSEM)
						.withTema(TEMA)
						.withForsendelseTittel(FORSENDELSE_TITTEL)
						.withArkivInformasjon(new ArkivInformasjon()
								.withArkivId(ARKIV_ID)
								.withArkivSystem(ARKIV_SYSTEM))
						.withMottaker(new Person()
								.withNavn(PERSON_NAVN)
								.withPersonidentifikator(PERSON_IDENTIFIKATOR))
						.withDokumentProdApp(DOKUMENT_PROD_APP)
						.withDokumenter()
				);
	}
}
