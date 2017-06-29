package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import com.cpjd.roblu.forms.elements.EBoolean;
import com.cpjd.roblu.forms.elements.ECheckbox;
import com.cpjd.roblu.forms.elements.EChooser;
import com.cpjd.roblu.forms.elements.ECounter;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.forms.elements.ESlider;
import com.cpjd.roblu.forms.elements.EStopwatch;
import com.cpjd.roblu.forms.elements.ETextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.teams.TeamsView;
import com.cpjd.roblu.utils.Text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import lombok.Data;

/**
 * Welcome to the belly of the beast! (Not really)
 * This class models what data a team should store.
 *
 * Don't forget to add any new variables to the duplicate() method!
 *
 * @since 3.0.0
 * @author Will Davies
 */
@Data
public class RTeam implements Serializable, Comparable<RTeam> {

    /**
     * Identifies duplicate teams, used for file storage as well
     */
    private long ID;

    /**
     * General attributes of this team.
     * Note, lastEdit has a value 0 if not edited and is used for resolving merge conflicts
     */
    private String name;
    private int number;
    private long lastEdit;

    /**
     * Stores the scouting data. See RTab for more info.
     * tabs.get(0) is always the PIT tab
     * tabs.get(1) is always the Predictions tab
     */
    private ArrayList<RTab> tabs;
    /**
     * tabs.get(page) is the page that the user looked at last
     */
    private int page;

    /**
     * In order to make the user base happier by downloading less data,
     * TBA data is only downloaded once
     */
    private String fullName, location, motto, website;
    private int rookieYear;

    /**
     * Stores the historical edits made to this checkout.
     * The most recent edit (edits.get(edits.size()-1))) is the
     * edit used to determine what to do in merge requests (based of completion time)
     */
    private ArrayList<RCheckoutEdit> edits;


    /**
     * Uses for searching and contextual information that
     * is displayed on team cards. Can be safely deleted
     * when the app is closed.
     */
    private transient double relevance;
    private transient String searchTip;
    private transient String searchTip2;
    private transient String searchTip3;
    private transient double average;
    private transient int min;
    private transient int max;
    private transient double maxDouble;
    private transient double minDouble;

    /**
     * Creates a new RTeam with default values
     * @param name
     * @param number
     * @param ID
     */
    public RTeam(String name, int number, long ID) {
        this.name = name;
        this.number = number;
        this.ID = ID;
        this.page = 1;

        relevance = 0;
        lastEdit = 0;
    }

    /**
     * After we download TBA information, save it to this class so we don't have to download it again
     * @param name
     * @param location
     * @param motto
     * @param website
     * @param rookieYear
     */
    public void setTBAInfo(String name, String location, String motto, String website, int rookieYear) {
        this.fullName = name;
        this.location = location;
        this.motto = motto;
        this.website = website;
        this.rookieYear = rookieYear;
    }

    /**
     * Checks if their is locally stored TBA data, if there is, we don't need to redownload any more data
     * @return true if we've got locally stored TBA data
     */
    public boolean hasTBAInfo() {
        return name != null || location != null || motto != null || website == null || rookieYear > 0;
    }

    public void addAverage(double value) {
        average += value;
    }
    public void addToSearchTip3(String string) {
        searchTip3+=string;
    }

    /**
     * Creates a new instance matching this class, useful utility function
     * for various things
     * @return a new instance, but with all the same values as the current class
     */
    public RTeam duplicate() {
        RTeam team = new RTeam(name, number, ID);
        team.setLastEdit(lastEdit);
        team.setPage(page);
        team.setTabs(tabs);
        team.setRelevance(relevance);
        team.setTBAInfo(fullName, location, motto, website, rookieYear);
        team.setSearchTip(searchTip);
        return team;
    }

    /**
     * Loads the correct element from the specified tab and with the specified ID
     * @param tab the tab to get the element from
     * @param ID the ID of the element
     * @return null if no element is found, otherwise, the element matching the input criteria
     */
    public Element getElement(int tab, int ID) {
        if(tabs == null || tabs.get(tab).getElements() == null) return null;
        for(int i = 0; i < tabs.get(tab).getElements().size(); i++) if(tabs.get(tab).getElements().get(i).getID() == ID) return tabs.get(tab).getElements().get(i);
        return null;
    }

