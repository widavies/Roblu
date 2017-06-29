package com.cpjd.roblu.models;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
 * Backup allows an entire event to be stored within an instance
 * of RBackup.
 *
 * @since 3.5.5
 * @author Will Davies
 */
@Data
public class RBackup implements Serializable {
    private REvent event;
    private RTeam[] teams;
    private RForm form;
    private ArrayList<RImage> images;
    private final String fileVersion;

    public RBackup(REvent event, RTeam[] teams, RForm form) {
        this.event = event;
        this.teams = teams;
        this.form = form;

        fileVersion = Loader.PREFIX;
    }
}
