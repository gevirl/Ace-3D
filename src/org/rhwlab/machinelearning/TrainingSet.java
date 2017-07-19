package org.rhwlab.machinelearning;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
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
    Integer minTime;
    Integer maxTime;
    Integer positiveCount=null;
    private ArrayList<Comparable[]> data = new ArrayList<>();
    private ArrayList<Comparable[]> validation = new ArrayList<>();
    int splitColumn;
    Comparable splitValue;
    TrainingSet lessSet;
    TrainingSet greaterSet; 
    Random rnd;
    
    public TrainingSet(){
        rnd = new Random();
    }
    public TrainingSet(Integer minTime,Integer maxTime){
        this();
        this.minTime = minTime;
        this.maxTime = maxTime;
    }    
    static public NamedNucleusFile readNucleusFile(File sessionXML)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(sessionXML);
        Element root = doc.getRootElement();     
        Element nucFileEle = root.getChild("ImagedEmbryo").getChild("Nuclei");
        NamedNucleusFile nucFile = new NamedNucleusFile();
        nucFile.fromXML(nucFileEle); 
        return nucFile;
    }
    public void addDataRecord(Comparable[] rec,double prob){
        if (prob > rnd.nextDouble()){
            validation.add(rec);
        } else {
            data.add(rec);
        }
    }
  
    public void formDecisionTree(int minCases){
        if (this.data.size() <= minCases) return;
        // find the split that creates the maximum gain in information
//        ColumnGain bestGain = null;
        this.countPositiveCases();
        Split bestSplit = null;
        String[] labels = this.getLabels();
        for (int c =1 ; c<labels.length ; ++c){
            Split split = bestSplit(c);
            if (bestSplit == null){
                bestSplit = split;
            }else {
                if (split.getEntropy() < bestSplit.getEntropy()){
                    bestSplit = split;
                }
            }
        }

        split(bestSplit.column,bestSplit.value);

//System.out.printf("%s,%s,%d,%d\n",labels[bestGain.column],bestGain.value.toString(),this.positiveCount,this.data.size());   

            // check if split is good enouch         
        if (bestSplit.lessPos > 0 && bestSplit.lessPos!=bestSplit.lessSize ){
            lessSet.formDecisionTree(minCases);
        }
        if (bestSplit.greatPos > 0 && bestSplit.greatPos!=bestSplit.greatSize){
            greaterSet.formDecisionTree(minCases);
        }        
        
    }
    public Integer nextValue(int start,Comparable value,int col){
        int ret = start + 1;
        if (ret >= data.size()) return null;  // no more values
        
        Comparable nextVal = data.get(ret)[col];
        while (value.compareTo(nextVal)==0){
            ++ret;
            if (ret >= data.size()) return null;
            nextVal = data.get(ret)[col];
        }
        return ret;
    }
    public Split bestSplit(int column){
        sort(column);
        double minE = Double.MAX_VALUE;
        Split best = null;
        int start = 0;
        Comparable currentValue = data.get(start)[column];
        Integer next = nextValue(start,currentValue,column);
        if (next == null){
            // all the data has the same value
            Split split = new Split(column,currentValue);
            split.lessPos = this.positiveCount;
            split.lessSize = this.data.size();
            split.greatPos = 0;
            split.greatSize = 0;
            return split;
        }
        Split split = new Split(column,currentValue);
        split.greatPos = this.positiveCount;
        split.greatSize = this.data.size();
        split.lessPos = 0;
        split.lessSize = 0;
        
        while (next != null){

            for (int i = start;  i<next ; ++i){
                if (isPositive(data.get(i))){
                    ++split.lessPos ;
                    --split.greatPos;
                }
                ++split.lessSize;
                --split.greatSize;
            }
            double e = split.getEntropy();
            if (e < minE){
                minE = e;
                best = split.clone();
            }
            start = next;
            currentValue = data.get(start)[column];
            split.setValue(currentValue);
            next = nextValue(start,currentValue,column);
        }
        return best;
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
    }

    public void initSets(){
        if (this instanceof TimeLinkageSet){
            this.lessSet = new TimeLinkageSet();
            this.greaterSet = new TimeLinkageSet();
        } else if (this instanceof DivisionSet){
            this.lessSet = new DivisionSet();
            this.greaterSet = new DivisionSet();
        } else if (this instanceof DividingNucleusSet){
            this.lessSet = new DividingNucleusSet();
            this.greaterSet = new DividingNucleusSet();
        } else if (this instanceof DivisionLinkSet) {
            this.lessSet = new DivisionLinkSet();
            this.greaterSet = new DivisionLinkSet();
        } else if (this instanceof AllLinksSet){
            this.lessSet = new AllLinksSet();
            this.greaterSet = new AllLinksSet();
        }
    }
    public void countPositiveCases(){
        int count = 0;
        for (Object[] objs : data){
            if (isPositive(objs)){
                ++count;
            }
        }
        this.positiveCount = count;
    }
    public boolean isPositive(Object[] objs){
        String classification = (String)objs[classificationColumn];
        return classification.equals("+");
    }

    public void saveValidationData(PrintStream stream){
        String[] labels = this.getLabels();
        if (labels != null){
            stream.printf("%s", labels[0]);
            for (int i=1 ; i<labels.length ; ++i){
                stream.printf(",%s", labels[i]);
            }
            stream.println();
        }
        for (Object[] objs : this.validation){
            stream.printf("%s", objs[0].toString());
            for (int i=1 ; i<objs.length ; ++i){
                stream.printf(",%s", objs[i].toString());
            }
            stream.println();
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

    // report results of decision tree training into xml
    public Element toXML(String nodeType){
        String[] labels = this.getLabels();
        Element ele = new Element(nodeType);
        double p = (double)this.getPositiveCount()/(double)data.size();
        ele.setAttribute("positiveProb",Double.toString(p));
        ele.setAttribute("positive", Integer.toString(this.getPositiveCount()));
        ele.setAttribute("total", Integer.toString(data.size()));
        
        if (splitValue != null){
            ele.setAttribute("entropy",Double.toString(getEntropy()));
            ele.setAttribute("gain",Double.toString(getGain()));
            ele.setAttribute("variable",labels[splitColumn]);
            ele.setAttribute("value", splitValue.toString());
            ele.setAttribute("class", splitValue.getClass().getName());
            if (lessSet != null){
                ele.addContent(lessSet.toXML("LessOrEqual"));
            }
            if (greaterSet != null){
                ele.addContent(greaterSet.toXML("Greater"));
            }
        }
        return ele;
    }
    public TreeMap<String,Integer> buildLabelsMap(String[] labels){
        TreeMap<String,Integer> labelMap = new TreeMap<>();
        for (int i=0 ; i<labels.length ; ++i){
            labelMap.put(labels[i],i);
        }
        return labelMap;
    }  
    public double getEntropy(){
        return entropy(this.getPositiveCount(),data.size());
    }
    public double getGain(){
        return this.getEntropy() - (lessSet.data.size()*lessSet.getEntropy() - greaterSet.data.size()*greaterSet.getEntropy())/(double)data.size();
    }
    static public double entropy(int posCount,int total){
        if (posCount == 0 || posCount == total) return 0.0;

        double posRatio = (double)posCount/(double)total;
        double negRatio = 1.0 - posRatio;
        return  -( posRatio*Math.log(posRatio) + negRatio*Math.log(negRatio) )/ln2;            
    }  
    public Comparable[] formData(String[] values)throws Exception{
        String[] classNames = this.getDataClasses();
        Comparable[] ret = new Comparable[classNames.length];
        for (int i=0 ; i<ret.length ; ++i){
            Class dataClass = Class.forName(classNames[i]);
            Constructor constructor = dataClass.getConstructor(String.class);
            ret[i] = (Comparable)constructor.newInstance(values[i]);
        }
        return ret;
    }
    public List<Comparable[]> getTestSet(){
        return this.validation;
    }
    
    abstract public void addNucleiFrom(NamedNucleusFile nucFile, double localRegion,double prob) throws Exception ;          
    abstract public Comparable[] formDataVector(String cl,Nucleus source,Object next);
    abstract public TreeMap<String,Integer> getLabelsAsMap();
    abstract public String[] getDataClasses();
    abstract public String[] getLabels();
    
    static int classificationColumn = 0;
    static double ln2 = Math.log(2.0);
    
    public class ColumnGain{
        public int column;
        int index;
        public Comparable value;
        public double gain;
        public int lessPos;
        public int lessSize;
        public int greatPos;
        public int greatSize;
       
    }
    public class Split {
        Double entropy;
        int column;
        Comparable value;
        public int lessPos;
        public int lessSize;
        public int greatPos;
        public int greatSize;

        public Split(int col,Comparable val){
            this.column = col;
            this.value = val;
        }
        public void setValue(Comparable val){
            this.value = val;
        }
        public Split clone(){
            Split clone = new Split(column,value);
            clone.entropy = this.entropy;
            clone.greatPos = this.greatPos;
            clone.greatSize = this.greatSize;
            clone.lessPos = this.lessPos;
            clone.lessSize = this.lessSize;
            return clone;
        }
        public Double getEntropy(){
            entropy = totalEntropy();
            return entropy;
        }
       
        private double totalEntropy(){
            double lessFraction = (double)lessSize/(double)(lessSize+greatSize);
            double greatFraction = 1.0 - lessFraction;
            return lessFraction*entropy(lessPos,lessSize) + greatFraction*entropy(greatPos,greatSize);
        }

        private double entropy(int posCount,int total){
            if (posCount == 0 || posCount == total) return 0.0;
            
            double posRatio = (double)posCount/(double)total;
            double negRatio = 1.0 - posRatio;
            return  -( posRatio*Math.log(posRatio) + negRatio*Math.log(negRatio) )/ln2;            
        }
    }

}