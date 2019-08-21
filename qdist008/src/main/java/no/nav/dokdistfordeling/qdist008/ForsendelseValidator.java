package no.nav.dokdistfordeling.qdist008;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.util.Qdist008Util.countHoveddokument;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.functional.BestillingsIdInvalidUuidFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.dokdistfordeling.storage.Storage;
import org.apache.camel.Handler;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Slf4j
@Component
public class ForsendelseValidator {

	private final Storage storage;

	public ForsendelseValidator(Storage storage) {
		this.storage = storage;
	}

	@Handler
	public void validate(DistribuerForsendelseTo distribuerForsendelseTo) {
		final DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo = distribuerForsendelseTo.getDistribusjonbestilling();
		assertThatForsendelseContainsExactlyOneHoveddokument(distribusjonbestillingTo);
		assertThatAdresseIsPresentIfMottakerIsSamhandler(distribusjonbestillingTo);
		assertThatBestillingsIdIsAValidUuid(distribusjonbestillingTo.getBestillingsId());

		//TODO This is only for testing and must be removed
		persistTestDocumentsToS3(distribusjonbestillingTo);

		assertThatDocumentsAreAvailableInS3(distribusjonbestillingTo);
	}

	private void assertThatForsendelseContainsExactlyOneHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		int numberOfHoveddokumenter = countHoveddokument(distribusjonbestillingTo);

		if (numberOfHoveddokumenter != 1) {
			throw new ValidationException(format("Forsendelsen må inneholde nøyaktig ett hoveddokument. Fant %s hoveddokument(er) på forsendelsen", numberOfHoveddokumenter));
		}
	}

	private void assertThatAdresseIsPresentIfMottakerIsSamhandler(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		if (distribusjonbestillingTo.getMottaker().isSamhandler() && distribusjonbestillingTo.getAdresse() == null) {
			throw new ValidationException("Mottaker er av typen samhandler. Da er adresse et påkrevd felt i input til qdist008. Fant ingen adresse på bestilling");
		}
	}

	private void assertThatBestillingsIdIsAValidUuid(String bestillingsId) {
		try {
			UUID.fromString(bestillingsId);
		} catch (IllegalArgumentException exception) {
			throw new BestillingsIdInvalidUuidFunctionalException(format("bestillingsId er ikke en gyldig UUID (universally unique identifier). Fikk bestilling=%s", bestillingsId));
		}
	}

	private void assertThatDocumentsAreAvailableInS3(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		distribusjonbestillingTo.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					storage.get(dokumentInformasjonTo.getDokumentObjektReferanse());
				});
	}


	//TODO Remove the methods below after testing
	private void persistTestDocumentsToS3(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		distribusjonbestillingTo.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					storage.put(dokumentInformasjonTo.getDokumentObjektReferanse(), JsonSerializer.serialize(DokdistDokument.builder()
							.pdf(readTestFileToBytes()).build()));
				});
	}

	private byte[] readTestFileToBytes() {
		try {
//			URI uri = getClass().getClassLoader().getResource("Brev000050.pdf").toURI();
//			log.info(String.format("uri string=%s", uri.toString()));
//			Path path = Paths.get(uri);
//			log.info(String.format("path string=%s", path.toString()));
//			log.info(String.format("absolute path string=%s", path.toAbsolutePath()));
//			log.info(String.format("file system string=%s", path.getFileSystem().toString()));
//			return Files.readAllBytes(path);

			InputStream in = getClass().getClassLoader().getResourceAsStream("/Brev000050.pdf");
			if(in == null){
				throw new RuntimeException("Problem med å lese inn testdokument. Inputstream=null");

			}
			return IOUtils.toByteArray(in);

		} catch (IOException e) {
			throw new RuntimeException(format("Problem med å lese inn testdokument. Feilmelding=%s", e.getMessage(), e));
		}
	}

}
