package com.cpjd.roblu.models;

// Serializes the objects

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class IO {

    private final Context context;

    IO(Context context) {
        this.context = context;

        File f = new File(context.getFilesDir(), Loader.PREFIX+File.separator);
        if(!f.exists()) {
            boolean result = f.mkdir();
            if(!result) System.out.println("prefix directory could not be created");
            File events = new File(context.getFilesDir(), Loader.PREFIX+File.separator+"events");
            boolean result2 = events.mkdir();
            if(!result2) System.out.println("events directory could not be created");
            File requests = new File(context.getFilesDir(), Loader.PREFIX+File.separator+"requests");
            requests.mkdir();
            File mycheckouts = new File(context.getFilesDir(), Loader.PREFIX+File.separator+"checkoutsconflicts");
            mycheckouts.mkdir();

            File checkouts = new File(context.getFilesDir(), Loader.PREFIX+File.separator+"checkouts");
            checkouts.mkdir();
        }


    }

    File[] getChildFiles(String location) {
        File file = new File(context.getFilesDir(), location);
        return file.listFiles();
    }

    void serializeObject(Object object, String location) {
        try {
            File file = new File(context.getFilesDir(), location);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void serializeObject(Object object, File location) {
        try {
            FileOutputStream fos = new FileOutputStream(location);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    Object deserializeObject(String location) {
        try {
            FileInputStream fis = new FileInputStream(new File(context.getFilesDir(), location));
            ObjectInputStream in = new ObjectInputStream(fis);
            Object o = in.readObject();
            in.close();
            fis.close();
            return o;
        } catch(Exception e) {
            return null;
        }
    }

    Object deserializeObject(File location) {
        try {
            FileInputStream fis = new FileInputStream(location);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object o = in.readObject();
            in.close();
            fis.close();
            return o;
        } catch(Exception e) {
            return null;
        }
    }

    protected void delete(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    delete(f);
                } else {
                    boolean result = f.delete();
                    if(!result) System.out.println("Failed to delete file");
                }
            }
        }
        boolean result = folder.delete();
        if(!result) System.out.println("Failed to delete folder");
    }
}
