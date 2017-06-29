package com.cpjd.roblu.teams;

import android.view.View;

import com.cpjd.roblu.models.RTeam;

interface TeamsItemClickListener {
    void onItemClick(View v);
    void deleteTeam(RTeam team);
}
