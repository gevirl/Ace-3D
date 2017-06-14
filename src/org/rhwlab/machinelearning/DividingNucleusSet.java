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
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class DividingNucleusSet extends TrainingSet implements Runnable {
    public DividingNucleusSet(){
        super();
    }
    
    public DividingNucleusSet(Integer minTime,Integer maxTime){
        super(minTime,maxTime);

    }
    @Override
    public void addNucleiFrom(NamedNucleusFile nucFile, double localRegion,double prob) throws Exception {
        for (Integer time : nucFile.getAllTimes()){
            if (minTime == null || time >= minTime){
                if ((maxTime == null | time <=maxTime)){            
                    Set<Nucleus> nucs = nucFile.getNuclei(time);

                    for (Nucleus nuc : nucs){
                        String cl = "-";
                        if (nuc.isDividing()){
                            cl = "+";
                        }
                        Nucleus[] next = nuc.nextNuclei();
                        Comparable[] v = formDataVector(cl,nuc,next);
                        if (v != null){
                            addDataRecord(v,prob);   
                        }
                    }
                }
            }
        }
    }
    static String[] labels ={"Class","Time","Lineage","PCVolumeRatio","GPVolumeRatio","PostDivisionTime",
    "Ecc1","Ecc2","Ecc3","parentEcc1","parentEcc2","parentEcc3","grandEcc1","grandEcc2","grandEcc3","PCIntensityRatio","GPIntensityRatio",
    "IntensityRSD","ParentRSD","GrandRSD"};    
    @Override
    public Comparable[] formDataVector(String cl, Nucleus source, Object nextObj) {
        Nucleus parent = source.getParent();
        if (parent == null) return null; // need a parent to form a data vector
        Nucleus grand = parent.getParent();
        if (grand == null) return null;
        
        Comparable[] data = new Comparable[DividingNucleusSet.labels.length];
        int i = 0;
        data[i++] = cl;  
        data[i++] = source.getTime();
        data[i++] = source.getLineage();
//        data[3] = parent.getName();
        data[i++] = parent.getVolume()/source.getVolume();
        data[i++] = grand.getVolume()/parent.getVolume();
//        data[i++] = parent.getVolume();
        int postTime = source.timeSinceDivsion();
        if (postTime == -1) postTime = source.getTime();
        data[i++] = postTime;   
        double[] ecc = source.eccentricity();
        data[i++] = ecc[0];
        data[i++] = ecc[1];
        data[i++] = ecc[2];
        double[] parentEcc = parent.eccentricity();
        data[i++] = parentEcc[0];
        data[i++] = parentEcc[1];
        data[i++] = parentEcc[2];
        double[] grandEcc = grand.eccentricity();
        data[i++] = grandEcc[0];
        data[i++] = grandEcc[1];
        data[i++] = grandEcc[2];        
        data[i++] = parent.getAvgIntensity()/source.getAvgIntensity();
        data[i++] = grand.getAvgIntensity()/parent.getAvgIntensity();
//        data[i++] = parent.getAvgIntensity();
        data[i++] = source.getIntensityRSD();
        data[i++] = parent.getIntensityRSD();
        data[i++] = grand.getIntensityRSD();
        return data;
    }

    @Override
    public String[] getLabels() {
        return DividingNucleusSet.labels;
    }
    @Override
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = super.buildLabelsMap(DividingNucleusSet.labels);
        }
        return labelMap;
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
            for (int i=0 ; i<5 ; ++i){
                DividingNucleusSet trainingSet = new DividingNucleusSet(i*delTime-overlap,(i+1)*delTime+overlap);
                for (int f=0 ; f<nucFiles.length ; ++f){
                    trainingSet.addNucleiFrom(nucFiles[f],50.0,0.3);
                }
                trainingSet.formDecisionTree(0);
                Element rootEle = trainingSet.toXML("Root");
                String name = trainingSet.getClass().getName();
                rootEle.setAttribute("training", name);             
                DecisionTree decisionTree = new DecisionTree(rootEle,null);
                decisionTree.reducedErrorPruning(trainingSet.getTestSet());
                decisionTree.saveAsXML(String.format("/net/waterston/vol9/diSPIM/DividingNucleusTree%03d.xml",delTime*(i+1)));  
            }
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }    
    static public void main(String[] args)throws Exception{
        DividingNucleusSet set = new DividingNucleusSet();
        set.run();
    }  
    int delTime = 50;
    int overlap = 10;
    

    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
