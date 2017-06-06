/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.io.File;
import java.io.FileOutputStream;

import java.io.OutputStream;
import java.io.PrintStream;
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
 **/

public class TimeLinkageSet extends TrainingSet {
    
    public TimeLinkageSet(){
        super();
    }
    
    public TimeLinkageSet(Integer minTime,Integer maxTime){
        super(minTime,maxTime);

    }
    public void addNucleiFrom(NamedNucleusFile nucFile,double localRegion,double prob)throws Exception {
        
        for (Integer time : nucFile.getAllTimes()){
            if (minTime == null || time >= minTime){
                if ((maxTime == null | time <=maxTime)){
                    Set<Nucleus> nucs = nucFile.getNuclei(time);
                    // create the positive set
                    for (Nucleus nuc : nucs){
                        if (!nuc.isDividing() && !nuc.isLeaf()){
                            Nucleus[] next = nuc.nextNuclei();
                            addDataRecord(formDataVector("+",nuc,next),prob);
                        }
                    }
                    // create a negative set
                    for (Nucleus source : nucs){
                        if (!source.isLeaf()){  // source cannot be a leaf
                            Nucleus[] next = source.nextNuclei();
                            for (Nucleus nuc : nucFile.getNuclei(source.getTime()+1)){  // all the nuclei in the following time point
                                if (nuc != next[0]){   // exclude the nucleus that the source is linked to (ie the positive case)
                                    if (next.length==2 && nuc != next[1]){  // also make sure the nucleus is not part of a division from the source
                                        if (source.distance(nuc) <= localRegion){  // make sure it is ion the local region 
                                            Nucleus[] negNuc = new Nucleus[1];
                                            negNuc[0] = nuc;
                                            addDataRecord(formDataVector("-",source,negNuc),prob);
                                        }
                                    }
                                }
                            }
                            for (Nucleus remnant : nucFile.getRemnants(source.getTime(), 100)){
                                if (source.distance(remnant)<=localRegion){
                                    Nucleus[] negNuc = new Nucleus[1];
                                    negNuc[0] = remnant;
                                    addDataRecord(formDataVector("-",source,negNuc),prob);
                                }
                            }

                        }
                    }                    
                }
            }

        }
    }  
    @Override
    public Comparable[] formDataVector(String classification,Nucleus source, Object nextObj) {
        Nucleus[] next = (Nucleus[])nextObj;
        Comparable[] data = new Comparable[labels.length];
        data[0] = classification;
        data[1] = source.getTime();
        data[2] = source.getLineage();
        data[3] = source.distance(next[0]); 
        data[4] = source.getVolume()/next[0].getVolume();
        data[5] = source.getAvgIntensity()/next[0].getAvgIntensity();
        int postTime = source.timeSinceDivsion();
        if (postTime == -1) postTime = 0;
        data[6] = postTime;
        return data;
    }  
    @Override
    public String[] getLabels(){
        return labels;
    }
    @Override
    public String[] getDataClasses() {
        return TimeLinkageSet.dataClasses;
    }    
    @Override
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = super.buildLabelsMap(labels);
        }
        return labelMap;
    }
    static public void main(String[] args)throws Exception{
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
        }
        
        int delTime = 50;
        int overlap = 10;
        for (int i=0 ; i<5 ; ++i){
            TimeLinkageSet trainingSet = new TimeLinkageSet(i*delTime-overlap,(i+1)*delTime+overlap);
            for (int f=0 ; f<nucFiles.length ; ++f){
                trainingSet.addNucleiFrom(nucFiles[f],100.0,0.1);
            }
            trainingSet.formDecisionTree(0);
            Element rootEle = trainingSet.toXML("Root");
            String name = trainingSet.getClass().getName();
            rootEle.setAttribute("training", name);             
            DecisionTree decisionTree = new DecisionTree(rootEle,null);
            decisionTree.reducedErrorPruning(trainingSet.getTestSet());
            decisionTree.saveAsXML(String.format("/net/waterston/vol9/diSPIM/TimeLinkageTree%03d.xml",delTime*(i+1)));
        
        }
        int iusagdfugsd=0;
    }
    static String[] labels = {"Class","Time","Lineage","Distance","VolumeRatio","IntensityRatio","PostDivisionTime"};
    static String[] dataClasses = 
    {"java.lang.String","java.lang.Integer","java.lang.String","java.lang.Double","java.lang.Double","java.lang.Double","java.lang.Integer"};
    static TreeMap<String,Integer> labelMap;




}
