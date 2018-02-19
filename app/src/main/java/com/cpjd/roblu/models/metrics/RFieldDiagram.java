package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a image field diagram that can be drawn upon
 * @see RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RFieldDiagram")
public class RFieldDiagram extends RMetric {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    private byte[] diagram;
    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RFieldDiagram() {}

    public RFieldDiagram(int ID, byte[] diagram) {
        super(ID, "Field diagram");
        this.diagram = diagram;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Field diagram";
    }

    @Override
    public RMetric clone() {
        return new RFieldDiagram(ID, this.diagram);
    }
}
