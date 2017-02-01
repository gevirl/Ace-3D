/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.Color;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class DataSetProperties {
    public DataSetProperties(){
        
    }
    public DataSetProperties (boolean auto,float mn,float mx,Color c,boolean sel){
        this.autoContrast = auto;
        this.max = mx;
        this.min = mn;
        this.color = c;
        this.selected = sel;
    }
    public DataSetProperties(Element xml){
        this.color = colorFromXML(xml.getChild("Color"));
        this.autoContrast = Boolean.valueOf(xml.getAttributeValue("Auto"));
        this.selected = Boolean.valueOf(xml.getAttributeValue("Selected"));
        this.min = Float.valueOf(xml.getAttributeValue("Min"));
        this.max = Float.valueOf(xml.getAttributeValue("Max"));
    }
    public Element toXML(){
        Element ret = new Element("DataSetProperties");
        ret.addContent(colorToXML(this.color));
        ret.setAttribute("Min", Float.toString(min));
        ret.setAttribute("Max", Float.toString(max));
        ret.setAttribute("Auto", Boolean.toString(autoContrast));
        ret.setAttribute("Selected", Boolean.toString(selected));
        return ret;
    }
    
    private Element colorToXML(Color c){
        Element ret = new Element("Color");
        ret.setAttribute("Red", Integer.toString(c.getRed()));
        ret.setAttribute("Blue", Integer.toString(c.getBlue()));
        ret.setAttribute("Green", Integer.toString(c.getGreen()));
        ret.setAttribute("Alpha", Integer.toString(c.getAlpha()));
        return ret;
    }
    private Color colorFromXML(Element xml){
        
        int red = Integer.valueOf(xml.getAttributeValue("Red"));
        int blue = Integer.valueOf(xml.getAttributeValue("Blue"));
        int green = Integer.valueOf(xml.getAttributeValue("Green"));
        int alpha = Integer.valueOf(xml.getAttributeValue("Alpha"));
        return new Color(red,green,blue,alpha);
    }
   
    public boolean autoContrast = false;
    public float min = (float)0.0;
    public float max = Short.MAX_VALUE;
    public Color color = Color.white;
    public boolean selected = true;
}
