/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;


import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.rhwlab.dispim.nucleus.Division;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.machinelearning.Utils.Pair;

/**
 *
 * @author gevirl
 */

public class DivisionSet extends TrainingSet  {
    
    public DivisionSet(){
        super();
    }
    
    public DivisionSet(Integer minTime,Integer maxTime){
        super(minTime,maxTime);

    }
    @Override
    public void addNucleiFrom(NamedNucleusFile nucFile, double localRegion,double prob) throws Exception {
        for (Integer time : nucFile.getAllTimes()){
            if (minTime == null || time >= minTime){
                if ((maxTime == null | time <=maxTime)){            
                    Set<Nucleus> nucs = nucFile.getNuclei(time);
                    // create the positive set
                    for (Nucleus nuc : nucs){
                        if (nuc.isDividing()){
                            Nucleus[] next = nuc.nextNuclei();
                            Comparable[] dataArray = formDataVector("+",nuc,next);
                            if (dataArray != null){
                                addDataRecord(dataArray,prob);
                                
                            }
                        }
                    }
                    
                    // create a negative set
                    for (Nucleus source : nucs){
                        Nucleus[] children = source.nextNuclei();                
                        Set<Nucleus> region = nucFile.localRegion(source, localRegion);
                        if (!region.isEmpty()){
                            int uiasdfuih=0;
                        }
                        Set<Pair> pairs = Utils.allPairsCombinations(region);
                        for (Pair pair : pairs){
                            if (!source.isDividing()){
                                Nucleus[] pairArray = pair.getAsNucleusArray();
                                Comparable[] dataArray = formDataVector("-",source,pairArray);
                                if (dataArray != null){
                                    addDataRecord(dataArray,prob);
                                }
                            } else {
                                //make sure we are not adding the actual division as a negative
                                Nucleus[] pairNucs = pair.getAsNucleusArray();
                                if (children[0].equals(pairNucs[0]) && children[1].equals(pairNucs[1])) continue;
                                if (children[0].equals(pairNucs[1]) && children[1].equals(pairNucs[0])) continue;
                                Comparable[] dataArray = formDataVector("-",source,pairNucs);
                                if (dataArray != null){
                                    addDataRecord(dataArray,prob);
                                }
                            }
                        }

                    }
                }
            }
        }
    }
    static String[] labels ={"Class","Time","Lineage","DistanceRatio","Cosine","Distance12","Volume1","Volume2","Axis","ParentAxis",
    "Ecc1","Ecc2","parentEcc1","parentEcc2","Intensity1","Intensity2"};  
    @Override
    public Comparable[] formDataVector(String classification, Nucleus source, Object nextobj) {
        Nucleus[] next = (Nucleus[])nextobj;
        Nucleus sourceParent = source.getParent();
        if (sourceParent == null) return null;
        
        Comparable[] data = new Comparable[labels.length];
        int i = 0;
        data[i++] = classification;
        data[i++] = source.getTime();
        data[i++] = source.getLineage();
        double d= source.distance(next[0])/source.distance(next[1]);
        if (d <1.0) d= 1.0/d;
        data[i++] = d; 
        data[i++] = source.getDirectionCosine(next[0],next[1]); 
        data[i++] = next[0].distance(next[1]);
        data[i++] = source.getVolume()/next[0].getVolume();
        data[i++] = source.getVolume()/next[1].getVolume();
        RealVector minorAxis = source.getAxes()[0];
        RealVector parentMinorAxis = sourceParent.getAxes()[0];
        RealVector v1 = new ArrayRealVector(next[0].getCenter());
        RealVector v2 = new ArrayRealVector(next[1].getCenter());
        RealVector divAxis = v2.subtract(v1);      
        data[i++] = Division.cosineAngleBetween(minorAxis,divAxis);
        data[i++] = Division.cosineAngleBetween(parentMinorAxis,divAxis);
        double[] ecc =source.eccentricity();
        double[] parentEcc = sourceParent.eccentricity();
        data[i++] = ecc[0];
        data[i++] = ecc[1];
 //       data[i++] = ecc[2];
        data[i++] = parentEcc[0];
        data[i++] = parentEcc[1];
//        data[i++] = parentEcc[2];
        data[i++] = source.getAvgIntensity()/next[0].getAvgIntensity();
        data[i++] = source.getAvgIntensity()/next[1].getAvgIntensity();
        return data;
    }

    @Override
    public String[] getLabels() {
        return labels;
    } 


    @Override
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = super.buildLabelsMap(labels);
        }
        return labelMap;
    }    
    
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
        double[] radii = {100.0,50.0,50.0,30.0};
        String[] names = {"TimeLinkageTree","DividingNucleusTree","DivisionLinkTree","DivisionsTree"};
        int[] minCases = {20,20,20,20};
        
        NamedNucleusFile[] nucFiles = new NamedNucleusFile[files.length];
        for (int i=0 ; i<files.length ; ++i){
            nucFiles[i] = TrainingSet.readNucleusFile(new File(files[i]));
        }
        
        int delTime = 50;
        int overlap = 10;
        
        int c=3;
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
            
            String fileName = String.format("/net/waterston/vol2/home/gevirl/NetBeansProjects/Ace-3D/src/org/rhwlab/machinelearning/trees/%s.xml",names[c]);
            decisionTreeSet.saveAsXML(fileName);
        }
    }  
    
 
    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}