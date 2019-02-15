package no.nav.dokdistfordeling.consumer.aktoerv2;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@Builder
public class HentAktoerIdForIdentResponseTo {

	private final String aktoerId;
	@Builder.Default
	private final List<String> historiskeIdenter = new ArrayList<>();

}
