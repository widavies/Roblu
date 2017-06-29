package com.cpjd.roblu.forms;

import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.forms.elements.EBoolean;
import com.cpjd.roblu.forms.elements.ECheckbox;
import com.cpjd.roblu.forms.elements.EChooser;
import com.cpjd.roblu.forms.elements.ECounter;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.EStopwatch;
import com.cpjd.roblu.forms.elements.ETextfield;
import com.cpjd.roblu.forms.elements.Element;

import java.util.ArrayList;

class PForms {

    private final int year;
    private final ArrayList<Element> pit, match;
    private final ArrayList<String> defaultChoices;

    PForms(int year) {
        this.year = year;
        pit = new ArrayList<>();
        match = new ArrayList<>();

        defaultChoices = new ArrayList<>();
        defaultChoices.add("Yes");
        defaultChoices.add("No");
        defaultChoices.add("Sometimes");
        defaultChoices.add("Unknown");
    }

    public RForm getForm() {
        switch(year) {
            case 2016:
                return get2016();
            case 2017:
                return get2017();
            default:
                return null;
        }
    }

    private RForm get2017() {
        ArrayList<String> pos = new ArrayList<>();
        pos.add("Left");
        pos.add("Center");
        pos.add("Right");

        ArrayList<String> items = new ArrayList<>();
        items.add("Floor");
        items.add("Hopper");
        items.add("Load station");
        items.add("Climb");
        items.add("Gears");
        items.add("Gear time okay");

        ArrayList<Boolean> defaultValues = new ArrayList<>();
        for(int i = 0; i < pos.size(); i++) defaultValues.add(false);

        ArrayList<Boolean> defaultValues2 = new ArrayList<>();
        for(int i = 0; i < items.size(); i++) defaultValues2.add(false);

        pit.add(new ESTextfield("Team name", false));
        pit.add(new ESTextfield("Team number", true));
        pit.add(new ETextfield("Comments", ""));
        pit.add(new EGallery("Pictures"));

        match.add(new ECounter("Points possible", 0, 10000, 1, 0));
        match.add(new ECounter("Shots", 0, 10000, 1, 0));
        match.add(new ECounter("High shots", 0, 10000, 1, 0));
        match.add(new ECounter("Gears lifted", 0, 10000, 1, 0));
        match.add(new EChooser("Autonomous?",defaultChoices,3));
        match.add(new EBoolean("Teleoperated?", -1));
        match.add(new ECheckbox("Position", pos, defaultValues));
        match.add(new ECheckbox("Items", items, defaultValues2));
        match.add(new EStopwatch("Climb time", 0));
        match.add(new ETextfield("Comments", ""));

        for(int i = 0; i < pit.size(); i++) pit.get(i).setID(i);
        for(int i = 0; i < match.size(); i++) match.get(i).setID(i);

        return new RForm(pit, match);

    }

    private RForm get2016() {
        ArrayList<String> defends = new ArrayList<>();
        defends.add("Portcullis");
        defends.add("Cheval de Frise");
        defends.add("Moat");
        defends.add("Ramparts");
        defends.add("Drawbridge");
        defends.add("Sally port");
        defends.add("Rock wall");
        defends.add("Rough terrain");
        defends.add("Low bar");

        ArrayList<Boolean> defaultValues = new ArrayList<>();
        for(int i = 0; i < defends.size(); i++) defaultValues.add(false);

        pit.add(new ESTextfield("Team name", false));
        pit.add(new ESTextfield("Team number", true));
        pit.add(new ETextfield("Comments", ""));

        match.add(new ECounter("Points possible", 0, 10000, 1, 0));
        match.add(new EChooser("Autonomous?",defaultChoices,3));
        match.add(new ECheckbox("Defenses", defends, defaultValues));
        match.add(new ETextfield("Comments", ""));

        for(int i = 0; i < pit.size(); i++) pit.get(i).setID(i);
        for(int i = 0; i < match.size(); i++) match.get(i).setID(i);

        return new RForm(pit, match);
    }
}
