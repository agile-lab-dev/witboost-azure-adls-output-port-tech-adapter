package it.agilelab.witboost.provisioning.adlsop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public abstract class PublicInfoObject {

    @JsonProperty
    protected String type;

    @JsonProperty
    protected String label;

    @JsonProperty
    protected String value;

    public static PublicInfoObject createTextInfo(String label, String value) {
        return new TextInfo(label, value);
    }

    public static PublicInfoObject createLinkInfo(String label, String value, String href) {
        return new LinkInfo(label, value, href);
    }
}

class TextInfo extends PublicInfoObject {
    public TextInfo(String label, String value) {
        this.type = "string";
        this.label = label;
        this.value = value;
    }
}

class LinkInfo extends PublicInfoObject {
    @JsonProperty
    protected String href;

    public LinkInfo(String label, String value, String href) {
        this.type = "string";
        this.label = label;
        this.value = value;
        this.href = href;
    }
}