    /**
     * verify() makes sure that the form and team are syncrhonized. Here's what it does:
     * <p>
     * PIT:
     * -If the user modified the form and ADDED elements, then we'll make sure to add them to this team
     * -If the user modified the form and REMOVED elements, then we'll make sure to remove them from this team
     * -If the user changed any item titles, change them right away
     * -If the user changed any default values, reset all the values on all elements that have NOT been modified
     * -If the user changed the order of any elements, change the order
     * <p>
     * MATCH:
     * -If the user modified the match form and ADDED elements, then we'll add those to EVERY match profile
     * -If the user modified the match form and REMOVED elements, then we'll remove those from EVERY match profile
     * -If the user changed any item titles, change them on ALL match profiles
     * -If the user changed any default values, reset all the values of EVERY match that have NOT been modified
     * -If the user changed the order of any elements, change the order
     * <p>
     * PREMISE:
     * -PIT and MATCH form arrays may NOT be null, only empty
     * <p>
     * NULLS to check for:
     * -If the team has never been opened before, set the PIT values, matches don't need to be set until creation.
     */
    public void verify(RForm form) {
        // Check for null team (pit)
        if (this.tabs == null) {
            this.tabs = new ArrayList<>();
            addTab(Text.createNew(form.getPit()), "PIT", false, false);
            addTab(Text.createNew(form.getMatch()), "PREDICTIONS", false, false);
            return;
        }

        // Remove elements
        ArrayList<Element> formb = form.getPit(); // changes depending on index
        for (int i = 0; i < tabs.size(); i++) {
            if(i == 1) formb = form.getMatch();
            for (int j = 0; j < tabs.get(i).getElements().size(); j++) {
                boolean found = false;
                if(formb.size() == 0) {
                    tabs.get(i).getElements().clear();
                    break;
                }
                for (int k = 0; k < formb.size(); k++) {
                    if (tabs.get(i).getElements().get(j).getID() == formb.get(k).getID()) found = true;
                    if (k == formb.size() - 1 && !found) {
                        tabs.get(i).getElements().remove(j);
                        j = 0;
                        break;
                    }
                }
            }
        }
        
        // Add elements
        formb = form.getPit(); // changes depending on index
        for (int i = 0; i < tabs.size(); i++) {
            if (i == 1) formb = form.getMatch();
            for (int j = 0; j < formb.size(); j++) {
                boolean found = false;
                if(tabs.get(i).getElements().size() == 0) {
                    tabs.get(i).getElements().add(Text.createNew(formb.get(j)));
                    continue;
                }
                for (int k = 0; k < tabs.get(i).getElements().size(); k++) {
                    if (tabs.get(i).getElements().get(k).getID() == formb.get(j).getID()) found = true;
                    if (k == tabs.get(i).getElements().size() - 1 && !found) {
                        tabs.get(i).getElements().add(Text.createNew(formb.get(j)));
                        j = 0;
                        break;
                    }
                }
            }
        }

        // Update item names
        formb = form.getPit();
        for (int i = 0; i < tabs.size(); i++) {
            if (i == 1) formb = form.getMatch();
            for (int j = 0; j < formb.size(); j++) {
                for (int k = 0; k < tabs.get(i).getElements().size(); k++) {
                    if (formb.get(j).getID() == tabs.get(i).getElements().get(k).getID()) {
                        tabs.get(i).getElements().get(k).setTitle(formb.get(j).getTitle());
                        break;
                    }
                }
            }
        }

        // Update default values for non-modified values, also check for some weird scenarioes
        formb = form.getPit();
        for (int i = 0; i < tabs.size(); i++) {
            if (i == 1) formb = form.getMatch();
            for (int j = 0; j < formb.size(); j++) {
                for (int k = 0; k < tabs.get(i).getElements().size(); k++) {
                    if (formb.get(j).getID() == tabs.get(i).getElements().get(k).getID()) {
                        Element e = formb.get(j);
                        Element s = tabs.get(i).getElements().get(k);

                        if (e instanceof EBoolean && !s.isModified() && s instanceof EBoolean)
                            ((EBoolean) s).setValue(((EBoolean) e).getValue());
                        else if (e instanceof ECheckbox && s instanceof ECheckbox) {
                            if (!s.isModified()) {
                                ((ECheckbox) s).setValues(((ECheckbox) e).getValues());
                            }
                            if (!((ECheckbox) s).getValues().equals(((ECheckbox) e).getValues())) {
                                ((ECheckbox) s).setChecked(((ECheckbox) e).getChecked());
                                ((ECheckbox) s).setValues(((ECheckbox) e).getValues());
                            }
                        }
                        //else if (e instanceof ETextfield && !s.isModified()) ((ETextfield) s).setText(((ETextfield) e).getText());
                        else if (e instanceof EChooser && s instanceof EChooser) {
                            if (!s.isModified())
                                ((EChooser) s).setSelected(((EChooser) e).getSelected());
                            if (!((EChooser) s).getValues().equals(((EChooser) e).getValues())) {
                                ((EChooser) s).setValues(((EChooser) e).getValues());
                                ((EChooser) s).setSelected(((EChooser) e).getSelected());
                            }
                        } else if (e instanceof EStopwatch && !s.isModified() && s instanceof EStopwatch)
                            ((EStopwatch) s).setTime(((EStopwatch) e).getTime());
                        else if (e instanceof ESlider && !s.isModified() && s instanceof ESlider) {
                            ((ESlider) s).setMax(((ESlider) e).getMax());
                            ((ESlider) s).setCurrent(((ESlider) e).getCurrent());
                        } else if (e instanceof ECounter && s instanceof ECounter) {
                            ((ECounter) s).setMin(((ECounter) e).getMin());
                            ((ECounter) s).setMax(((ECounter) e).getMax());
                            ((ECounter) s).setIncrement(((ECounter) e).getIncrement());
                            if (!s.isModified())
                                ((ECounter) s).setCurrent(((ECounter) e).getCurrent());
                            if (((ECounter) e).getCurrent() < ((ECounter) e).getMin() || ((ECounter) e).getCurrent() > ((ECounter) e).getMax()) {
                                ((ECounter) s).setCurrent(((ECounter) s).getMin());
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets all the image IDs for every single EGallery element and compiles them
     * into one big list
     * @return the compiled list of image IDs for all images in this team
     */
    ArrayList<Long> getAllImagesID() {
        ArrayList<Long> ids = new ArrayList<>();

        for(RTab tab : tabs) {
            for(Element e : tab.getElements()) {
                if(e instanceof EGallery) ids.addAll(((EGallery) e).getImageIDs());
            }
        }
        return ids;
    }


    /**
     * Adds the tab to the team.
     * @param elements the form elements for this tab
     * @param title the title of the tab
     * @param isRedAlliance whether the team is on the blue or red alliance for this "tab" (match)
     * @param won whether the user has won this match
     * @return the position of the new tab within the array
     */
    public int addTab(ArrayList<Element> elements, String title, boolean isRedAlliance, boolean won) {
        tabs.add(new RTab(elements, title, isRedAlliance, won));
        Collections.sort(tabs);
        for(int i = 0; i < tabs.size(); i++) if(tabs.get(i).getTitle().equals(title)) return i;
        return 1;
    }

    /**
     * Deletes the tab from the RTabs array
     * @param position the index or position of the tab to delete
     */
    public void removeTab(int position) {
        tabs.remove(position);
    }


    /**
     * Shortcut method to get the elements from the specified tab
     * @param index the tab index
     * @return the form elements from that index
     */
    public ArrayList<Element> getItems(int index) {
        return tabs.get(index).getElements();
    }


    /**
     * Resets the relevance when and associated contextual information
     */
    public void resetRelevance() {
        searchTip = "";
        searchTip2 = "";
        searchTip3 = "";
        this.relevance = 0;
        average = 0;
        min = 0;
        max = 0;
        maxDouble = 0;
        minDouble = 0;
    }

    /**
     * Adds the specified amount of relevance to the team
     * @param relevance the amount of relevance to add
     */
    public void addRelevance(double relevance) {
        this.relevance += relevance;
    }

    /**
     * Searching utility to help provide the user with more in-depth results
     * when they are searching teams
     * @param query the search query
     * @return the relevance to add to the team because of matches with the following name
     */
    public int searchMatches(String query) {
        if(tabs == null || tabs.size() == 0) return 0;
        query = query.toLowerCase();
        searchTip = "Contains matches: ";
        for(int i = 2; i < tabs.size(); i++) {
            if(tabs.get(i).getTitle().equalsIgnoreCase(query)) searchTip += tabs.get(i).getTitle()+", ";
            else if(tabs.get(i).getTitle().toLowerCase().contains(query)) searchTip += tabs.get(i).getTitle()+", ";
        }
        if(!searchTip.equals("Contains matches: ")) {
            searchTip = searchTip.substring(0, searchTip.length() - 2);
            return 200;
        } else {
            searchTip = "";
        }
        return 0;
    }

    /**
     * Returns the number of matches this team is in
     * @return
     */
    public int getNumMatches() {
        if(tabs == null) return 0;
        else return tabs.size() - 2;
    }

    /**
     * Update methods. These manage updating of form elements with RTabs
     */


    /**
     * Updates the time this team was last edited
     */
    public void updateEdit() {
        lastEdit = System.currentTimeMillis();
    }


    public void updateBoolean(int index, int ID, boolean b) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((EBoolean) tabs.get(index).getElements().get(j)).setValue(b);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateCounter(int index, int ID, int value) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((ECounter) tabs.get(index).getElements().get(j)).setCurrent(value);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateSlider(int index, int ID, int value) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((ESlider) tabs.get(index).getElements().get(j)).setCurrent(value);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateChooser(int index, int ID, int selected) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID && ((EChooser) tabs.get(index).getElements().get(j)).getSelected() != selected) {
                ((EChooser) tabs.get(index).getElements().get(j)).setSelected(selected);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateCheckbox(int index, int ID, ArrayList<Boolean> checked) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((ECheckbox) tabs.get(index).getElements().get(j)).setChecked(checked);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateStopwatch(int index, int ID, double time) {
        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((EStopwatch) tabs.get(index).getElements().get(j)).setTime(Text.round(time, 1));
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    public void updateTextfield(int index, int ID, String value) {
        if (ID == 0 && index == 0) {
            name = value;
            updateEdit();
            return;
        } else if (ID == 1 && index == 0) {
            number = Integer.parseInt(value);
            updateEdit();
            return;
        }

        for (int j = 0; j < tabs.get(index).getElements().size(); j++) {
            if (tabs.get(index).getElements().get(j).getID() == ID) {
                ((ETextfield) tabs.get(index).getElements().get(j)).setText(value);
                tabs.get(index).getElements().get(j).setModified(true);
                updateEdit();
                break;
            }
        }
    }

    /**
     * Used for sorting the team from TeamsView
     * @param team the team to compare against this copy
     * @return the corrected position
     */
    @Override
    public int compareTo(@NonNull RTeam team) {
        switch (TeamsView.FILTER) {
            case TeamsView.ALPHABETICAL:
                return this.getName().compareTo(team.getName());
            case TeamsView.NUMERICAL:
                return ((Integer) getNumber()).compareTo(team.getNumber());
            case TeamsView.SEARCH:
                return ((Integer) (int)Math.round(getRelevance())).compareTo((int)Math.round(team.getRelevance()));
            case TeamsView.CUSTOM:
                return ((Integer) (int)Math.round(getRelevance())).compareTo((int)Math.round(team.getRelevance()));
            case TeamsView.LAST_EDIT:
                return ((Long)getLastEdit()).compareTo(team.getLastEdit());
            default:
                return 0;
        }
    }

    @Data
    private class RCheckoutEdit {
        /**
         * The Google email address of the user who completed the checkout.
         * This will be filled out when the RCheckout model is uploaded by
         * the scouter.
         *
         */
        private String completedBy;

        /**
         * The time in ms, that the scouter completed the assignment.
         * Used for resolving merge conflicts.
         */
        private long completedTime;
    }
}
