package com.cpjd.roblu.ui.forms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.ArrayList;

/**
 * Result codes explained:
 *
 * Requests:
 * -Edit master form (extra "master" must be true)
 * -Edit event (must pass in boolean extra "editing" & form (nonnull))
 * -Create event (form and boolean extras aren't passed in)
 *
 * Result codes (by request):
 * Edit master form
 *      -Constants.MASTER_CONFIRMED // user tapped the check mark
 *          Return package: object extras "tempPit" and "tempMatch"
 *      -Constants.MASTER_DISCARDED // user pressed onBack or android.R.id.home
 * Edit event (already created)
 *      -Constants.EDIT_CONFIRMED // user tapped the check mark
 *          Return package: object extras "tempPit" and "tempMatch"
 *      -Constants.EDIT_DISCARDED // user tapped onBack or android.R.id.home
 * Create event (doesn't exist)
 *      -Constants.EVENT_CONFIRMED // user tapped the check mark
 *          Return package: object extras "tempPit" and "tempMatch"
 *      -Constants.EVENT_DISCARDED // user tapped check, still save arrays
 *          Return package: object extras "tempPit" and "tempMatch"
 *
 *
 * Things EditForm does NOT managae
 * -Creating empty form for event
 * -Using a predefined form for event
 * -Using the master form for event
 *
 * Outgoing requests from this class:
 * AddElement
 *      -Constants.REQUEST_ADD_ELEMENT // request a new element
 *      -Constants.REQUEST_EDIT_ELEMENT // request an edited element
 * Responses:
 *      -Constants.ELEMENT_CONFIRMED // user tapped check
 *          Return package: object extra "element"
 *      -Constants.ELEMENT_DISCARDED
 *          Return package: object extra "element"
 *
 */
public class EditForm extends AppCompatActivity implements View.OnClickListener, EditListener, OnTabSelectListener, EventSelectListener {

    private boolean changesMade;

    private boolean editing;
    private boolean masterForm;

    private ElementsAdapter elementsAdapter;

    // Temporary arrays
    private ArrayList<Element> tempPit, tempMatch;

    private int currentTab;

    private BottomBar bBar;
    private RUI rui;

