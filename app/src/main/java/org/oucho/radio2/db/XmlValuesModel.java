package org.oucho.radio2.db;


public class XmlValuesModel {


    private  String url = "";
    private  String name = "";


    public void setcompany(String url) {
        this.url = url;
    }

    public void setaddress(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}
