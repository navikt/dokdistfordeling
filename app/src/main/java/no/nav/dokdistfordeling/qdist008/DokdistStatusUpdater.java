package no.nav.dokdistfordeling.qdist008;

import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DokdistStatusUpdater {

	@Handler
	public void doUpdate(){
		//Todo Update dokdist status
	}

}
