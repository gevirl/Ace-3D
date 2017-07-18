package org.rhwlab.machinelearning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;

/**
 *
 * @author gevirl
 */
public class DecisionTree {
    public DecisionTree(TrainingSet train,String resource)throws Exception {
        Element ele = xmlRootFromResource(resource);
        buildTree(ele,train);
    }    
    public DecisionTree(String xml,TrainingSet train)throws Exception {
        this(xmlRoot(xml),train);
    }
    public DecisionTree(Element rootEle,TrainingSet train)throws  Exception {
        buildTree(rootEle,train);
    }
    public DecisionTree(Element rootEle)throws  Exception {
        buildTree(rootEle,null);
    }    
    public void buildTree(Element rootEle,TrainingSet train)throws Exception {
        if (train == null){
            String className = rootEle.getAttributeValue("training");
            if (className != null){
                Class cl = Class.forName(className);
                trainingSet = (TrainingSet)cl.newInstance();    
            }
        } else {
            trainingSet = train;
        }
        root = new DecisionTreeNode(rootEle);
        this.labelMap = trainingSet.getLabelsAsMap();          
    }
    public Element xmlRootFromResource(String resource){
        
        InputStream stream = this.getClass().getResourceAsStream(resource);
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            Document doc = saxBuilder.build(stream);
            Element root = doc.getRootElement();
            return root;
        } catch (Exception exc){
            exc.printStackTrace();
        }
        return null;        
        
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
    // classify a data vector 
    public DecisionTreeNode classify(Comparable[] data){
        return root.classify(data, labelMap);
    }
    // return the decicion node wth the highest probability
    public DecisionTreeNode highestProbability(Comparable[] data){
        return root.highestPositiveProb(data, labelMap);
    }    
    // return the probability that the data is a positive case
    public double positiveClassification(Comparable[] data){
        return root.positiveClassificaion(data, labelMap);
    }
    public void reducedErrorPruning (File file)throws Exception {
        List<Comparable[]> data = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        line = reader.readLine();
        while (line != null){
            String[] tokens = line.split(",");
            data.add(trainingSet.formData(tokens));
            line = reader.readLine();
        }
        reader.close();
        reducedErrorPruning(data);
    }
    // prune this tree using the reduced error pruning algorithm
    public void reducedErrorPruning(List<Comparable[]> dataList){
        for (Comparable[] data : dataList){
            classify(data);
        }
        this.root.prune();
    }
    
    public Element toXML(int time) throws Exception {
        Element rootEle = root.toXML("DecisionTree");
        rootEle.setAttribute("time", Integer.toString(time));
        String name = trainingSet.getClass().getName();
        rootEle.setAttribute("training", name);
        return rootEle;
    }
    
    public void saveAsXML(String file) throws Exception {
            OutputStream stream = new FileOutputStream(file);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            Element rootEle = root.toXML("Root");
            String name = trainingSet.getClass().getName();
            rootEle.setAttribute("training", name);            
            out.output(rootEle, stream);
            stream.close();         
    }    
    public double getRootProbability(){
        return root.probability();
    }
    // test pruning
    static public void main(String[] args)throws Exception {
        String[] files = {"/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/pete3.xml",
                           "/net/waterston/vol9/diSPIM/20161229_hmbx-1_OP656/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170103_B0310.2_OP642/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170105_M03D4.4_OP696/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170118_sptf-1_OP722/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170125_lsl-1_OP720/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170321_unc-130_OP76/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170329_cog-1_OP541/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170405_irx-1_OP536/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170411_mls-2_OP645/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170509_tbx-7_OP331/pete.xml"
                };
        
        String[] trainingSetClasses = {"org.rhwlab.machinelearning.TimeLinkageSet",
                                        "org.rhwlab.machinelearning.DividingNucleusSet",
                                        "org.rhwlab.machinelearning.DivisionLinkSet",
                                        "org.rhwlab.machinelearning.DivisionSet"};
        double[] radii = {100.0,50.0,50.0,25.0};
        String[] names = {"TimeLinkageTree","DividingNucleusTree","DivisionLinkTree","DivisionsTree"};
        int[] minCases = {20,20,20,20};
        
        NamedNucleusFile[] nucFiles = new NamedNucleusFile[files.length];
        for (int i=0 ; i<files.length ; ++i){
            nucFiles[i] = TrainingSet.readNucleusFile(new File(files[i]));
        }
        
        int delTime = 50;
        int overlap = 10;
        
        for (int c = 0 ; c<trainingSetClasses.length ; ++c){
            boolean process = false;
            if (args.length == 0){
                process = true;
            } else {
                for (String arg : args){
                    if (names[c].equals(arg)){
                        process = true;
                    }
                }
            }
            if (process){
                DecisionTreeSet decisionTreeSet = new DecisionTreeSet();
                for (int i=0 ; i<6 ; ++i){
                    Constructor contruct =Class.forName(trainingSetClasses[c]).getConstructor(Integer.class,Integer.class);
                    TrainingSet trainingSet = (TrainingSet)contruct.newInstance(i * delTime - overlap, (i + 1) * delTime + overlap);
                    for (int f=0 ; f<nucFiles.length ; ++f){
                        trainingSet.addNucleiFrom(nucFiles[f],radii[c],0.3);
                    }
                    trainingSet.formDecisionTree(minCases[c]);
                    Element rootEle = trainingSet.toXML("DecisionTree");
                    String name = trainingSet.getClass().getName();
                    rootEle.setAttribute("training", name);
                    int time = delTime * (i + 1);
                    rootEle.setAttribute("time", Integer.toString(time));
                    DecisionTree decisionTree = new DecisionTree(rootEle,null);
                    decisionTree.reducedErrorPruning(trainingSet.getTestSet());
                    decisionTreeSet.addDecisionTree(time, decisionTree);
                }
                String fileName = String.format("/net/waterston/vol2/home/gevirl/NetBeansProjects/Ace-3D/src/org/rhwlab/machinelearning/trees/%s.xml",names[c]);
                decisionTreeSet.saveAsXML(fileName);
            }
        }
    }    
    DecisionTreeNode root;
    TrainingSet trainingSet;
    TreeMap<String,Integer> labelMap;
}