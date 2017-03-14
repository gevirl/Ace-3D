/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.lang.reflect.Constructor;
import java.util.TreeMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class DecisionTreeNode {

    public DecisionTreeNode(Element ele) {
        this.positive = Integer.valueOf(ele.getAttributeValue("positive"));
        this.total = Integer.valueOf(ele.getAttributeValue("total"));
        this.variable = ele.getAttributeValue("variable");
        if (this.variable != null){
            String type = ele.getAttributeValue("class");
            try {
                Class c = Class.forName(type);
                Constructor cons = c.getConstructor(Class.forName("java.lang.String"));
                value = (Comparable)cons.newInstance(ele.getAttributeValue("value"));

                less = new DecisionTreeNode(ele.getChild("Less"));
                greater = new DecisionTreeNode(ele.getChild("Greater"));
            } catch (Exception exc){
                exc.printStackTrace();
            }
        }
    }
    public double positiveClassificaion(Comparable[] data,TreeMap<String,Integer> labelIndexes){
        if (variable == null){
            return probability();
        }
        Integer i = labelIndexes.get(variable);
        if (data[i].compareTo(value) <= 0 ) {
            return less.positiveClassificaion(data, labelIndexes);
        } else {
            return greater.positiveClassificaion(data, labelIndexes);
        }
        
    }
    public double probability(){
        return (double)positive/(double)total;
    }

    
    int positive;
    int total;
    String variable;
    Comparable value;
    DecisionTreeNode less;
    DecisionTreeNode greater;
}