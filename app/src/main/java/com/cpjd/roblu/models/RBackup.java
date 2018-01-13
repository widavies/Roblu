package com.cpjd.roblu.models;

import com.cpjd.roblu.io.IO;

import java.io.Serializable;

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
    private final String fileVersion;

    public RBackup(REvent event, RTeam[] teams, RForm form) {
        this.event = event;
        this.teams = teams;
        this.form = form;

        fileVersion = IO.PREFIX;
    }
}