    private Element editElement;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_form);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        bBar = ((BottomBar) findViewById(R.id.bottomBar));
        bBar.setOnTabSelectListener(this);
        BottomBarTab tab = bBar.getTabAtPosition(0);
        BottomBarTab tab2 = bBar.getTabAtPosition(1);
        tab.setBarColorWhenSelected(rui.getPrimaryColor());
        tab2.setBarColorWhenSelected(rui.getPrimaryColor());

        bBar.selectTabAtPosition(0);

        setTitle("Form editor");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // Load the form
        masterForm = getIntent().getBooleanExtra("master", false);
        editing = getIntent().getBooleanExtra("editing", false);
        RForm form;
        if(!masterForm) form = (RForm) getIntent().getSerializableExtra("form");
        else form = new Loader(getApplicationContext()).loadSettings().getMaster();

        if(masterForm) getSupportActionBar().setSubtitle("Master form");

        if(form == null) {
            ESTextfield name = new ESTextfield("Team name", false);
            ESTextfield number = new ESTextfield("Team number", true);
            name.setID(0);
            number.setID(1);
            ArrayList<Element> pit = new ArrayList<>();
            pit.add(name);
            pit.add(number);



            form = new RForm(pit, new ArrayList<Element>());
        }

        tempPit = form.getPit();
        tempMatch = form.getMatch();

        RecyclerView rv = (RecyclerView) findViewById(R.id.movie_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        elementsAdapter = new ElementsAdapter(this, this, editing);
        rv.setAdapter(elementsAdapter);

        ItemTouchHelper.Callback callback = new ElementTouchHelper(elementsAdapter, false);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        loadViews(true, 0);

        new UIHandler(this, toolbar, fab).update();
	}

    /**
     * This method does several things:
     *
     * 1) First, retrieve elements from adapter and store them in a temporary array (either pit or match)
     * 2) Tell the adapter to remove all views
     * 3) Re-load the appropriate views into the adapter
     *
     * We have to handle these events differently based on the selected tab (and whether we're editing or creating form):
     * 1) Element edited
     * 2) Element created
     * 3) Element deleted
     *
     * We have to handle these events globablly:
     * 1) Back button pressed (put everything into a RForm and return it as a result)
     * 2) Saved (put everything into a RForm and return)
     *
     * Tab 0 - pit
     * Tab 1 - match
     *
     * We're assuming that we're loading coming from the other tab
     */
	private void loadViews(boolean init, int tab) {
        currentTab = tab;
        if(tab == 0) {
            if(!init) tempMatch = elementsAdapter.getElements();
            elementsAdapter.removeAll();
            elementsAdapter.pushElements(tempPit);
        } else {
            tempPit = elementsAdapter.getElements();
            elementsAdapter.removeAll();
            elementsAdapter.pushElements(tempMatch);
        }
    }

    public void postEdit(Element e) {
        editElement = e;
        Intent intent = new Intent(this, AddElement.class);
        intent.putExtra("editing_element", e);
        startActivityForResult(intent, Constants.GENERAL);
    }

    @Override
    public void onTabSelected(@IdRes int tabId) {
        if(elementsAdapter == null) return;

        if(tabId == R.id.tab_pit) {
            loadViews(false, 0);
        } else {
            loadViews(false, 1);
        }
    }
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, AddElement.class);
        startActivityForResult(intent, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(masterForm || editing) inflater.inflate(R.menu.master_edit_form, menu);
        else inflater.inflate(R.menu.add_element, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            checkDiscard();
            return true;
        }
        if(item.getItemId() == R.id.add_element) {
            finishUp(Constants.FORM_CONFIMRED);
            return true;
        }
        if(item.getItemId() == R.id.import_from_event) {
            if(!Utils.launchEventPicker(this, this)) {
                Utils.showSnackbar(findViewById(R.id.edit_form_layout), getApplicationContext(), "No events found.", true, 0);
            }
            return true;
        }
        return false;
    }

    private void checkDiscard() {
        if((!changesMade || !editing) && !masterForm) {
            finishUp(Constants.FORM_DISCARDED);
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Discard changes?");
            builder.setMessage("Really discard changes you've made to this form?");

            builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finishUp(Constants.FORM_DISCARDED);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
            dialog.show();
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
        }
    }

    @Override
    public void onBackPressed() {
        checkDiscard();
    }

    private void finishUp(int mode) {
        if(currentTab == 0) tempPit = elementsAdapter.getElements();
        else tempMatch = elementsAdapter.getElements();

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("tempPit", tempPit);
        bundle.putSerializable("tempMatch", tempMatch);
        intent.putExtras(bundle);
        setResult(mode, intent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(resultCode == Constants.NEW_CONFIRMED) {
                Bundle bundle = data.getExtras();
                Element e = (Element) bundle.getSerializable("element");
                elementsAdapter.add(e, elementsAdapter.getNewID());
                changesMade = true;
            }
            else if(resultCode == Constants.EDIT_CONFIRMED) {
                Bundle bundle = data.getExtras();
                Element e = (Element) bundle.getSerializable("element");
                elementsAdapter.reAdd(e);
                changesMade = true;
            } else if(resultCode == Constants.EDIT_DISCARDED) {
                Bundle bundle = data.getExtras();
                elementsAdapter.reAdd(editElement);
            }
    }

    @Override
    public void eventSelected(long eventID) {
        RForm form = new Loader(getApplicationContext()).loadForm(eventID);
        tempPit = form.getPit();
        tempMatch = form.getMatch();
        elementsAdapter.pushElements(tempMatch);
        onTabSelected(R.id.tab_pit);
        bBar.getChildAt(0).setSelected(true);
    }
}
