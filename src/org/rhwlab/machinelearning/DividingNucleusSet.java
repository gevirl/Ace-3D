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
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class DividingNucleusSet extends TrainingSet {
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

    @Override
    public Comparable[] formDataVector(String cl, Nucleus source, Object nextObj) {
        Nucleus parent = source.getParent();
        if (parent == null) return null; // need a parent to form a data vector
        
        Comparable[] data = new Comparable[DividingNucleusSet.labels.length];
        data[0] = cl;  
        data[1] = source.getTime();
        data[2] = source.getCellName();
        data[3] = parent.getName();
        data[4] = source.getVolume();
        data[5] = parent.getVolume();
        int postTime = source.timeSinceDivsion();
        if (postTime == -1) postTime = source.getTime();
        data[6] = postTime;   
        double[] ecc = source.eccentricity();
        data[7] = ecc[0];
        data[8] = ecc[1];
        data[9] = ecc[2];
        double[] parentEcc = parent.eccentricity();
        data[10] = parentEcc[0];
        data[11] = parentEcc[1];
        data[12] = parentEcc[2];
        data[13] = source.getAvgIntensity();
        data[14] = parent.getAvgIntensity();
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
            System.out.printf("Read file: %s\n",files[i]);
        }
        
        int delTime = 50;
        int overlap = 10;
        for (int i=0 ; i<5 ; ++i){
            DividingNucleusSet ts = new DividingNucleusSet(i*delTime-overlap,(i+1)*delTime+overlap);
            for (int f=0 ; f<nucFiles.length ; ++f){
                ts.addNucleiFrom(nucFiles[f],50.0,0.1);
                System.out.printf("Added nuclei from %s\n",files[f]);
            }
            System.out.println("Forming tree");
            ts.formDecisionTree(0);
            OutputStream stream = new FileOutputStream(String.format("/net/waterston/vol9/diSPIM/DividingNucleusTree%03d.xml",delTime*(i+1)));
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(ts.toXML("Root"), stream);
            stream.close();             
            
        } 
    }    
    static String[] labels ={"Class","Time","Cell","Parent","Volume","ParentVolume","PostDivisionTime",
    "Ecc1","Ecc2","Ecc3","parentEcc1","parentEcc2","parentEcc3","Intensity","ParentIntensity"};    
    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
