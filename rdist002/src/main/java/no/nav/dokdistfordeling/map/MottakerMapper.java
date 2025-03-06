package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.UKJENT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class MottakerMapper {

	private MottakerMapper() {
	}

	public static Aktoer map(AvsenderMottaker avsenderMottaker) {
		return switch (avsenderMottaker.getType()) {
			case FNR -> {
				Person person = new Person();
				person.setNavn(avsenderMottaker.getNavn());
				person.setPersonidentifikator(avsenderMottaker.getId());
				yield person;
			}
			case ORGNR -> {
				Organisasjon organisasjon = new Organisasjon();
				organisasjon.setNavn(avsenderMottaker.getNavn());
				organisasjon.setOrgnummer(avsenderMottaker.getId());
				yield organisasjon;
			}
			case HPRNR -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setNavn(avsenderMottaker.getNavn());
				samhandler.setSamhandleridentifikator(utledAvsenderMottakerId(avsenderMottaker.getId()));
				samhandler.setSamhandlerkategori(SamhandlerKategoriCode.HPR.name());
				yield samhandler;
			}
			case UTL_ORG -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setNavn(avsenderMottaker.getNavn());
				samhandler.setSamhandleridentifikator(utledAvsenderMottakerId(avsenderMottaker.getId()));
				samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UTL_ORG.name());
				yield samhandler;
			}
			case UKJENT -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setNavn(avsenderMottaker.getNavn());
				samhandler.setSamhandleridentifikator(utledAvsenderMottakerId(avsenderMottaker.getId()));
				samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
				yield samhandler;
			}
			case null -> {
				Samhandler samhandler = new Samhandler();
				samhandler.setNavn(avsenderMottaker.getNavn());
				samhandler.setSamhandleridentifikator(utledAvsenderMottakerId(avsenderMottaker.getId()));
				samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
				yield samhandler;
			}
		};

	}

	private static String utledAvsenderMottakerId(String mottakerId) {
		return isEmpty(mottakerId) ? UKJENT.name() : mottakerId;
	}
}
