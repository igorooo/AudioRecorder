package com.example.swim_zad6_audio;

import java.io.File;

public class Position {

    String Name, Surname, Title, Discription, Date;
    File file;

    public Position(String Name, String Surname, String Title, String Discription,String Date, File file){
        this.Name = Name;
        this.Surname = Surname;
        this.Title = Title;
        this.Discription = Discription;
        this.file = file;
        this.Date = Date;
    }

    public String getTitle() {
        return Title;
    }

    public String getName() {
        return Name;
    }

    public String getSurname() {
        return Surname;
    }

    public String getDiscription() {
        return Discription;
    }

    public File getFile() {
        return file;
    }

    public String getDate() {
        return Date;
    }
}
