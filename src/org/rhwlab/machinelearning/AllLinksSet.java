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
public class AllLinksSet extends TrainingSet {
    
    public AllLinksSet(){
        super();
    }
    
    public AllLinksSet(Integer minTime,Integer maxTime){
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
                        Nucleus[] next = nuc.nextNuclei();
                        if (next.length > 0){
                            if (next.length == 1){
                                addDataRecord(formDataVector("+",nuc,next[0]),prob);
                            } else {
                                addDataRecord(formDataVector("+",nuc,next[0]),prob);
                                addDataRecord(formDataVector("+",nuc,next[1]),prob);
                            }
                        }

                    }
                    // create a negtive set
                    for (Nucleus nuc : nucs){
                        Nucleus[] next = nuc.nextNuclei();
                        if (next.length == 0) continue;  // don't use dead nuclei
                        Set<Nucleus> region = nucFile.localRegionPlusRemnants(nuc, localRegion, 0.2*nuc.getVolume());
                        for (Nucleus negNuc : region){
                            if (next.length==2 && negNuc.equals(next[1]) ) continue;
                            if (next.length>=1 && negNuc.equals(next[0])) continue;
                            addDataRecord(formDataVector("-",nuc,negNuc),prob);
                        }   
                    }
                }
            }
        }
    }

    @Override
    public Comparable[] formDataVector(String cl, Nucleus source, Object nextObj) {
        Nucleus next = (Nucleus)nextObj;
        Comparable[] data = new Comparable[labels.length];
        data[0] = cl;
        data[1] = source.getTime();
        data[2] = source.getCellName();
        data[3] = source.distance(next); 
        data[4] = source.getVolume()/next.getVolume();
        data[5] = source.getAvgIntensity()/next.getAvgIntensity();
        return data;
    }

    @Override
    public String[] getLabels() {
        return AllLinksSet.labels;
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
            AllLinksSet ts = new AllLinksSet(i*delTime-overlap,(i+1)*delTime+overlap);
            for (int f=0 ; f<nucFiles.length ; ++f){
                ts.addNucleiFrom(nucFiles[f],100.0,0.1);
            }
            ts.formDecisionTree(0);
            OutputStream stream = new FileOutputStream(String.format("/net/waterston/vol9/diSPIM/AllLinkTree%03d.xml",delTime*(i+1)));
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            Element rootEle = ts.toXML("Root");
            String name = ts.getClass().getName();
            rootEle.setAttribute("training", name);
            out.output(rootEle, stream);
            stream.close();             
            
        }
        int iusagdfugsd=0;
    }    
    static String[] labels = {"Class","Time","Cell","Distance","VolumeRatio","IntensityRatio"};    
    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
