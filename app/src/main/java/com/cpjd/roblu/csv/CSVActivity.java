package com.cpjd.roblu.csv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.cpjd.roblu.BuildConfig;
import com.cpjd.roblu.R;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.ui.UIHandler;

import java.io.File;
import java.util.ArrayList;

/**
 * Allows the user to select several .CSV options for export
 *
 * @since 4.1.0
 * @version 1
 * @author Will Davies
 */
public class CSVActivity extends AppCompatActivity implements ExportCSVTask.ExportCSVListener {

    private REvent event;

    private ProgressDialog pd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv);

        event = (REvent) getIntent().getSerializableExtra("event");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("CSV Exporter");

        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.csv_menu, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.export) {
            // Get verboseness ID
            Spinner spinner = findViewById(R.id.verboseness);

            Spinner fileTypes = findViewById(R.id.file_type);

            // Check the check lists
            CheckBox matchData = findViewById(R.id.match_data);
            CheckBox pitData = findViewById(R.id.pit_data);
            CheckBox matchList = findViewById(R.id.match_list);
            CheckBox matchLookup = findViewById(R.id.match_lookup);
            CheckBox ourMatches = findViewById(R.id.our_matches);
            CheckBox fieldData = findViewById(R.id.field_data);
            ArrayList<Integer> enabledSheets = new ArrayList<>();

            if(matchData.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.MATCH_DATA);
            if(pitData.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.PIT_DATA);
            if(matchList.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.MATCH_LIST);
            if(matchLookup.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.MATCH_LOOKUP);
            if(ourMatches.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.OUR_MATCHES);
            if(fieldData.isChecked()) enabledSheets.add(ExportCSVTask.SHEETS.FIELD_DATA);

            String fileName = ((AppCompatEditText)findViewById(R.id.file_name)).getText().toString();

            if(enabledSheets.size() == 0) {
                Toast.makeText(getApplicationContext(), "You must select at least 1 sheet before exporting.", Toast.LENGTH_LONG).show();
                return true;
            }

            pd = ProgressDialog.show(CSVActivity.this, "Generating spreadsheet file...", "This may take several moments...", false);
            pd.setCancelable(false);
            pd.show();
            new ExportCSVTask(getApplicationContext(), this, event, fileName, fileTypes.getSelectedItemPosition() == 0, enabledSheets, spinner.getSelectedItemPosition()).start();
        }
        else if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    public void errorOccurred(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "An error occurred while exporting CSV: "+message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void csvFileGenerated(final File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.dismiss();
                Log.d("RBS", "CSV file successfully generated.");
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID, file);
                intent.setType("application/vnd.ms-excel");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                PackageManager pm = getPackageManager();
                if(intent.resolveActivity(pm) != null) {
                    startActivity(Intent.createChooser(intent, "Export spreadsheet to..."));
                }
//                    Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Error occurred while generating .CSV file: "+message, true, 0);
            }
        });
    }
}
