package com.moodld.moodld;

/**
 * Created by Nihal on 05-07-2016.
 */
public class Course {
    String name;
    String path;
    String url;
    Boolean isChecked;
    String newsForumUrl;
    String lastMain;
    String lastNF;

    public Course(String name, String url) {
        this.name = name ;
        this.url = url;
        this.path = this.newsForumUrl = this.lastMain = this.lastNF = "";
        this.isChecked = Boolean.FALSE;
    }

    public Course(String name, String url, String path) {
        this.name = name ;
        this.url = url;
        this.path = path;
        this.newsForumUrl = this.lastMain = this.lastNF = "";
        this.isChecked = Boolean.FALSE;
    }

    public String getName(){
        return name;
    }

    public String getPath(){
        return path;
    }

    public String getUrl(){
        return url;
    }

    public Boolean isChecked(){
        return isChecked;
    }

    public String getNewsforumurl(){
        return newsForumUrl;
    }

    public String getLastmain(){
        return lastMain;
    }

    public String getLastnf(){
        return lastNF;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public void toggleChecked() {
        this.isChecked = !this.isChecked;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setNewsForumUrl(String newsForumUrl) {
        this.newsForumUrl = newsForumUrl;
    }

    public void setLastMain(String lastMain) {
        this.lastMain = lastMain;
    }

    public void setLastNF(String lastNF) {
        this.lastNF = lastNF;
    }

    public String toString() {
        return "Name: " + name + " URL: " + url + " Path: " + path + "isChecked: " + isChecked + " newsForumUrl" + newsForumUrl + " lastMain" + lastMain + " lastNF" + lastNF  ;
    }

}


