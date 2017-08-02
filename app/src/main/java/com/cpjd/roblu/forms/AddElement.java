package com.cpjd.roblu.forms;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.elements.EBoolean;
import com.cpjd.roblu.forms.elements.ECheckbox;
import com.cpjd.roblu.forms.elements.EChooser;
import com.cpjd.roblu.forms.elements.ECounter;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.forms.elements.ESlider;
import com.cpjd.roblu.forms.elements.EStopwatch;
import com.cpjd.roblu.forms.elements.ETextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;

import java.util.ArrayList;
import java.util.Arrays;

public class AddElement extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ElementsListener {

    private Element e; // if we're editing

    private Elements elements;
    private LinearLayout layout;
    private Spinner types;
    private RelativeLayout.LayoutParams params;

    // Element type IDs, should match the order of the strings
    private final static String[] ELEMENTS = {"Boolean", "Counter", "Slider", "Chooser", "Checkbox", "Stopwatch", "Text field", "Gallery"};
    private final static int BOOLEAN = 0;
    private final static int COUNTER = 1;
    private final static int SLIDER = 2;
    private final static int CHOOSER = 3;
    private final static int CHECKBOX = 4;
    private final static int STOPWATCH = 5;
    private final static int TEXTFIELD = 6;
    private final static int GALLERY = 7;

