package no.nav.dokdistfordeling.qdist008.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "DistribuerForsendelseTilSentralPrint",
		propOrder = {"forsendelseId"}
)
@XmlRootElement(
		namespace = "http://nav.no/melding/virksomhet/dokdistsentralprint",
		name = "distribuerForsendelseTilSentralPrint"
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DistribuerForsendelseTilSentralPrint {
	@XmlElement(
			required = true
	)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlSchemaType(
			name = "forsendelseId"
	)
	protected String forsendelseId;
}
