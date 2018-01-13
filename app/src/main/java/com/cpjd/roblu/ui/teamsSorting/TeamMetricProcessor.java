package com.cpjd.roblu.ui.teamsSorting;

import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;
import java.util.Map;

import lombok.Setter;

/**
 * TeamMetricProcessor generates a string representing a certain metric within the team's matches.
 * TeamMetricProcessor may also generate some statistics and overview metrics.
 *
 * Please familiarize yourself with
 * @see PROCESS_METHOD
 * before using this class.
 *
 * @version 2
 * @author Will Davies
 * @since 3.5.9
 */
public class TeamMetricProcessor {

    /**
     * Process method defines what data set should be processed
     * by this class
     */
    public static class PROCESS_METHOD {
        /**
         * Metric (matching inputted ID) should be analyzed from the PIT tab within each team
         */
        public static final int PIT = 0;
        /**
         * Metric (matching inputted ID) should be analyzed from the PREDICTIONS tab within each team
         */
        public static final int PREDICTIONS = 1;
        /**
         * Metric (matching inputted ID) should be analyzed from the MATCHES within each team
         */
        public static final int MATCHES = 2;
        /**
         * No metric is being analyzed (and thus ID will be ignored), teams should just be sorted by how many match wins they have
         */
        public static final int MATCH_WINS = 3;
        /**
         * No metric is being analyzed (and thus ID will be ignored), teams should just be sorted by if they are in the selected match.
         * Note: This sort method actually requires the teams to be removed from the list if its not in the requested match, this
         * must be handled somewhere not in this class!
         */
        public static final int IN_MATCH = 4;
        /**
         * No metric is being analyzed (and thus ID will be ignored), teams should just be sorted as they normally are
         */
        public static final int RESET = 6;
    }

    /**
     * This is a helper variable for PROCESS_METHOD.IN_MATCH, it must be set externally.
     * It is a match name like "Quals 1"
     */
    @Setter
    private String inMatchTitle;

