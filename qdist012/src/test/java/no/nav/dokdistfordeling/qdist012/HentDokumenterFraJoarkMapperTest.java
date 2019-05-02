package no.nav.dokdistfordeling.qdist012;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HentDokumenterFraJoarkMapperTest {

    private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();

    @Test
    public void shouldMap(){
//        HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(null);

//        assertEquals(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getBatchId(),null);

        assertThrows(Exception.class,
                () -> hentDokumenterFraJoarkMapper.map(null),
                "Expected distribuerForsendelseMapper.map() to throw AbstractDokdistfordelingFunctionalException, but it didn't");
    }

}