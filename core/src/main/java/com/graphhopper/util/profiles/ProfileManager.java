package com.graphhopper.util.profiles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.Object;
import java.lang.String;


public class ProfileManager {


    private RidersProfile ridersProfile;
    private final String base = "profiles/";

    public ProfileManager(){}

    public void createProfile(String name) {
        ridersProfile = new RidersProfile();

        saveProfile(name);
    }

    public void readProfile(String name){

        FileInputStream fileInputStream = null;
        ObjectInputStream inputStream = null;
        Object obj = null;

        try {

            fileInputStream = new FileInputStream(base + name);
            inputStream = new ObjectInputStream (fileInputStream);
            obj = inputStream.readObject();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


        if (obj instanceof RidersProfile)
        {
            ridersProfile = (RidersProfile) obj;
        }
    }


    public void saveProfile(String name){
        if (ridersProfile == null)
            return;

        FileOutputStream fileOutputStream = null;
        ObjectOutputStream outputStream = null;

        try {
            fileOutputStream = new FileOutputStream(base + name);
            outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(ridersProfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public RidersProfile getProfile(String name){
        if (ridersProfile == null) {
            readProfile(name);
        }

        return ridersProfile;
    }



}
