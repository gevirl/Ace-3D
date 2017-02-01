/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.rhwlab.dispim.nucleus.PrettyWriter;

/**
 *
 * @author gevirl
 */
public class Properties {
    public Properties(){
        
    }
    public void open(String file) {
        File f = new File(file);
        if (f.exists()){
            try {
                JsonReader reader = Json.createReader(new FileReader(file));
                JsonObject obj = reader.readObject();
                for (String key : obj.keySet()){
                    String val = obj.getString(key);
                    props.put(key, val);
                }
                reader.close();                
            } catch (Exception exc){
                exc.printStackTrace();
            }
        }
        this.file = file;
    }
    public String getProperty(String name){
        return props.get(name);
    }
    public void setProperty(String name,String value){
        props.put(name, value);
    }
    public void save()throws Exception{
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (String name : props.keySet()){
            builder.add(name, props.get(name));
        }
        PrintWriter writer = new PrintWriter(file);
        PrettyWriter pretty = new PrettyWriter(writer);
        pretty.writeObject(builder.build(), 0);
        writer.close();       
    }
    String file;
    TreeMap<String,String> props = new TreeMap<>();
}
