package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokument;
import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokumentResponseTo;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.dokdistfordeling.storageaws.AwsStorage;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
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
	private final AwsStorage storage;
	private final Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper;


	@Inject
	public Qdist012Service(HentDokument hentDokument,
						   AwsStorage storage,
						   Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper) {
		this.hentDokument = hentDokument;
		this.storage = storage;
		this.qdist008DistribuerForsendelseMapper = qdist008DistribuerForsendelseMapper;
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
					storage.put(dokumentObjektReferanse, buildAndSerializeDokdistDokument(hentDokumentResponseTo.getDokument()));
				});

		return qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);
	}

	private String buildAndSerializeDokdistDokument(byte[] document) {
		return JsonSerializer.serialize(DokdistDokument.builder().pdf(document).build());
	}
}
