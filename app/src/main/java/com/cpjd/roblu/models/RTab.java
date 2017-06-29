package com.cpjd.roblu.models;

import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.utils.Text;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
 * This class stores information about either PIT, Prediction, or Match data.
 *
 * @since 3.5.0
 * @author Will Davies
 */
@Data
public class RTab implements Serializable, Comparable<RTab> {

    /**
     * RTabs are identified by their title, no duplicate titles!
     */
    private String title;
    private ArrayList<Element> elements;
    private final boolean redAlliance;
    private boolean won;

    public RTab(ArrayList<Element> elements, String title, boolean redAlliance, boolean won) {
        this.elements = elements;
        this.title = title;
        this.redAlliance = redAlliance;
        this.won = won;
    }

    @Override
    public int compareTo(RTab tab) {
        return ((Long)Text.getMatchScore(title)).compareTo(Text.getMatchScore(tab.getTitle()));
    }
}
