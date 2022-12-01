package no.nav.dokdistfordeling.consumer.rdist001;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersisterForsendelseResponseTo {
	String forsendelseId;
}
