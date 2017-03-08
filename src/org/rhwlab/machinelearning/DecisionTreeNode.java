/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.lang.reflect.Constructor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class DecisionTreeNode {
    public DecisionTreeNode(String xml){
        
    }
    public DecisionTreeNode(Element ele)throws Exception {
        this.positive = Integer.valueOf(ele.getAttributeValue("positive"));
        this.total = Integer.valueOf(ele.getAttributeValue("total"));
        this.variable = ele.getAttributeValue("variable");
        if (this.variable != null){
            String type = ele.getAttributeValue("class");
            Class c = Class.forName(type);
            Constructor cons = c.getConstructor(Class.forName("java.lang.String"));
            value = (Comparable)cons.newInstance(ele.getAttributeValue("value"));
            
            less = new DecisionTreeNode(ele.getChild("Less"));
            greater = new DecisionTreeNode(ele.getChild("Greater"));
        }
    }
    static public Element xmlRoot(String file)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(file);
        Element root = doc.getRootElement();  
        return root;
    }
    
    static public void main(String[] args)throws Exception {
        DecisionTreeNode tree = new DecisionTreeNode("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/DecisionTree.xml");
    }
    String variable;
    Comparable value;
    int positive;
    int total;
    DecisionTreeNode less;
    DecisionTreeNode greater;
}