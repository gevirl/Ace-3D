package org.rhwlab.machinelearning;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class DecisionTreeNode {
    int positive;
    int total;
    double prob;
    double highestProb;
    String variable;
    Comparable value;
    DecisionTreeNode less;
    DecisionTreeNode greater;
    int testPos;
    int testTotal;
    
    public DecisionTreeNode(Element ele) {
        this.prob = Double.valueOf(ele.getAttributeValue("positiveProb"));
        this.positive = Integer.valueOf(ele.getAttributeValue("positive"));
        this.total = Integer.valueOf(ele.getAttributeValue("total"));
        this.variable = ele.getAttributeValue("variable");
        if (this.variable != null){
            String type = ele.getAttributeValue("class");
            try {
                Class c = Class.forName(type);
                Constructor cons = c.getConstructor(Class.forName("java.lang.String"));
                value = (Comparable)cons.newInstance(ele.getAttributeValue("value"));

                less = new DecisionTreeNode(ele.getChild("LessOrEqual"));
                greater = new DecisionTreeNode(ele.getChild("Greater"));
            } catch (Exception exc){
                exc.printStackTrace();
            }
        }
    }
    public Element toXML(String nodeType){
        Element ele = new Element(nodeType);
        double p = (double)positive/(double)total;
        ele.setAttribute("positiveProb",Double.toString(p));
        ele.setAttribute("positive", Integer.toString(positive));
        ele.setAttribute("total", Integer.toString(total));
        
        if (variable != null){
            ele.setAttribute("variable",variable);
            ele.setAttribute("value", value.toString());
            ele.setAttribute("class", value.getClass().getName());
            ele.addContent(less.toXML("LessOrEqual"));
            ele.addContent(greater.toXML("Greater"));
        }  
        return ele;
    }
    public DecisionTreeNode highestPositiveProb(Comparable[] data,Map<String,Integer> labelIndexes){
        double p = this.probability();
        if (variable == null){
            return this;  // the node is a leaf
        } 
        DecisionTreeNode childP;
        Integer i = labelIndexes.get(variable);
        if (data[i].compareTo(value) <= 0 ) {
            childP = less.highestPositiveProb(data, labelIndexes);
        } else {
            childP = greater.highestPositiveProb(data, labelIndexes);
        }
        if (childP.probability() > p){
            return childP;
        }
        return this;
    }
    public DecisionTreeNode classify(Comparable[] data,Map<String,Integer> labelIndexes){
        String cl = data[0].toString();
        if (cl.equals("+")){
            ++testPos;
        }
        ++testTotal;
        
        if (variable == null){
            highestProb = this.probability();
            return this;  // the node is a leaf
        }
        
        Integer i = labelIndexes.get(variable);
        if (data[i].compareTo(value) <= 0 ) {
            DecisionTreeNode ret = less.classify(data, labelIndexes);
            if (this.probability() > ret.getHighest()){
                ret.highestProb = this.probability();
                
            }
            return ret;
        } else {
            DecisionTreeNode ret= greater.classify(data, labelIndexes);
            if (this.probability() > ret.getHighest()){
                ret.highestProb = this.probability();
                
            } 
            return ret;
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
    // prune this node/subtree - returns the misclassification count after pruning
    public int prune(){
        if (this.isLeaf()){
            if (getClassification().equals("+")){
                return this.testTotal-this.testPos;  // negatives are misclassifed
            }
            return this.testPos;  // positives are misclassified
        } else {
            // not a leaf - prune the children
            int misClass = this.less.prune() + this.greater.prune();  // total of misclassification from children
            if (misClass < Math.min(testPos,testTotal-testPos)){
                return misClass;  // do not prune this node - this children are doing a good job of classifiying
            } else {
                // make this node a leaf
                variable = null;
                if (getClassification().equals("+")){
                    return this.testTotal-this.testPos;
                } else {
                    return this.testPos;
                }
            }
        }
    }
    public double probability(){
        return prob;
    }
    public int getTotal(){
        return total;
    }
    public int getPositive(){
        return positive;
    }
    public String getClassification(){
        if (prob > 0.5){
            return "+";
        }
        return "-";
    }
    public void clearCounts(){
        testPos = 0;
        testTotal = 0;
        if (less != null){
            less.clearCounts();
        }
        if (greater != null){
            greater.clearCounts();
        }
    }
    public boolean isLeaf(){
        return variable == null;
    }
    public double getHighest(){
        return this.highestProb;
    }

}