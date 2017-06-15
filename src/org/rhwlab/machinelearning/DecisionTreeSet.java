package org.rhwlab.machinelearning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author gevirl
 */
// a set of decision tree indexed by time
public class DecisionTreeSet {
    public DecisionTreeSet(){
        
    }
    // build the set from a jar resource xml file
    public DecisionTreeSet(String prefix)throws Exception {
        InputStream stream = this.getClass().getResourceAsStream(String.format("/org/rhwlab/machinelearning/trees/%s.xml",prefix));
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            Document doc = saxBuilder.build(stream);
            Element root = doc.getRootElement();
            List<Element> trees = root.getChildren("DecisionTree");
            for (Element tree : trees){
                DecisionTree decisionTree = new DecisionTree(tree);
                int time = Integer.valueOf(tree.getAttributeValue("time"));
                map.put(time, decisionTree);
            }
        } catch (Exception exc){
            exc.printStackTrace();
        } 
        stream.close();
    }
    public DecisionTreeSet(String prefix,File directory,TrainingSet training) throws Exception {
        Pattern pattern = Pattern.compile(String.format("%s(\\d{3}).xml",prefix));
        
        File[] files = directory.listFiles();
        for (File file : files){
            String name = file.getName();
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()){
                int index = Integer.valueOf(matcher.group(1));
                DecisionTree tree = new DecisionTree(file.getPath(),training);
                map.put(index, tree);
            }
        }
    }
    public void addDecisionTree(int time,DecisionTree tree){
        map.put(time,tree);
    }
    public DecisionTreeNode classify(int time,Comparable[] data){
        return getTree(time).classify(data);
    }
    public DecisionTreeNode highestProbability(int time,Comparable[] data){
        return getTree(time).highestProbability(data);
    }    
    public void saveAsXML(String file) throws Exception {
        OutputStream stream = new FileOutputStream(file);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        Element rootEle = new Element("DecisionTreeSet");
        for (Integer time : map.keySet()){
            Element treeEle = map.get(time).toXML(time);
            rootEle.addContent(treeEle);
        }
        out.output(rootEle, stream);
        stream.close();         
    }  
   
    public DecisionTree getTree(int time){
        Entry<Integer,DecisionTree> entry = map.ceilingEntry(time);
        if (entry == null) {
            entry = map.lastEntry();
        }
        return entry.getValue();
    }
    public static void main(String[] args) {
    }
    TreeMap<Integer,DecisionTree> map = new TreeMap<>();
}
