package com.cpjd.roblu.cloud.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;

/**
 * Allows the user to create a Roblu Cloud Team,
 * manages payment / free trial as well.
 *
 * Roblu will cost $50 bucks annually, with a 7 day free trial.
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class CreateCloudTeam extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_cloud_team);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_element, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.confirm) {
            /*
             * Here we need to:
             * -Process payments
             * -Create team (with name, number, and master email account)
             * -Get a new team code
             */

            String name = ((TextView)findViewById(R.id.editText1)).getText().toString();
            String number = ((TextView)findViewById(R.id.editText2)).getText().toString();
            String ownerEmail = new Loader(getApplicationContext()).loadSettings().getAdminEmail();

            Intent result = new Intent();
            result.putExtra("teamCode", "a234zdf");
            setResult(Constants.CLOUD_TEAM_CREATED, result);
            finish();
            return true;
        }
        return false;
    }

}
