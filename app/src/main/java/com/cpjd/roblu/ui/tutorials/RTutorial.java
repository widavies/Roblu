package com.cpjd.roblu.ui.tutorials;

import lombok.Data;

@Data
class RTutorial {

    private final String title;
    private final String subtitle;
    private final String youtubeID;

    RTutorial(String title, String subtitle, String youtubeID) {
        this.title = title;
        this.subtitle = subtitle;
        this.youtubeID = youtubeID;
    }

}
