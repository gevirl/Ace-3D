/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.Division;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.machinelearning.Utils.Pair;

/**
 *
 * @author gevirl
 */

public class DivisionSet extends TrainingSet implements Runnable {
    
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
                    Comparable distMax = new Double(0.0);
                    for (Nucleus nuc : nucs){
                        if (nuc.isDividing()){
                            Nucleus[] next = nuc.nextNuclei();
                            Comparable[] dataArray = formDataVector("+",nuc,next);
                            if (dataArray != null){
                                addDataRecord(dataArray,prob);
                                if (dataArray[3].compareTo(distMax) > 0){
                                    distMax = dataArray[3];
                                }
                                if (dataArray[4].compareTo(distMax) > 0){
                                    distMax = dataArray[4];
                                }                                
                            }
                        }
                    }
                    System.out.printf("Distance max = %f\n",distMax);
                    // create a negative set
                    for (Nucleus source : nucs){
                        Nucleus[] children = source.nextNuclei();                
                        Set<Nucleus> region = nucFile.localRegion(source, 40.0);
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

    @Override
    public Comparable[] formDataVector(String classification, Nucleus source, Object nextobj) {
        Nucleus[] next = (Nucleus[])nextobj;
        Nucleus sourceParent = source.getParent();
        if (sourceParent == null) return null;
        
        Comparable[] data = new Comparable[labels.length];
        data[0] = classification;
        data[1] = source.getTime();
        data[2] = source.getLineage();
        data[3] = source.distance(next[0]); 
        data[4] = source.distance(next[1]); 
        data[5] = next[0].distance(next[1]);
        data[6] = source.getVolume()/next[0].getVolume();
        data[7] = source.getVolume()/next[1].getVolume();
        RealVector minorAxis = source.getAxes()[0];
        RealVector parentMinorAxis = sourceParent.getAxes()[0];
        RealVector v1 = new ArrayRealVector(next[0].getCenter());
        RealVector v2 = new ArrayRealVector(next[1].getCenter());
        RealVector divAxis = v2.subtract(v1);      
        data[8] = Division.cosineAngleBetween(minorAxis,divAxis);
        data[9] = Division.cosineAngleBetween(parentMinorAxis,divAxis);
        double[] ecc =source.eccentricity();
        double[] parentEcc = sourceParent.eccentricity();
        data[10] = ecc[0];
        data[11] = ecc[1];
        data[12] = ecc[2];
        data[13] = parentEcc[0];
        data[14] = parentEcc[1];
        data[15] = parentEcc[2];
        data[16] = source.getAvgIntensity()/next[0].getAvgIntensity();
        data[17] = source.getAvgIntensity()/next[1].getAvgIntensity();
        return data;
    }

    @Override
    public String[] getLabels() {
        return labels;
    } 

    @Override
    public void run() {
        try {
            String[] files = {"/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/pete3.xml",
                               "/net/waterston/vol9/diSPIM/20161229_hmbx-1_OP656/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170103_B0310.2_OP642/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170105_M03D4.4_OP696/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170118_sptf-1_OP722/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170125_lsl-1_OP720/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170321_unc-130_OP76/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170329_cog-1_OP541/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170405_irx-1_OP536/pete.xml",
                                "/net/waterston/vol9/diSPIM/20170411_mls-2_OP645/pete.xml"
                    };
            NamedNucleusFile[] nucFiles = new NamedNucleusFile[files.length];
            for (int i=0 ; i<files.length ; ++i){
                nucFiles[i] = TrainingSet.readNucleusFile(new File(files[i]));
                System.out.printf("Read file: %s\n",files[i]);
            }
            for (int i=4 ; i<5 ; ++i){
                DivisionSet trainingSet = new DivisionSet(i*delTime-overlap,(i+1)*delTime+overlap);
                for (int f=0 ; f<nucFiles.length ; ++f){
                    trainingSet.addNucleiFrom(nucFiles[f],50.0,0.1);
                }
                trainingSet.formDecisionTree(0);
                Element rootEle = trainingSet.toXML("Root");
                String name = trainingSet.getClass().getName();
                rootEle.setAttribute("training", name);             
                DecisionTree decisionTree = new DecisionTree(rootEle,null);
                decisionTree.reducedErrorPruning(trainingSet.getTestSet());
                decisionTree.saveAsXML(String.format("/net/waterston/vol9/diSPIM/DivisionsTree%03d.xml",delTime*(i+1)));  
            }
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }
    @Override
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = super.buildLabelsMap(labels);
        }
        return labelMap;
    }    
    static public void main(String[] args)throws Exception{
        DivisionSet ds = new DivisionSet();
        ds.run();
    }    
    
    static int overlap = 10;
    static int delTime = 50;
    static String[] labels ={"Class","Time","Lineage","Distance1","Distance2","Distance12","Volume1","Volume2","Axis","ParentAxis",
    "Ecc1","Ecc2","Ecc3","parentEcc1","parentEcc2","parentEcc3","Intensity1","Intensity2"};  
    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}