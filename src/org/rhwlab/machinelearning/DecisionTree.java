package org.rhwlab.machinelearning;

import java.util.TreeMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class DecisionTree {
    public DecisionTree(String xml){
        root = new DecisionTreeNode(xmlRoot(xml));
    }
    static public Element xmlRoot(String file) {
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            Document doc = saxBuilder.build(file);
            Element root = doc.getRootElement();
            return root;
        } catch (Exception exc){
            exc.printStackTrace();
        }
          
        return null;
    }   
    
    // return the probability that the data is a positive case
    public double positiveClassification(Comparable[] data,TreeMap<String,Integer> labelIndexes){
        return root.positiveClassificaion(data, labelIndexes);
    }
    
    static public void main(String[] args)throws Exception {
        DecisionTree tree = new DecisionTree("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/DecisionTree.xml");
    }    
    DecisionTreeNode root;
}