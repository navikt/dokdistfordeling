package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 * <p>
 * Foreløpig støtter qdist008 kun PRINT-distribusjon.
 * Kall mot dokdistkanal vil komme på sikt.
 */
@Component
public class BestemDistribusjonskanalImpl implements BestemDistribusjonskanal {

	public DistribusjonsKanalCode bestemKanal() {
		return DistribusjonsKanalCode.PRINT;
	}
}