    // Temp variables for storing temporary values
    private String title;
    private int max = 100, min = 0, increment = 1, currentValue = 0;
    private ArrayList<String> values;
    private ArrayList<Boolean> checked;
    private double timeValue = 0.0;
    private int selected = 0;
    private RUI rui;
    private int booleanCurrent = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_element);

        Toolbar toolbar = (Toolbar) findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.clear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        values = new ArrayList<>();
        checked = new ArrayList<>();

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        elements = new Elements(AddElement.this, rui, this, true);

        try { e = (Element) getIntent().getSerializableExtra("editing_element"); } catch(Exception e) { System.out.println("Non-error, but no element was passed in");}
        setTitle("Add element");

        layout = (LinearLayout) findViewById(R.id.add_element_layout);
        if(e == null) layout.addView(initHub());
        else {
            updateLayout(getEditType());
        }

        new UIHandler(this, toolbar).update();
    }

    private CardView initHub() {
        //toolbar.setId(Text.generateViewId());

        RelativeLayout layout = new RelativeLayout(this);

        params = newParams();
        params.addRule(RelativeLayout.CENTER_VERTICAL);
       // params.addRule(RelativeLayout.BELOW, toolbar.getId());

        TextView elements = new TextView(this);
        elements.setTextColor(rui.getText());
        elements.setId(Text.generateViewId());
        elements.setText(R.string.add_element);
        elements.setLayoutParams(params);
        layout.addView(elements);

        params = newParams();
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        types = new Spinner(this);
        types.setId(Text.generateViewId());
        types.setSelection(0);
        types.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        types.setOnItemSelectedListener(this);
        types.setPadding(Text.DPToPX(this, 15), types.getPaddingTop(), types.getPaddingRight(), types.getPaddingBottom());
        types.setLayoutParams(params);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, ELEMENTS);
        types.setAdapter(adapter);
        layout.addView(types);

        CardView card = new CardView(this);
        card.setRadius(0);
        card.setContentPadding(Text.DPToPX(this, 10), Text.DPToPX(this, 10), Text.DPToPX(this, 10), Text.DPToPX(this, 10));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(rui.getCardColor());
        card.addView(layout);

        return card;
    }

    private void addBasic() {
        if(e == null) {
            checked.clear(); checked.add(false);
            values.clear(); values.add("");
        }

        RelativeLayout layout = new RelativeLayout(this);
        TextInputLayout name = getNameField();
        layout.addView(name);
        this.layout.addView(getMetadataCard(layout));
    }

    private void addSlider() {
        ESlider s = (ESlider)e;

        RelativeLayout layout = new RelativeLayout(this);
        TextInputLayout name = getNameField();
        layout.addView(name);

        TextInputLayout maxLayout = new TextInputLayout(this);
        maxLayout.setId(Text.generateViewId());
        maxLayout.setHint("Maximum");
        AppCompatEditText maxInput = new  AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), maxLayout, maxInput);
        maxInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(e != null) {
            maxInput.setText(String.valueOf(s.getMax()));
        }
        maxInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().equals("")) charSequence = "0";
                if(charSequence.toString().length() >= 11) charSequence = charSequence.toString().substring(0, 10);

                max = Integer.parseInt(charSequence.toString());
                Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                ViewGroup view = (ViewGroup) tl.getChildAt(0);
                RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                TextView text = (TextView) t.getChildAt(4);
                text.setText(charSequence);
                SeekBar s = (SeekBar) t.getChildAt(1);
                s.setMax(Integer.parseInt(charSequence.toString()));
                TextView current = (TextView) t.getChildAt(3);
                if(Integer.parseInt(current.getText().toString()) > max) current.setText(String.valueOf(max));

            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, name.getId());
        maxLayout.setLayoutParams(params);
        maxLayout.addView(maxInput);
        layout.addView(maxLayout);
        this.layout.addView(getMetadataCard(layout));
    }

    private void addCounter() {
        ECounter c = (ECounter)e;

        RelativeLayout layout = new RelativeLayout(this);
        TextInputLayout name = getNameField();
        layout.addView(name);

        // ADD MINIMUM FIELD
        TextInputLayout minLayout = new TextInputLayout(this);
        minLayout.setId(Text.generateViewId());
        minLayout.setHint("Minimum");
        AppCompatEditText minInput = new AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), minLayout, minInput);

        if(e != null) {
            minInput.setText(String.valueOf(c.getMin()));
        }
        minInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().equals("")) charSequence = "0";
                if(charSequence.toString().length() >= 11) charSequence = charSequence.toString().substring(0, 10);

                min = Integer.parseInt(charSequence.toString());
                Elements.min = min;
                Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                ViewGroup view = (ViewGroup) tl.getChildAt(0);
                RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                TextView text = (TextView) t.getChildAt(2);
                if(Integer.parseInt(text.getText().toString()) < min) {
                    text.setText(String.valueOf(min));
                    currentValue = min;
                }

            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, name.getId());
        minLayout.setLayoutParams(params);
        minLayout.addView(minInput);
        layout.addView(minLayout);

        // ADD MAXIMUM FIELD
        TextInputLayout maxLayout = new TextInputLayout(this);
        maxLayout.setId(Text.generateViewId());
        maxLayout.setHint("Maximum");
        AppCompatEditText maxInput = new  AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), maxLayout, maxInput);
        maxInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(e != null) {
            maxInput.setText(String.valueOf(c.getMax()));
        }
        maxInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().equals("")) charSequence = "0";
                if(charSequence.toString().length() >= 11) charSequence = charSequence.toString().substring(0, 10);

                max = Integer.parseInt(charSequence.toString());
                Elements.max = max;
                Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                ViewGroup view = (ViewGroup) tl.getChildAt(0);
                RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                TextView text = (TextView) t.getChildAt(2);
                if(Integer.parseInt(text.getText().toString()) > max) {
                    text.setText(String.valueOf(max));
                    currentValue = max;
                }
                if(Integer.parseInt(text.getText().toString()) < min) {
                    text.setText(String.valueOf(min));
                    currentValue = min;
                }

            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, minLayout.getId());
        maxLayout.setLayoutParams(params);
        maxLayout.addView(maxInput);
        layout.addView(maxLayout);

        // ADD INCREMENT FIELD
        TextInputLayout incLayout = new TextInputLayout(this);
        incLayout.setId(Text.generateViewId());
        incLayout.setHint("Increment");
        AppCompatEditText incInput = new  AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), incLayout, incInput);
        incInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(e != null) {
            incInput.setText(String.valueOf(c.getIncrement()));
        }
        incInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().equals("")) charSequence = "1";
                if(charSequence.toString().length() >= 11) charSequence = charSequence.toString().substring(0, 10);
                Elements.increment = Integer.parseInt(charSequence.toString());
                increment = Elements.increment;
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, maxLayout.getId());
        incLayout.setLayoutParams(params);
        incLayout.addView(incInput);
        layout.addView(incLayout);

        this.layout.addView(getMetadataCard(layout));
    }

    private void addChooserOrCheckbox() {
        RelativeLayout layout = new RelativeLayout(this);
        TextInputLayout name = getNameField();
        layout.addView(name);

        TextInputLayout itemLayout = new TextInputLayout(this);
        itemLayout.setId(Text.generateViewId());
        itemLayout.setHint("Comma seperated list");
        AppCompatEditText itemInput = new  AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), itemLayout, itemInput);
        if(e != null) {
            try {
                if(getEditType() == CHECKBOX) {
                    this.checked = ((ECheckbox)e).getChecked();
                }

                ArrayList<String> values;
                if(getEditType() == CHOOSER) values = ((EChooser)e).getValues();
                else values = ((ECheckbox)e).getValues();
                this.values = values;

                String text = "";
                for(String s : values) {
                    text += s + ",";
                }
                text = text.substring(0, text.length() - 1);
                itemInput.setText(text);
            } catch(Exception e) {
                System.out.println("Error parsing comma metadata");
            }
        }

        itemInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int selectedPosition;
                try {
                    selectedPosition = types.getSelectedItemPosition();
                } catch(Exception e) {
                    selectedPosition = -1;
                }

                if(getEditType() == CHOOSER || selectedPosition == CHOOSER) {
                    try {
                        String[] items = charSequence.toString().split(",");
                        Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                        ViewGroup view = (ViewGroup) tl.getChildAt(0);
                        RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                        Spinner spinner = (Spinner) t.getChildAt(1);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AddElement.this, R.layout.spinner_item, items);
                        spinner.setAdapter(adapter);
                        values.clear();
                        values.addAll(Arrays.asList(items));

                    } catch (Exception e) {
                        System.out.println("Error parsing chooser metadata");
                    }
                } else {
                    RelativeLayout.LayoutParams params;
                    Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                    ViewGroup view = (ViewGroup) tl.getChildAt(0);
                    RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                    try {
                        for(int k = 1; k < t.getChildCount(); i++) {
                            t.removeViewAt(k);
                        }
                        TextView title = (TextView) t.getChildAt(0);
                        String[] items = charSequence.toString().split(",");
                        AppCompatCheckBox[] boxes = new AppCompatCheckBox[items.length];
                        values.clear();
                        checked.clear();
                        for(int k = 0; k < boxes.length; k++) {
                            values.add(items[k]);
                            params = newParams();
                            params.addRule(RelativeLayout.RIGHT_OF, title.getId());
                            if(k > 0) params.addRule(RelativeLayout.BELOW, boxes[k - 1].getId());
                            else params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                            AppCompatCheckBox box = new AppCompatCheckBox(AddElement.this);
                            box.setId(Text.generateViewId());
                            box.setTag(k);
                            ColorStateList colorStateList = new ColorStateList(
                                    new int[][] {
                                            new int[] { -android.R.attr.state_checked }, // unchecked
                                            new int[] {  android.R.attr.state_checked }  // checked
                                    },
                                    new int[] {
                                            rui.getText(),
                                            rui.getAccent()
                                    }
                            );
                            box.setSupportButtonTintList(colorStateList);
                            box.setText(items[k]);
                            box.setLayoutParams(params);
                            checked.add(false);
                            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                    checked.set((Integer)compoundButton.getTag(), b);
                                }
                            });
                            boxes[k] = box;
                        }
                        for(AppCompatCheckBox box : boxes) t.addView(box);

                    } catch (Exception e) {
                        for(int k = 1; k < t.getChildCount(); i++) {
                            t.removeViewAt(k);
                        }
                    }
                }

            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, name.getId());
        itemLayout.setLayoutParams(params);
        itemLayout.addView(itemInput);
        layout.addView(itemLayout);
        this.layout.addView(getMetadataCard(layout));
    }

    private void updateLayout(int position) {
        Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
        tl.setBackgroundColor(rui.getPrimaryColor());
        tl.removeAllViews();

        if(e != null) {
            title = e.getTitle();
        }
        switch(position) {
            case BOOLEAN:
                if(e != null) {
                    int value =((EBoolean)e).getValue();
                    booleanCurrent = value;
                    tl.addView(elements.getBoolean(-1, e.getTitle(), value, !e.isModified()));
                }
                else tl.addView(elements.getBoolean(-1, "Boolean", -1, true));
                addBasic();
                break;
            case COUNTER:
                if(e != null) {
                    min = ((ECounter)e).getMin();
                    max = ((ECounter)e).getMax();
                    increment = ((ECounter)e).getIncrement();
                    currentValue = ((ECounter)e).getCurrent();
                    tl.addView(elements.getCounter(-1, e.getTitle(), min, max, increment, currentValue, !e.isModified()));
                }
                else tl.addView(elements.getCounter(-1, "Counter", 0, 100, 1, 0, false));
                addCounter();
                break;
            case SLIDER:
                if(e != null) {
                    max = ((ESlider)e).getMax();
                    currentValue = ((ESlider)e).getCurrent();
                    tl.addView(elements.getSlider(-1, e.getTitle(), max, currentValue, !e.isModified()));
                }
                else tl.addView(elements.getSlider(-1, "Slider", 100, 0, false));
                addSlider();
                break;
            case CHOOSER:
                if(e != null) {
                    values = ((EChooser)e).getValues();
                    selected =  ((EChooser)e).getSelected();
                    tl.addView(elements.getChooser(-1, e.getTitle(), values, selected));
                }
                else tl.addView(elements.getChooser(-1, "Chooser", null, 0));
                addChooserOrCheckbox();
                break;
            case CHECKBOX:
                if(e != null) {
                    values = ((ECheckbox)e).getValues();
                    checked = ((ECheckbox)e).getChecked();
                    tl.addView(elements.getCheckbox(-1, e.getTitle(), values, checked));
                }
                else tl.addView(elements.getCheckbox(-1, "Checkbox", null, null));
                addChooserOrCheckbox();
                break;
            case STOPWATCH:
                if(e != null) {
                    timeValue = ((EStopwatch)e).getTime();
                    tl.addView(elements.getStopwatch(-1, e.getTitle(), timeValue, !e.isModified()));
                }
                else tl.addView(elements.getStopwatch(-1, "Stopwatch", 0.0, false));
                addBasic();
                break;
            case TEXTFIELD:
                if(e != null) {
                    values.add(((ETextfield)e).getText());
                    tl.addView(elements.getTextfield(-1, e.getTitle(), values.get(0)));
                }
                else tl.addView(elements.getTextfield(-1, "Text field", ""));
                addBasic();
                break;
            case GALLERY:
                if(e != null) {
                    tl.addView(elements.getGallery(-1, e.getTitle(), true, null, null, -1));
                }
                else tl.addView(elements.getGallery(-1, "Gallery", true, null, null, -1));
                addBasic();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            if(e != null) {
                Bundle b = new Bundle();
                b.putSerializable("element", e);
                Intent data = new Intent();
                data.putExtras(b);
                setResult(Constants.EDIT_DISCARDED, data);
            } else setResult(Constants.NEW_DISCARDED);
            finish();
            return true;
        }
        if(item.getItemId() == R.id.add_element) {
            if(e == null) processResult(types.getSelectedItemPosition());
            else processResult(getEditType());
            return true;
        }
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if(e != null) return;
        ((TextView) adapterView.getChildAt(0)).setTextColor(rui.getText());
        try { layout.removeViewAt(3); } catch(Exception e) { System.out.println("Failed to remove view from layout");}
        updateLayout(i);
    }

    // user tapped confirm
    private void processResult(int type) {
        Element e = null;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        if(type == BOOLEAN) e = new EBoolean(title, booleanCurrent);
        else if(type == SLIDER) e = new ESlider(title, max, currentValue);
        else if(type == COUNTER) e = new ECounter(title, min, max, increment, currentValue);
        else if(type == CHOOSER) e = new EChooser(title,  values, selected);
        else if(type == CHECKBOX) e = new ECheckbox(title, values, checked);
        else if(type == STOPWATCH) e = new EStopwatch(title, Text.round(timeValue, 1));
        else if(type == TEXTFIELD) e = new ETextfield(title, values.get(0));
        else if(type == GALLERY) e = new EGallery(title);
        if(this.e != null && e != null) e.setID(this.e.getID());
        bundle.putSerializable("element", e);
        intent.putExtras(bundle);
        if(this.e == null) setResult(Constants.NEW_CONFIRMED, intent);
        else setResult(Constants.EDIT_CONFIRMED, intent);
        finish();
    }

    private int getEditType() {
        if(e instanceof EBoolean) return BOOLEAN;
        else if(e instanceof ECheckbox) return CHECKBOX;
        else if(e instanceof ETextfield) return TEXTFIELD;
        else if(e instanceof EStopwatch) return STOPWATCH;
        else if(e instanceof ECounter) return COUNTER;
        else if(e instanceof EChooser) return CHOOSER;
        else if(e instanceof ESlider) return SLIDER;
        else if(e instanceof EGallery) return GALLERY;
        return 0;
    }

    private RelativeLayout.LayoutParams newParams() {
        return new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextInputLayout getNameField() {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Name");
        inputLayout.setId(Text.generateViewId());
        AppCompatEditText nameInput = new  AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), inputLayout, nameInput);
        nameInput.setTextColor(rui.getText());
        nameInput.setHighlightColor(rui.getAccent());
        nameInput.setId(Text.generateViewId());
        if(e != null) {
            nameInput.setText(e.getTitle());
        }

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
                ViewGroup view = (ViewGroup) tl.getChildAt(0);
                RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                TextView text = (TextView) t.getChildAt(0);
                text.setText(charSequence);
                title = charSequence.toString();
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        inputLayout.setLayoutParams(params);
        inputLayout.addView(nameInput);
        inputLayout.requestFocus();
        return inputLayout;
    }

    private CardView getMetadataCard(View layout) {
        CardView card = new CardView(getApplicationContext());
        card.setBackgroundColor(rui.getCardColor());
        card.setRadius(rui.getFormRadius());
        card.setContentPadding(Text.DPToPX(this, 8), Text.DPToPX(this, 8), Text.DPToPX(this, 8), Text.DPToPX(this, 8));
        card.setUseCompatPadding(true);
        card.addView(layout);
        return card;
    }

    @Override
    public void nameInited(String name) {
        title = name;
    }

    @Override
    public void booleanUpdated(int ID, int b) {
        booleanCurrent = b;
    }

    @Override
    public void counterUpdated(int ID, int value) {
        currentValue = value;
    }

    @Override
    public void sliderUpdated(int ID, int value) {
        currentValue = value;
    }

    @Override
    public void chooserUpdated(int ID, int selected) {
        this.selected = selected;
    }

    @Override
    public void checkboxUpdated(int ID, ArrayList<Boolean> checked) {
        this.checked.clear();
        for(boolean b : checked) this.checked.add(b);
    }

    @Override
    public void stopwatchUpdated(int ID, double time) {
        timeValue = time;
    }

    @Override
    public void textfieldUpdated(int ID, String value) {
        values.clear();
        values.add(value);
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_element, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }
}
