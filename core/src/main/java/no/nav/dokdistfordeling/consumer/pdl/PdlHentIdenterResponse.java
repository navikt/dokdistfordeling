package no.nav.dokdistfordeling.consumer.pdl;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class PdlHentIdenterResponse {
    private PdlHentIdenterData data;
    private List<PdlErrorTo> errors;

    @Data
    public static class PdlHentIdenterData {
        private PdlIdenter hentIdenter;
    }

    @Data
    public static class PdlIdenter {
        private List<PdlIdentTo> identer;
    }

    @Data
    public static class PdlIdentTo {
        @ToString.Exclude
        private String ident;
        private boolean historisk;
        private IdentType gruppe;
    }

    @Data
    public static class PdlErrorTo {
        private String message;
        private PdlErrorExtensionTo extensions;
    }

    @Data
    public static class PdlErrorExtensionTo {
        private String code;
        private String classification;
    }
}
