package org.oucho.radio2.categories.model;

import com.google.gson.annotations.SerializedName;

public class BrowsableItem {
    private String element;
    private String type;
    private String text;
    @SerializedName("URL")
    private String url;

    public String getElement() {
        return element;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }
}
