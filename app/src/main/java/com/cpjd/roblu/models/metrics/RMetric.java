package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.Serializable;

import lombok.Data;
import lombok.NonNull;

/**
 * Parent class for ALL metric elements. Any children of this class need to contain serializable variables.
 * Note: To reduce code needed, existing metrics can't be updated without deleting old data. That's why you shouldn't
 * modify old metrics, but only add new ones.
 *
 * Also, RMetrics will normally contain variables that represent a value. Roblu will automatically store a duplicate copy
 * of a form to reference for default values, so in the case of the form, a metric's value will represent the default value.
 *
 * @author Will Davies
 * @version 2
 * @since 3
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RBoolean.class, name = "RBoolean"),
        @JsonSubTypes.Type(value = RCheckbox.class, name = "RCheckbox"),
        @JsonSubTypes.Type(value = RChooser.class, name = "RChooser"),
        @JsonSubTypes.Type(value = RCounter.class, name = "RCounter"),
        @JsonSubTypes.Type(value = RGallery.class, name = "RGallery"),
        @JsonSubTypes.Type(value = RSlider.class, name = "RSlider"),
        @JsonSubTypes.Type(value = RStopwatch.class, name = "RStopwatch"),
        @JsonSubTypes.Type(value = RTextfield.class, name = "RTextfield")
})
public abstract class RMetric implements Serializable {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * Specifies an object's ID, the primary identification tool
     */
    protected int ID;
    /**
     * Specifies the title or string identifier of a metric.
     */
    @NonNull
    protected String title;
    /**
     * true if this metric has been modified one or more times
     */
    private boolean modified;

    /**
     * If true, the user will not be permitted to remove this metric from an RForm model. This is a flag, and must
     * be handled by the UI code, not RMetric
     */
    protected boolean required;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RMetric() {}

    public RMetric(int ID, String title) {
        this.ID = ID;
        this.title = title;
        this.modified = false;
    }

    /**
     * Summarizes this element by describing its type
     * and it's default value. Should ONLY be used if this
     * metric is being using in the RForm model.
     * @return String representing metric defaults information
     */
    @SuppressWarnings("unused")
    public abstract String getFormDescriptor();

    /**
     * We need to be able to create a new instance of any RMetric.
     * Used mostly for form syncing and verification in RTeam.verify()
     * @return re-instantiated RMetric object
     */
    public abstract RMetric clone();

}
