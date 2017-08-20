package org.oucho.radio2.categories.model;

import com.google.gson.annotations.SerializedName;

public class TuneInItem extends BrowsableItem {
    private String subtext;
    private String image;
    @SerializedName("current_track")
    private String currentTrack;

    public String getSubtext() {
        return subtext;
    }

    public String getImage() {
        return image;
    }

    public String getCurrentTrack() {
        return currentTrack;
    }
}
