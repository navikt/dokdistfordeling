package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker;
import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PersonnummerService {

	private final PdlGraphQLConsumer pdlGraphQLConsumer;

	public PersonnummerService(PdlGraphQLConsumer pdlGraphQLConsumer) {
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
	}

	public String utledPersonnummer(Bruker bruker, boolean harPostadresse) {
		if (bruker.erTypeAktoerId()) {
			try {
				return hentPersonnummerForAktoerId(bruker.getId());
			} catch (PdlHentFolkeregisteridentForAktoerIdFunctionalException e) {
				if (harPostadresse) {
					log.info("Returnerer null som personnummer etter at mapping fra aktørid til fnr feilet for person med oppgitt postadresse.");
					return null;
				} else {
					throw e;
				}
			}
		} else {
			return bruker.getId();
		}
	}

	private String hentPersonnummerForAktoerId(String aktoerId) {
		return pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(aktoerId);
	}
}