    /**
     * Processes an RTeam object and sets its filterTag and relevance variables.
     * -filterTag is a string representing a certain metric as defined by param method and param ID
     * -relevance is used for sorting (relevance is assigned differently for the type of RMetric, but for example,
     * counters with the highest value with cause that team to be sorted at the top)
     *
     * @param team the team to process
     * @param method an int from PROCESS_METHOD that defines how an team should be processed
     * @param ID the id of the metric to process, this value will be ignored if method is PIT, PREDICTIONS, or MATCHES
     */
    public void process(RTeam team, int method, int ID) {
        team.setCustomRelevance(0);

        /*
         * Helper variables
         */
        StringBuilder rawData = new StringBuilder();
        int occurrences = 0;
        double relevance = 0;
        double average = 0.0, min = 0.0, max = 0.0;

        /*
         * If the request method is PIT or PREDICTIONS, use the following code to
         * sort the team. Note, each team will only have one value that needs to be
         * analyzed, so statistics are essentially invalid
         */
        if(method == PROCESS_METHOD.PIT || method == PROCESS_METHOD.PREDICTIONS) {
            for(RMetric metric : team.getTabs().get(method).getMetrics()) {
                if(metric.getID() != ID) continue;

                if(metric instanceof RBoolean) {
                    rawData.append("Boolean: ").append(metric.getTitle()).append(" is ").append(friendlyBoolean((RBoolean)metric));
                    if(metric.isModified() && ((RBoolean)metric).isValue()) relevance++; 
                } else if(metric instanceof RCounter) {
                    rawData.append("Counter: ").append(metric.getTitle()).append(" is ").append(friendlyCounter((RCounter)metric));
                    relevance += ((RCounter) metric).getValue();
                } else if(metric instanceof RSlider) {
                    rawData.append("Slider: ").append(metric.getTitle()).append(" is ").append(friendlySlider((RSlider)metric));
                    relevance += ((RSlider) metric).getValue();
                } else if(metric instanceof RStopwatch) {
                    rawData.append("Stopwatch: ").append(metric.getTitle()).append(" is ").append(friendlyStopwatch((RStopwatch) metric));
                    relevance += ((RStopwatch) metric).getTime();
                } else if(metric instanceof RTextfield) {
                    rawData.append("Textfield: ").append(metric.getTitle()).append(" has  ").append(((RTextfield) metric).getText().length()).append(" characters");
                    relevance += ((RTextfield) metric).getText().length();
                } else if(metric instanceof RGallery) {
                    rawData.append("Gallery: ").append(metric.getTitle()).append(" contains  ").append(((RGallery) metric).getImages().size()).append(" images");
                    relevance += ((RGallery) metric).getImages().size();
                } else if(metric instanceof RCheckbox) {
                    rawData.append("Checkbox: ").append(metric.getTitle()).append(" values:  ").append(friendlyCheckbox((RCheckbox) metric));
                    relevance += getCheckedAmount((RCheckbox) metric);
                } else if(metric instanceof RChooser) {
                    rawData.append("Chooser: ").append(metric.getTitle()).append(" has value  ").append(((RChooser)metric).getValues()[((RChooser)metric).getSelectedIndex()]);
                }

                rawData.append(" in ").append(friendlyMode(method));
                team.setFilterTag(rawData.toString());
                team.setCustomRelevance(relevance);
                break;
            }
        }
        /*
         * If the request method is MATCHES, then the following code has to be used
         * to look at each particular RTab object within the team object
         */
        else if(method == PROCESS_METHOD.MATCHES) {
            if(team.getTabs() == null || team.getTabs().size() == 0) {
                team.setFilterTag("This team does not contain any matches that can be sorted.");
                return;
            }

            /*
             * This nested for loop will go through every team tab and every metric within each team tab.
             * This loop should only process the RAW DATA for each metric, the overview stuff will be added at the end.
             * Use StringBuilder rawData to store raw data
             */
            rawData.append("\nRaw data: ");
            for(int i = 2; i < team.getTabs().size(); i++) {
                for(RMetric metric : team.getTabs().get(i).getMetrics()) {
                    // Make sure that the metric that is being processed is equal to the inputted value
                    if(metric.getID() != ID) continue;

                    // RBoolean type
                    if(metric instanceof RBoolean) {
                        RBoolean rBoolean = (RBoolean)metric;
                        // if the value is modified and true, add some relevance info
                        if(rBoolean.isModified() && rBoolean.isValue()) {
                            occurrences++;
                            relevance++;
                        }
                        // add raw data
                        rawData.append(friendlyBoolean((RBoolean)metric)).append(ending(i, team.getTabs()));
                    }
                    // RCounter type
                    else if(metric instanceof RCounter) {
                        double value = ((RCounter) metric).getValue();
                        // Overview stats will only consider modified items
                        if(metric.isModified()) {
                            /*
                             * Progressively calculate the min, max, and average values
                             */
                            if(value < min) min = value;
                            if(value > max) max = value;
                            average += value / (double) numModified(team.getTabs(), ID);
                            relevance = average;
                        }
                        // add raw data
                        rawData.append(friendlyCounter((RCounter)metric)).append(ending(i, team.getTabs()));
                    }
                    // RSlider type
                    else if(metric instanceof RSlider) {
                        int value = ((RSlider) metric).getValue();
                        // Overview stats will only consider modified sliders
                        if(metric.isModified()) {
                            if(value < min) min = value;
                            if(value > max) max = value;
                            average += (double) value / (double) numModified(team.getTabs(), ID);
                            relevance = average;
                        }
                        // add raw data
                        rawData.append(friendlySlider((RSlider)metric)).append(ending(i, team.getTabs()));
                    }
                    // RStopwatch type
                    else if(metric instanceof RStopwatch) {
                        double value = ((RStopwatch) metric).getTime();
                        // Overview stats will only consider modified stopwatches
                        if(metric.isModified()) {
                            /*
                             * Progressively calculate the min, max, and average values
                             */
                            if(value < min) min = value;
                            if(value > max) max = value;
                            average += value / (double) numModified(team.getTabs(), ID);
                            relevance = average;
                        }
                        // add raw data
                        rawData.append(friendlyStopwatch((RStopwatch)metric)).append(ending(i, team.getTabs()));
                    }
                    // RTextfield type
                    else if(metric instanceof RTextfield) {
                        int value = ((RTextfield) metric).getText().length();
                        // Overview stats will only consider modified textfields
                        if(metric.isModified()) { // Only add this value to stats if it's modified
                            /*
                             * Progressively calculate the min, max, and average values
                             */
                            if(value < min) min = value;
                            if(value > max) max = value;
                            average += (double) value / (double) numModified(team.getTabs(), ID);
                            relevance = average;
                        }
                        // add raw data
                        rawData.append(value).append(" chars").append(ending(i, team.getTabs()));
                    }
                    // RGallery type
                    else if(metric instanceof RGallery) {
                        int value = ((RGallery) metric).getImages().size();
                        // Overview stats will only consider modified textfields
                        if(metric.isModified()) {
                            /*
                             * Progressively calculate the min, max, and average values
                             */
                            if(value < min) min = value;
                            if(value > max) max = value;
                            average += (double) value / (double) numModified(team.getTabs(), ID);
                            relevance = average;
                        }
                        // add raw data
                        rawData.append(value).append(" images ").append(ending(i, team.getTabs()));
                    }
                    // RCheckbox type
                    else if(metric instanceof RCheckbox) {
                        // add raw data
                        rawData.append(friendlyCheckbox((RCheckbox)metric)).append(ending(i, team.getTabs()));
                        relevance += getCheckedAmount((RCheckbox) metric);
                    }
                    // RChooser type
                    else if(metric instanceof RChooser) {
                        if(metric.isModified()) relevance++;
                        // add raw data
                        rawData.append(((RChooser)metric).getValues()[((RChooser)metric).getSelectedIndex()]).append(ending(i, team.getTabs()));
                    }

                    /*
                     * Now, add the overview statistics to the team if the metric has overview statistics
                     * available
                    */
                    StringBuilder overview = new StringBuilder();
                    if(metric instanceof RBoolean) overview.append("Boolean: ").append(metric.getTitle()).append(" is true in ").append(occurrences).append(" / ").append(team.getTabs().size() - 2).append("matches");
                    else if(metric instanceof RCounter) overview.append("Counter: ").append(metric.getTitle()).append(" Average: ").append(Utils.round(average, 2)).append(" Min: ").append(min).append(" Max: ").append(max);
                    else if(metric instanceof RSlider) overview.append("Slider: ").append(metric.getTitle()).append(" Average: ").append(Utils.round(average, 2)).append(" Min: ").append(min).append(" Max: ").append(max);
                    else if(metric instanceof RStopwatch) overview.append("Stopwatch: ").append(metric.getTitle()).append(" Average: ").append(Utils.round(average, 2)).append(" Min: ").append(min).append(" Max: ").append(max);
                    else if(metric instanceof RTextfield) overview.append("Textfield: ").append(metric.getTitle()).append(" Average chars: ").append(Utils.round(average, 2)).append(" Min: ").append(min).append(" Max: ").append(max);
                    else if(metric instanceof RGallery) overview.append("Gallery: ").append(metric.getTitle()).append(" Average images: ").append(Utils.round(average, 2)).append(" Min: ").append(min).append(" Max: ").append(max);
                    else if(metric instanceof RChooser) overview.append("RChooser: ").append(metric.getTitle());
                    else if(metric instanceof RCheckbox) overview.append("RCheckbox: ").append(metric.getTitle());

                    /*
                     * Now append the raw data as processed above
                     */
                    team.setFilterTag(overview.append(rawData).toString());
                    team.setCustomRelevance(0);

                    // exit the loop, the metric has been fully processed
                    break;
                }
            }
        }
        /*
         * The user requested MATCH_WINS
         */
        else if(method == PROCESS_METHOD.MATCH_WINS) {
            for(int i = 2; i < team.getTabs().size(); i++) {
                if(team.getTabs().get(i).isWon()) {
                    occurrences++;
                    relevance++;
                    rawData.append("W");
                } else rawData.append("L");
                rawData.append(ending(i, team.getTabs()));
            }
            /*
             * Setup overview rawData and add raw data
             */

            team.setCustomRelevance(relevance);
            team.setFilterTag(String.valueOf(occurrences) + " match wins" + rawData);
        }
        /*
         * The user requested IN_MATCH
         */
        else if(method == PROCESS_METHOD.IN_MATCH) {
            for(int i = 2; i < team.getTabs().size(); i++) {
                if(team.getTabs().get(i).getTitle().equalsIgnoreCase(inMatchTitle)) {
                    team.setFilterTag(rawData.append("In ").append(inMatchTitle).toString());
                }
            }
        }
        /*
         * The user request a reset
         */
        else if(method == PROCESS_METHOD.RESET) {
            team.setFilterTag("");
            team.setCustomRelevance(0);
        }

    }


