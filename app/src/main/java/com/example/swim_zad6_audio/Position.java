package com.example.swim_zad6_audio;

public class Position {

    String Name, Surname, Title, Discription, Path, Date;

    public Position(String Name, String Surname, String Title, String Discription,String Date, String Path){
        this.Name = Name;
        this.Surname = Surname;
        this.Title = Title;
        this.Discription = Discription;
        this.Path = Path;
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

    public String getPath() {
        return Path;
    }

    public String getDate() {
        return Date;
    }
}
