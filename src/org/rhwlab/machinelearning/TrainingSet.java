/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
abstract public class TrainingSet {
    
    public NamedNucleusFile readNucleusFile(File sessionXML)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(sessionXML);
        Element root = doc.getRootElement();     
        Element nucFileEle = root.getChild("ImagedEmbryo").getChild("Nuclei");
        NamedNucleusFile nucFile = new NamedNucleusFile();
        nucFile.fromXML(nucFileEle); 
        return nucFile;
    }
    public void formDecisionTree(String[] labels,int depth){
        this.labels = labels;
        // find the split that creates the maximum gain in information
        ColumnGain bestGain = null;
        for (int c =1 ; c<labels.length ; ++c){
            sort(c);
            ColumnGain gain = bestSplitInRange(0,data.size());
            gain.value = data.get(gain.index)[c];
            if (bestGain == null){
                bestGain = gain;
                bestGain.column = c;
            } else if (gain.gain > bestGain.gain){
                bestGain = gain;
                bestGain.column = c;
            }
        }

        split(bestGain.column,bestGain.value);
        for (int i=0 ; i<depth ; ++i){
//            System.out.print(" ");
        }
//System.out.printf("%s,%s,%d,%d\n",labels[bestGain.column],bestGain.value.toString(),this.positiveCount,this.data.size());            
            // check if split is good enouch         
        if (bestGain.lessPos > 0 && bestGain.greatPos > 0 && bestGain.lessPos!=bestGain.lessSize && bestGain.greatPos!=bestGain.greatSize){
            lessSet.formDecisionTree(labels,depth+1);
            greaterSet.formDecisionTree(labels,depth+1);
/*            
            if (bestGain.lessPos !=0 && bestGain.lessPos != bestGain.lessSize ){
                lessSet.formDecisionTree(labels,depth+1);
            }

            if (bestGain.greatPos !=0 && bestGain.greatPos != bestGain.greatSize  ){
                greaterSet.formDecisionTree(labels,depth+1);
            }
*/
        }
      
        
    }
    // sort the data by the given column
    public void sort(int column){
        Comparator com = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Comparable[] data1 = (Comparable[])o1;
                Comparable[] data2 = (Comparable[])o2;
                return data1[column].compareTo(data2[column]);
            }
        };
        Collections.sort(data, com);
    }
    // split the data on the given column and value
    public void split(int column,Comparable value){
        splitColumn = column;
        splitValue = value;
        initSets();
        for (Comparable[] values : data){
            if (values[column].compareTo(value) > 0){
                this.greaterSet.data.add(values);
            } else {
                this.lessSet.data.add(values);
            }
        }
        delta = null;
    }
    // values must be soted by a column for this to make sense
    public ColumnGain bestSplitInRange(int start,int end){
        ColumnGain ret = new ColumnGain();
        int mid = (start + end)/2;
        
        if (end-start <= 50){
        
            double midGain = splitAtIndex(mid);            
            ret.index = mid;
            ret.gain = midGain;
            ret.lessPos = this.lessSet.positiveCount;
            ret.lessSize = this.lessSet.data.size();
            ret.greatPos = this.greaterSet.positiveCount;
            ret.greatSize = this.greaterSet.data.size();
            return ret;
        }
        
        ColumnGain leftGain = bestSplitInRange(start,mid);
        ColumnGain rightGain = bestSplitInRange(mid+1,end);
        if (leftGain.gain > rightGain.gain){
            return leftGain;
        }
        return rightGain;
    }
    // split the set at the given index
    public double splitAtIndex(int index){
        initSets();
        for (int i=0 ; i<index ; ++i){
            lessSet.data.add(data.get(i));
        }
        for (int i=index ; i<data.size() ; ++i){
            greaterSet.data.add(data.get(i));
        }
        delta = null;
        double ret = this.getDelta();
        return ret;
    }
    public void initSets(){
        if (this instanceof TimeLinkageSet){
            this.lessSet = new TimeLinkageSet();
            this.greaterSet = new TimeLinkageSet();
        } else if (this instanceof DivisionSet){
            this.lessSet = new DivisionSet();
            this.greaterSet = new DivisionSet();
        } 
       
    }
    public void countPositiveCases(){
        int count = 0;
        for (Object[] objs : data){
            String classification = (String)objs[classificationColumn];
            if (classification.equals("+")){
                ++count;
            }
        }
        this.positiveCount = count;
    }
    public double getEntropy(){
        if (entropy == null){
            int posCount = this.getPositiveCount();
            int negCount = this.getNegativeCount();
            if (posCount==0 || negCount==0){
                entropy = 0.0;
            } else {
                double total = data.size();
                double posRatio = posCount/total;
                double negRatio = negCount/total;
                entropy = -( posRatio*Math.log(posRatio) + negRatio*Math.log(negRatio) )/ln2;
            }
        }
        return entropy;
    }
    public void saveData(PrintStream stream,String[] labels){
        if (labels != null){
            stream.print("#");
            for (int i=0 ; i<labels.length ; ++i){
                stream.printf(",%s", labels[i]);
            }
            stream.println();
        }
        int j=1;
        for (Object[] objs :data){
            stream.print(j);
            for (int i=0 ; i<objs.length ; ++i){
                
                stream.printf(",%s", objs[i].toString());
            }
            stream.println();
            ++j;
        }
    }
    public int getPositiveCount(){
        if (positiveCount == null){
            this.countPositiveCases();
        }
        return this.positiveCount;
    }
    public int getNegativeCount(){
        if (positiveCount == null){
            this.countPositiveCases();
        }
        return data.size() - positiveCount;
    }
    public double getDelta(){
        if (delta == null){
            double lessCount = lessSet.data.size();
            double greaterCount = greaterSet.data.size();
            delta = getEntropy()
                    - (lessCount/data.size())*lessSet.getEntropy()
                    - (greaterCount/data.size())*greaterSet.getEntropy();
        }
        return delta;
    }
    // report results of decision tree training into xml
    public Element toXML(String nodeType){
        Element ele = new Element(nodeType);
        
        ele.setAttribute("positive", Integer.toString(this.getPositiveCount()));
        ele.setAttribute("total", Integer.toString(data.size()));
        
        if (splitValue != null){
            ele.setAttribute("variable",labels[splitColumn]);
            ele.setAttribute("value", splitValue.toString());
            ele.setAttribute("class", splitValue.getClass().getName());
            if (lessSet != null){
                ele.addContent(lessSet.toXML("Less"));
            }
            if (greaterSet != null){
                ele.addContent(greaterSet.toXML("Greater"));
            }
        }
        return ele;
    }
    
    abstract public void addNucleiFrom(File sessionXML,double localRegion)throws Exception;
    abstract public Comparable[] formDataVector(String cl,Nucleus source,Nucleus[] next);
    abstract public String[] getLabels();
    
    String[] labels;
    Integer positiveCount=null;
    Double entropy=null;
    Double delta = null;
    ArrayList<Comparable[]> data = new ArrayList<>();
    int splitColumn;
    Comparable splitValue;
    TrainingSet lessSet;
    TrainingSet greaterSet;
   
    static int classificationColumn = 0;
    static double ln2 = Math.log(2.0);
    
    public class ColumnGain{
        public int column;
        public int index;
        public Comparable value;
        public double gain;
        public int lessPos;
        public int lessSize;
        public int greatPos;
        public int greatSize;
       
    }

}