    /*
     * All the methods that begin with 'friendly' just take
     * an object reference and translates the value it stores into
     * a short string that can be placed on a UI card
     */

    /**
     * Represents a boolean metric in a clean format
     * @param metric the metric to analyze
     * @return a string representing the RBoolean's data
     */
    private String friendlyBoolean(RBoolean metric) {
        if(!metric.isModified()) return "N.O.";
        else if(metric.isValue()) return "T";
        else return "F";
    }

    /**
     * Represents a counter metric in a clean format
     * @param metric the metric to analyze
     * @return a string representing the RCounter's data
     */
    private String friendlyCounter(RCounter metric) {
        if(!metric.isModified()) return "N.O.";
        else return String.valueOf(metric.getValue());
    }

    /**
     * Represents a counter slider in a clean format
     * @param metric the metric to analyze
     * @return a string representing the RSlider's data
     */
    private String friendlySlider(RSlider metric) {
        if(!metric.isModified()) return "N.O.";
        else return String.valueOf(metric.getValue());
    }

    /**
     * Represents a stopwatch metric in a clean format
     * @param metric the metric to analyze
     * @return a string representing the RStopwatch's data
     */
    private String friendlyStopwatch(RStopwatch metric) {
        if(!metric.isModified()) return "N.O.";
        else return String.valueOf(metric.getTime() + "s");
    }

