package com.cpjd.roblu.ui.forms;

import java.io.Serializable;
import java.util.ArrayList;

public interface ElementsListener extends Serializable {

    void nameInited(String name);
    void booleanUpdated(int ID, int value);
    void counterUpdated(int ID, int value);
    void sliderUpdated(int ID, int value);
    void chooserUpdated(int ID, int selected);
    void checkboxUpdated(int ID, ArrayList<Boolean> checked);
    void stopwatchUpdated(int ID, double time);
    void textfieldUpdated(int ID, String value);
}
