package no.nav.dokdistfordeling.consumer.saf.journalpost;

import java.io.Serializable;

public class DataJournalpost implements Serializable { // todo trenger vi virkelig denne?
	private Journalpost journalpost;

	public Journalpost getJournalpost() {
		return journalpost;
	}

	public void setJournalpost(Journalpost journalpost) {
		this.journalpost = journalpost;
	}
}