package no.nav.dokdistfordeling.consumer.saf.hentdokument;

public interface HentDokument {

	HentDokumentResponseTo hentDokument(String journalpostId, String dokumentInfoId, String variantFormat);

}
