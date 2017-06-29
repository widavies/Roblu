package com.cpjd.roblu.cloud.ui.management;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;

/**
 * Created by Will Davies on 6/17/2017.
 */

public class ManageFragment extends Fragment implements View.OnClickListener{

    private View view;
    private RelativeLayout layout;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.cloud_tab, container, false);

        layout = (RelativeLayout) view.findViewById(R.id.cloud_layout);

        view.findViewById(R.id.create_team).setOnClickListener(this);

        /*
         * Determine which UI needs to be shown - create team or manage team
         */

        setUI(new Loader(getActivity()).loadSettings().getTeamCode() == null || new Loader(getActivity()).loadSettings().getTeamCode().equals(""));
        return view;
    }

    public void setUI(boolean signedIn) {
        if(!signedIn) {
            Button premium = (Button) view.findViewById(R.id.premium);
            Button support = (Button) view.findViewById(R.id.support);
            Button delete = (Button) view.findViewById(R.id.delete);
            EditText name = (EditText) view.findViewById(R.id.editText1);
            EditText number = (EditText) view.findViewById(R.id.editText2);
            premium.setVisibility(View.GONE);
            support.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
            number.setVisibility(View.GONE);
        } else {
            Button create = (Button) view.findViewById(R.id.create_team);
            create.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        setUI(true);
    }
}
