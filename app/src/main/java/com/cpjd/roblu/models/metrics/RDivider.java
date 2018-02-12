package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This is a divider element used for logically grouping sets of other metrics.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RDivider")
public class RDivider extends RMetric {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * The empty constructor is required for de-serialization
     */
    public RDivider() {}

    public RDivider(int ID, String title) {
        super(ID, title);
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Divider\nDefault value: "+title;
    }

    @Override
    public RMetric clone() {
        return new RDivider(ID, title);
    }
}
