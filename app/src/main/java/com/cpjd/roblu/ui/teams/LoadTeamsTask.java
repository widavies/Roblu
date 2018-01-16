package com.cpjd.roblu.ui.teams;

import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.teamsSorting.TeamMetricProcessor;
import com.cpjd.roblu.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Loads and sorts RTeam models from the file system.
 * Manages:
 * -Loading
 * -Filtering (Alphabetical, numerical, last-edit, custom)
 * -Searching
 *
 * -use LoadTeamsTask.setTeams() to avoid reloading them if possible
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoadTeamsTask extends AsyncTask<Void, Void, Boolean> {

    /**
     * The int ID of the event whose teams need to be loaded
     */
    private int eventID;

    /**
     * Use this array if you don't want the LoadTeamsTask to reload the teams from the file system
     */
    private ArrayList<RTeam> teams;

    /**
     * Filter is really important, it defines how LoadTeamsTask should sort the loaded data.
     * If filter is equal to SORT_TYPE.SEARCH, query must be provided, and if it's equal to
     * SORT_TYPE.
     */
    private int filter;
    /**
     * A search query
     */
    private String query;
    /**
     * A sort token returned from CustomSort
     * Format string "TeamMetricProcessor.PROCESS_METHOD:[metric:ID]".
     * Example: 2:5
     */
    private String customSortToken;

    public interface LoadTeamsTaskListener {
        void teamsListLoaded(ArrayList<RTeam> teams);
    }

    /**
     * This will send a teams array back to the TeamsView activity if the teams have just been loaded from the file system,
     * that way the TeamsView activity can keep track of them so they don't have to be continually reloaded
     */
    private LoadTeamsTaskListener listener;

    /**
     * IO must be kept in a weak reference because it holds a context reference and we don't
     * want to risk leaking memory accidentally
     */
    private WeakReference<IO> ioWeakReference;

    /**
     * The teams array will be sent to this adapter when this task is finished
     */
    private WeakReference<TeamsRecyclerAdapter> teamsRecyclerAdapterWeakReference;
    /**
     * Reference to the progress bar so it can be made invisible again after this task finishes
     */
    private WeakReference<ProgressBar> progressBarWeakReference;
    /**
     * Reference to the recycler view so it can be made visible again after this task finishes
     */
    private WeakReference<RecyclerView> recyclerViewWeakReference;
    /**
     * Reference to the actionbar so it's subtitle can be updated after this task finishes
     */
    private WeakReference<ActionBar> actionBarWeakReference;

    /**
     * Instantiates a LoadTeamsTask.
     *
     * After that, just call setTaskParameters() and .execute() to perform an operation.
     * Also, UI should be adjusted accordingly before calling this class
     *
     * @param io IO object so that this class can interact with the file system
     *
     */
    LoadTeamsTask(IO io, TeamsRecyclerAdapter adapter, LoadTeamsTaskListener listener, ProgressBar progressBar, RecyclerView recyclerView, ActionBar actionBar) {
        this.ioWeakReference = new WeakReference<>(io);
        this.listener = listener;
        this.teamsRecyclerAdapterWeakReference = new WeakReference<>(adapter);
        this.progressBarWeakReference = new WeakReference<>(progressBar);
        this.recyclerViewWeakReference = new WeakReference<>(recyclerView);
        this.actionBarWeakReference = new WeakReference<>(actionBar);
        this.query = "";
    }

    /**
     * Should be called prior to .execute()
     *
     * Parameter notice:
     * -If filter == SORT_TYPE.ALPHABETICAL, SORT_TYPE.NUMERICAL, or SORT_TYPE.LAST_EDIT, query and customSortToken are IGNORED completely
     * -If filter == SORT_TYPE.SEARCH, the query variable will be examined, so it shouldn't be = null, but may be "", customSortToken will be ignored
     * -If filter == SORT_TYPE.CUSTOM_SORT, customSortToken will be examined, so it shouldn't be = null, but may be "", and query can be null, if not, it will perform a search also
     *
     * @param eventID the int ID of the event whose teams should be loaded
     * @param filter int according to TeamsView.SORT_TYPE
     * @param query search query string
     * @param customSortToken custom sort token string in format [TeamMetricProcessor.PROCESS_METHOD:ID], example: 2:2
     */
    void setTaskParameters(int eventID, int filter, String query, String customSortToken) {
        this.eventID = eventID;
        this.filter = filter;
        this.query = query;
        this.customSortToken = customSortToken;
        if(this.query == null) this.query = "";
        if(this.customSortToken == null) this.customSortToken = "";
    }

    /**
     * Performs loading, sorting, searching, etc.
     *
     * @param params no parameters are accepted here
     * @return will return the array that should be attached to TeamsView.teams
     */
    protected Boolean doInBackground(Void... params) {
        /*
         * Verify that the event actually exists
         */
        if(ioWeakReference.get() == null || !ioWeakReference.get().doesEventExist(eventID)) return false;

        /*
         * Next, take care of "reload", if reload is true, teams must be reloaded from the file system
         */
        if(teams == null || teams.size() == 0) {
            RTeam[] local = ioWeakReference.get().loadTeams(eventID);
            // no teams were found locally
            if(local == null || local.length == 0) {
                Log.d("RBS", "LoadTeamsTask found no teams locally stored.");
                teams = null;
                return false;
            }
            // teams were found locally, so attach them to the teams array
            teams = new ArrayList<>(Arrays.asList(local));
            listener.teamsListLoaded(teams);
            Log.d("RBS", "LoadTeamsTask loaded "+teams.size()+" teams");
        }

        /*
         * This check is necessary because the above method is conditional,
         * again, if the teams array is empty
         */
        if(teams == null || teams.size() == 0) return false;

        /*
         * Next, each RTeam contains a variable named filter, it's a sort of "helper" variable.
         * Each RTeam has a compareTo method that will allow it to easily be sorted, the filter variable within
         * the team will let the team now how it's to behave when compareTo() is called in it
         */
        for(RTeam team : teams) team.setFilter(filter);

        /*
         * If filter is ANYTHING but SORT_TYPE.SEARCH or SORT_TYPE.CUSTOM_SORT, just run a standard sort.
         * And actually, that also means that we're done with this method, return the teams!
         */
        if(filter == TeamsView.SORT_TYPE.LAST_EDIT || filter == TeamsView.SORT_TYPE.NUMERICAL || filter == TeamsView.SORT_TYPE.ALPHABETICAL) {
            for(RTeam team : this.teams) team.setFilterTag(""); // reset the filter tag

            try {
                Collections.sort(teams);
                if(filter == TeamsView.SORT_TYPE.LAST_EDIT) Collections.reverse(teams);
                return false;
            } catch(Exception e) {
                Log.d("RBS", "A problem occurred while attempting to sort teams with SORT_TYPE = " + filter);
            }
        }

        /*
         * Okay, the user actually wants to search, possibly.
         * Let's handle PURE searching next (no custom sort)
         */
        else if(filter == TeamsView.SORT_TYPE.SEARCH) {
            for(RTeam team : teams) {
                team.setCustomRelevance(0);
                team.setFilterTag("");

                // assign search relevance to the team
                processTeamForSearch(team);
            }

            try {
                Collections.sort(this.teams);
                Collections.reverse(this.teams);
                return true;
            } catch(Exception e) {
                Log.d("RBS", "A problem occurred while attempting to sort teams with SORT_TYPE = " + filter);
            }
        }

        /*
         * Alright, the user actually wants to custom sort, so, let's custom sort, also
         * check to see if query is not null, because searching is also allowing during custom sorting
         */
        else if(filter == TeamsView.SORT_TYPE.CUSTOM_SORT) {
            /*
             * Elements Processor is a powerful helper utility. How it works is the following:
             * -Elements Processor will be passed an input (int method defined by TeamMetricProcessor.PROCESS_METHOD, and ID of the metric to analyze
             * -Elements Processor will attach a string to filterTag and a relevance to customRelevance and return the team object, then just sort
             */
            TeamMetricProcessor teamMetricProcessor = new TeamMetricProcessor();

            /*
             * TeamMetricProcessor requires all inputted teams to be synced with the form, so let's do that
             */
            RForm form = ioWeakReference.get().loadForm(eventID);
            for(RTeam team : teams) {
                team.verify(form);
                ioWeakReference.get().saveTeam(eventID, team);
            }

            /*
             * Decode the method process and metric ID from the customSortToken string
             */
            int methodID = Integer.parseInt(customSortToken.split(":")[0]);
            int metricID = Integer.parseInt(customSortToken.split(":")[1]);

            Log.d("RBS", "Custom sorting with methodID: "+methodID+", metricID: "+metricID);

            /*
             * Make sure to set the extra "matchTitle" parameter if processMethod==PROCESS_METHOD.IN_MATCH
             */
            boolean shouldHideZeroRelevance = false; // for the "In match" option, zero relevance items should be hidden
            if(methodID == TeamMetricProcessor.PROCESS_METHOD.OTHER && metricID == TeamMetricProcessor.PROCESS_METHOD.OTHER_METHOD.IN_MATCH && customSortToken.split(":").length == 3) {
                teamMetricProcessor.setInMatchTitle(customSortToken.split(":")[2]);
                Log.d("RBS", "In match title: "+teamMetricProcessor.getInMatchTitle());
                shouldHideZeroRelevance = true;
            }

            /*
             * Next, perform the operation
             */
            for(RTeam team : teams) {
                team.setFilter(filter);
                team.setFilterTag(""); // reset old filter tags
                team.setCustomRelevance(0); // reset any old relevance
                teamMetricProcessor.process(team, methodID, metricID);
            }

            /*
             * Finally, check to see if the user also wants to sort through the array
             */
            for(RTeam team : teams) processTeamForSearch(team);

            try {
                Collections.sort(teams);
                // don't reverse array if sorting by "In matches" since in matches uses numerical sub sort
                if(!(methodID == TeamMetricProcessor.PROCESS_METHOD.OTHER && metricID == TeamMetricProcessor.PROCESS_METHOD.OTHER_METHOD.IN_MATCH)) Collections.reverse(teams);
                return shouldHideZeroRelevance;
            } catch(Exception e) {
                Log.d("RBS", "A problem occurred while attempting to sort teams with SORT_TYPE = " + filter);
            }
        }
        return false;
    }

    /**
     * This method will sauce some relevance onto the team object. It will check against the standard parameters
     * and add some relevance (it will not reset or override old relevance). This method is separate because
     * it's used by both SORT_TYPE.SEARCH and SORT_TYPE.CUSTOM_SORT
     *
     * This method will return automatically if the query is null
     * @param team the team to set relevance to based on query
     */
    private void processTeamForSearch(RTeam team) {
        if(query == null || query.equals("")) return;

        int relevance = 0;
        String name = team.getName().toLowerCase();
        int number = team.getNumber();

        /*
         * These are various search criteria, we'll just assign some arbitrary "relevance scores"
         * based off certain favorable criteria. NOTE: It looks tempting to put else if statements here,
         * don't!
         */
        if(name.equals(query)) relevance += 3;
        if(String.valueOf(number).equals(query)) relevance += 3;
        if(Utils.contains(name, query)) relevance += 2;
        if(name.contains(query)) relevance += 1;
        if(String.valueOf(number).contains(query)) relevance += 1;
        /*
         * There's one more relevance score that a team can earn, and that's for
         */
        if(team.getTabs() != null && team.getTabs().size() > 0) {
            StringBuilder filterTag = new StringBuilder("Contains matches: ");
            // Start the search! (use i = 2 because we aren't searching PIT or PREDICTIONS tabs)
            for(int i = 2; i < team.getTabs().size(); i++) {
                if(team.getTabs().get(i).getTitle().equalsIgnoreCase(query)
                        || team.getTabs().get(i).getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filterTag.append(team.getTabs().get(i).getTitle()).append(", ");
                }
            }
            // Alright, now filterTag contains all matches that relate to the query, let's generate a score
            if(!filterTag.toString().equals("Contains matches: ")) {
                // set the filter tag, also, remove the last comma off for nice formatting
                team.setFilterTag("\n"+team.getFilterTag() + "\n"+filterTag.toString().substring(0, filterTag.toString().length() - 2));
                relevance += 2;
            }
        }

        team.setCustomRelevance(team.getCustomRelevance() + relevance);
    }

    @Override
    protected void onPostExecute(Boolean shouldHideZeroRelevance) {
        progressBarWeakReference.get().setVisibility(View.INVISIBLE);
        recyclerViewWeakReference.get().setVisibility(View.VISIBLE);
        teamsRecyclerAdapterWeakReference.get().setTeams(teams, shouldHideZeroRelevance);

        int numTeams = 0;
        if(teams != null) numTeams = teams.size();

        StringBuilder subtitle = new StringBuilder(String.valueOf(numTeams));
        subtitle.append(" Team");
        if(numTeams != 1) subtitle.append("s");
        if(actionBarWeakReference.get() != null) actionBarWeakReference.get().setSubtitle(subtitle.toString());
    }

}

