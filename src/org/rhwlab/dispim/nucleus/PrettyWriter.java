/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 *
 * @author gevirl
 */
public class PrettyWriter {
    public PrettyWriter(){
        writer = new StringWriter();
        builder = new PrintWriter(writer);
    }
    public PrettyWriter(PrintWriter pw){
        builder = pw;
    }
    public String getPrettyJson(){
        if (writer == null){
            return null;
        }
        return writer.toString();
    }
    // reformat a String into pretty json format
    public void write(String jsonStr){
        StringReader stringReader = new StringReader(jsonStr);
        json = Json.createReader(stringReader).read();
        if (json instanceof JsonObject){
            writeObject((JsonObject)json,0);
        } else if (json instanceof JsonArray){
            writeArray((JsonArray)json,0);
        }
    }
    // output the json value in pretty 
    public void write(JsonValue jsonValue,int indent){
        
        if (jsonValue instanceof JsonArray){
            writeArray((JsonArray)jsonValue,indent);
        } else if (jsonValue instanceof JsonObject){
            writeObject((JsonObject)jsonValue,indent);
        } else if (jsonValue instanceof JsonString){
            writeString((JsonString)jsonValue);
        } else if (jsonValue instanceof JsonNumber){
            writeNumber((JsonNumber)jsonValue);
        } else if (jsonValue == JsonValue.TRUE){
            builder.append("true");
        } else if (jsonValue == JsonValue.FALSE){
            builder.append("false");
        }
    }
 
    public void writeString(JsonString jsonString){
        builder.append('"');
        builder.append(jsonString.getString());
        builder.append('"');
    }
    public void writeNumber(JsonNumber jsonNumber){
        builder.append(Double.toString(jsonNumber.doubleValue()));
    }


    public void writeObject(JsonObject jsonObject,int indent){
        writeIndent(indent);
        builder.append("{");
        int n = jsonObject.size();
        int i=0;
        for (String key : jsonObject.keySet()){
            ++i;
            JsonValue jsonValue = jsonObject.get(key);
            builder.append('"');
            builder.append(key);
            builder.append('"');
            builder.append(':');
            if (jsonValue instanceof JsonStructure){
                builder.append("\n");
                write(jsonValue,indent+inc);
            }else {
                write(jsonValue,0);

            }
            if (i != n){
                builder.append(",");
            }
        }
        builder.append("}");        
    }
    public void writeArray(JsonArray jsonArray,int indent){
        writeIndent(indent);
        builder.append("[\n");
        for (int i=0 ; i<jsonArray.size() ; ++i){
            JsonValue jsonValue = jsonArray.get(i);
            if (!(jsonValue instanceof JsonStructure)){
                writeIndent(indent);
            }
            write(jsonValue,indent);
            if (i+1 != jsonArray.size()){
                builder.append(",");
            }
            builder.append("\n");
        }
        writeIndent(indent);
        builder.append("]\n");
    }

    private void writeIndent(int indent){
        for (int i=0 ; i<indent ; ++i){
            builder.append(' ');
        }
    }
    public JsonObject getJson(){
        return (JsonObject)this.json;
    }
    static int inc = 4;
    StringWriter writer;
    PrintWriter builder;
    JsonStructure json;
}
