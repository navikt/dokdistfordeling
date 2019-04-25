package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokument;
import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokumentResponseTo;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.dokdistfordeling.storage.S3Storage;
import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.UUID;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist012Service {

	private final HentDokument hentDokument;
	private final S3Storage s3Storage;
	private final DistribuerForsendelseMapper distribuerForsendelseMapper;


	@Inject
	public Qdist012Service(HentDokument hentDokument,
						   S3Storage s3Storage,
						   DistribuerForsendelseMapper distribuerForsendelseMapper) {
		this.hentDokument = hentDokument;
		this.s3Storage = s3Storage;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
	}

	@Handler
	public DistribuerForsendelse copyDocumentsFromJoarkToDokdistmellomlagerS3Storage(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		final HentDokumenterFraJoarkTo.DistribusjonbestillingTo distribusjonbestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		final String arkivId = distribusjonbestilling.getArkivInformasjon().getArkivId();

		distribusjonbestilling.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					HentDokumentResponseTo hentDokumentResponseTo = hentDokument.hentDokument(arkivId, dokumentInformasjonTo.getArkivDokumentInfoId(),
							dokumentInformasjonTo.getVariantFormat());
					final String dokumentObjektReferanse = UUID.randomUUID().toString();
					dokumentInformasjonTo.setDokumentObjektReferanse(dokumentObjektReferanse);
					s3Storage.put(dokumentObjektReferanse, buildAndSerializeDokdistDokument(hentDokumentResponseTo.getDokument()));
				});

		return distribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);
	}

	private String buildAndSerializeDokdistDokument(byte[] document) {
		return JsonSerializer.serialize(DokdistDokument.builder().pdf(document).build());
	}
}
