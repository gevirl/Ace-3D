/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.List;
import org.jdom2.Element;
import org.rhwlab.dispim.datasource.MicroCluster;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class NucleusLogNode extends LogNode{
    // construct from an xml element
    public NucleusLogNode(Element ele,Node par){
        this.parent = par;
        this.label = Integer.valueOf(ele.getAttributeValue("label"));
        this.lnR = Double.valueOf(ele.getAttributeValue("posterior"));
        String volStr = ele.getAttributeValue("volume");
        if (volStr != null){
            volume = Double.valueOf(volStr);
        } else {
            volume = 0.0;
        }
        String avgIntensityStr = ele.getAttributeValue("avgIntensity");
        if (avgIntensityStr != null){
            avgIntensity = Double.valueOf(avgIntensityStr);   
        }else {
            avgIntensity = 0.0;
        }
        String eccString = ele.getAttributeValue("eccentricity");
        if (eccString != null){
            String[] tokens = eccString.split(" ");
            eccentricity = new double[tokens.length];
            for (int i=0 ; i< tokens.length ; ++i){
                eccentricity[i] = Double.valueOf(tokens[i]);
            }
        }else {
            this.eccentricity = new double[0];
        }
        String centerStr = ele.getAttributeValue("center");
        if (centerStr == null){
            List<Element> children = ele.getChildren("Node");
            this.left = new NucleusLogNode(children.get(0),this);
            this.right = new NucleusLogNode(children.get(1),this);
        } else {
            this.micro = new MicroCluster(ele);
        }
    } 
    public double getVolume(){
        return volume;
    }
    
    // build a nucleus from this node
    public Nucleus getNucleus(int time){
        if (nuc == null){
            Element ele = this.formElementXML();
            if (ele == null){
                return null;
            }
            nuc = new Nucleus(new BHCNucleusData(time,ele));  // do not have a time value
        }
        return nuc;
    }


    Nucleus nuc;
    double volume;
    double[] eccentricity;
    double avgIntensity;
}