    /**
     * Represents a checkbox metric in a clean format
     * @param metric the metric to analyze
     * @return a string representing the RCheckbox's data
     */
    private String friendlyCheckbox(RCheckbox metric) {
        if(!metric.isModified()) return "No values observed (N.O.)";
        StringBuilder tag = new StringBuilder("(");
        for(Object o : metric.getValues().entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            tag.append(pair.getValue()).append(",");
        }
        tag.setCharAt(tag.toString().length(), ')');
        return tag.toString();
    }

    /**
     * Instead of processing an element, this will just return a string based off the process mode
     * @param method the sort method being used
     * @return a String representing the sort method being used
     */
    private String friendlyMode(int method) {
        switch(method) {
            case PROCESS_METHOD.PIT:
                return "PIT.";
            case PROCESS_METHOD.PREDICTIONS:
                return "PREDICTIONS.";
            default:
                return "Matches.";
        }
    }

    /**
     * Gets the number of checked items within a checkbox
     * @param checkbox the RCheckbox object to analyze
     * @return the number of checked checkboxes within the item
     */
    private int getCheckedAmount(RCheckbox checkbox) {
        int num = 0;
        for(Object o : checkbox.getValues().entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            if(((Boolean)pair.getValue())) num++;
        }
        return num;
    }

    /**
     * Returns the number of metrics that have been modified across all matches for average purposes.
     * It ONLY includes modified metrics because only modified metrics are included in average data.
     * @param tabs the team tabs array containing the matches to analyze
     * @param ID the ID of the metric that is being analyzed
     * @return the number of metrics across all matches that have been modified
     */
    private int numModified(ArrayList<RTab> tabs, int ID) {
        int num = 0;
        for(int i = 2; i < tabs.size(); i++) for(RMetric metric : tabs.get(i).getMetrics()) if(metric.getID() == ID && metric.isModified()) num++;
        return num;
    }

    /**
     * Returns the correct string to be appended at the end of a raw data cycle.
     * This method prevents any complete raw data string from ending in ","
     * @param i the index the array is in
     * @param tabs the array of tabs being analyzed
     * @return "," if it's not the last raw data value, " " if it is
     */
    private String ending(int i, ArrayList<RTab> tabs) {
        if(i != tabs.size() - 1) return ", ";
        else return " ";
    }

}