/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.io.File;
import java.util.Set;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class TimeLinkageSet {

    public void addNucleiFrom(File sessionXML)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(sessionXML);
        Element root = doc.getRootElement();     
        Element nucFileEle = root.getChild("ImagedEmbryo").getChild("Nuclei");
        NamedNucleusFile nucFile = new NamedNucleusFile();
        nucFile.fromXML(nucFileEle);  
        for (Integer time : nucFile.getAllTimes()){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            for (Nucleus nuc : nucs){
                if (!nuc.isDividing() && !nuc.isLeaf()){
                    Nucleus[] next = nuc.nextNuclei();
                    Object[] data = new Object[labels.length];
                    data[0] = time;
                    data[1] = nuc.getCellName();
                    data[2] = nuc.distance(next[0]);
                }
            }
        }
    }
    static public void main(String[] args)throws Exception{
        TimeLinkageSet ts = new TimeLinkageSet();
        ts.addNucleiFrom(new File("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/pete3.xml"));
        int iusagdfugsd=0;
    }
    String[] labels ={"Time","Cell","Distance","VolumeRatio"};
    Set<Object[]> positive;
    Set<Object[]> negative;
}
