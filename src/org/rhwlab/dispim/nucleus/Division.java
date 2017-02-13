/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class Division implements Comparable {
    public Division(Nucleus parent,Nucleus child1,Nucleus child2){
        this(parent,child1,parent.distance(child1),child2,parent.distance(child2));
    }
    public Division(Nucleus parent,Nucleus child1,double d1,Nucleus child2,double d2){
        this.parent = parent;
        this.child1 = child1;
        this.child2 = child2;
        this.dist1 = d1;
        this.dist2 = d2;
        dist =  dist1 + dist2;
        score = null;
        init();
    } 

    public boolean isDistancePossible(){
        
        if (dist > divDistanceThresh){
            System.out.printf("Distance: %f\n",dist);
            return false;  // daughters are to far from parent
        }  
        return true        ;
    }
    public boolean isPossible(){
        return score != null;
    }
    // determine if this is a possible division
    public boolean init(){ 
        if (parent.getName().equals("154_10490")&&child1.getName().equals("155_19154")&&child2.getName().equals("155_17630")){
            int asuhfusd=0;
        }
        
        boolean debug = true;
        if (debug) System.out.printf("%s - %s,%s\n",this.parent.getName(),this.child1.getName(),this.child2.getName());
        
        if (dist1 >parentToChildDistance || dist2 > parentToChildDistance){
            if (debug) System.out.printf("parent to children: %f  %f\n",dist1,dist2);
            return false;  // children too far from parent            
        }
        
        if (parent.getCellName().contains("Polar") || parent.getCellName().contains("polar")) {
            if (debug) System.out.println("Polar parent");
            return false;  // polar bodies do not divide
        }
        if (child1.getCellName().contains("Polar") || child1.getCellName().contains("polar")) {
            if (debug) System.out.println("Polar child1");
            return false;  // polar bodies do not divide
        }     
        if (child2.getCellName().contains("Polar") || child2.getCellName().contains("polar")) {
            if (debug) System.out.println("Polar child2");
            return false;  // polar bodies do not divide
        }           
        double legDistRatio = dist1/dist2;
        if (legDistRatio < 1.0){
            legDistRatio = 1.0/legDistRatio;
        }
        if (legDistRatio >legRatio){
            if (debug) System.out.printf("Leg Ratio: %f\n", legDistRatio);
            return false;
        }
        
        double childDist = child1.distance(child2);
        if (childDist> divDistanceThresh){  // are children too far apart?
            if (debug) System.out.printf("Child distnce: %f\n",childDist);
            return false;
        }
        
        if (parent.getTime()>=65 && !parent.between(child1, child2)){
            if (debug) System.out.println("parent not between");
            return false;            
        }
        
        // check the divison axis compared to the parents shortest axis
        RealVector[] parentAxes = parent.getAxes();
        RealVector v1 = new ArrayRealVector(child1.getCenter());
        RealVector v2 = new ArrayRealVector(child2.getCenter());
        divAxis = v2.subtract(v1);
        if (!related(parentAxes[0],divAxis,debug)){
            // parent failed - try parents parent minor axis
            Nucleus parentParent = parent.getParent();
            if (parentParent != null){
                RealVector[] parParAxes = parentParent.getAxes();
                if (!related(parParAxes[0],divAxis,debug)){
                    if (debug) System.out.println("Not related ");
                    return false;                     
                }
            } else{
                if (debug) System.out.println("Not related - parent parent null");
                return false;   
            }
        }
        
        // children should have about the same volume
        double volRatio = ((BHCNucleusData)child1.getNucleusData()).getVolume()/((BHCNucleusData)child2.getNucleusData()).getVolume();
        if (volRatio < 1.0) {
            volRatio = 1.0/volRatio;
        }
        if (volRatio > volumeThresh){
            if (debug) System.out.printf("Volume ratio %s\n",volRatio);
            return false;
        }
        
        // children and parents should be about the same intensity
        double intensityRatio = ((BHCNucleusData)parent.getNucleusData()).getAverageIntensity()/((BHCNucleusData)child1.getNucleusData()).getAverageIntensity();
        if (intensityRatio < 1.0){
            intensityRatio = 1.0/intensityRatio;
        }
        if (intensityRatio > intensityThresh){
            if (debug) System.out.println("Intensity child1");
            return false;
        }
        intensityRatio = ((BHCNucleusData)parent.getNucleusData()).getAverageIntensity()/((BHCNucleusData)child2.getNucleusData()).getAverageIntensity();
        if (intensityRatio < 1.0){
            intensityRatio = 1.0/intensityRatio;
        }
        if (intensityRatio > intensityThresh){
            if (debug) System.out.println("Intensity child2");
            return false;
        }        
        int lastDiv = parent.timeSinceDivsion();
        if ( lastDiv != -1 && lastDiv < timeThresh){
            if (debug) System.out.println("Time");
            return false;  // lifetime of cell is too short for another division
        }
        
        // parent and children should be eccentric
        double[] ecc = parent.eccentricity();
        if (ecc[1] < parentEccThresh){
            if (parent.getParent() == null){
                if (debug) System.out.println("Parent Eccentricity");
                return false;
            }
            double[] parEcc = parent.getParent().eccentricity();
            if (parEcc[1] < parentEccThresh){
                if (debug) System.out.println("Parent Eccentricity");
                return false;
            }
        }
        ecc = child1.eccentricity();
        if (ecc[1] < eccThresh){
            if (debug) System.out.println("Child1 Eccentricity");
            return false;  // nuclei are not eccentric enough            
        }        
        ecc = child2.eccentricity();
        if (ecc[1] < eccThresh){
            if (debug) System.out.println("Child2 Eccentricity");
            return false;  // nuclei are not eccentric enough            
        }
        
        // children should not intersect in a possible divison
        if (Nucleus.intersect(child1, child2)){
            if (debug) System.out.println("Daughters intersect");
            return false;  // nuclei are not eccentric enough                
        }
/*        
        RealVector[] parentAxes = parent.getAxes();
        RealVector[] child1Axes = child1.getAxes();
        RealVector[] child2Axes = child2.getAxes();
        // the children's major axes must be close
        if (!related(child1Axes[0],child2Axes[0])){
            if (debug) System.out.println("Axes");
            return false;
        }
        
        // one of the  major axes of parent must be close to the major axis of both children
        if (!( (related(parentAxes[1],child1Axes[2])&&related(parentAxes[1],child2Axes[2])) ||
                (related(parentAxes[2],child1Axes[2])&&related(parentAxes[2],child2Axes[2])) )){
            return false;
        }
 */     
        if (debug) System.out.printf("Accepted: distance=%f\n",dist);
        RealVector[] child1Axes = child1.getAxes();
        RealVector[] child2Axes = child2.getAxes();
        double cos1 = cosineAngleBetween(child1Axes[0],divAxis);
        double cos2 = cosineAngleBetween(child2Axes[0],divAxis);
        score = divAxis.getNorm() + 4.0*(2.0-cos1-cos2) + 3.0*volRatio;
        
        return true;
    }
    // are two axis close enough to be a division
    private boolean related(RealVector axis1,RealVector axis2,boolean debug){
        double cos = cosineAngleBetween(axis1,axis2);
        boolean ret =  cos >= cosThresh ;
        if (!ret){
            if (debug) System.out.printf("Cosine: %f\n",cos);
            
        }
        return ret;
    }
    private static double cosineAngleBetween(RealVector v1,RealVector v2){
        return Math.abs(v1.unitVector().dotProduct(v2.unitVector()));
    }
    public double getDistance(){
        return dist;
    }

    // determine if a set of divisions is consistent
    // ie - can all exist at the same time
    // it is assumed that the parents are already unique in the set
    static public boolean isConsistent(Set<Division> divisions){
        // to be consistent, no two parents go to the same child
        Set<Nucleus> unique = new HashSet<>();
        for (Division div : divisions){
            unique.add(div.child1);
            unique.add(div.child2);        
        }
        return unique.size() == 2*divisions.size();
    }
    // make all possible divisions from a given nuclues
    static public Set<Division> possibleDivisions(Nucleus from,Nucleus[] to){
 
        ArrayList<NucleusPair> pairList = new ArrayList<>();
        for (Nucleus nuc : to){
            if (nuc != null){
                pairList.add(new NucleusPair(from,nuc));
            }
        }
        Collections.sort(pairList);
        
        HashSet<Division> ret = new HashSet<>();
        for (int i=0 ; i<pairList.size()-1 ; ++i){
            NucleusPair pair1 = pairList.get(i);
            Nucleus toNuc1 = pair1.nuc2;
            for (int j=i+1 ; j<pairList.size() ; ++j){
                NucleusPair pair2 = pairList.get(j);
                Nucleus toNuc2 = pair2.nuc2;
                Division div = new Division(from,toNuc1,pair1.shapeDistance,toNuc2,pair2.shapeDistance);
                System.out.printf("P:%s  C1:%s   C2:%s  ",from.getName(),toNuc1.getName(),toNuc2.getName());
                if (div.isDistancePossible()){
                    if (div.isPossible()){
                        ret.add(div);
                    }
                } else {
                    break;
                }
            }
        }
        return ret;
    }
    
    // determine which nuclei could be a parent in a divison
    static public List<Nucleus> possibleParents(List<Nucleus> nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (possibleParent(nuc)){
                ret.add(nuc);
            }
        }
        return ret;
    }
    static public boolean possibleParent(Nucleus nuc){
        double[] ecc = nuc.eccentricity();
        if (nuc.getTime() < 50) {
            return true;
        }
        return ecc[1]>eccThresh;
    }
    // decide on best divisions
    static public HashMap<Nucleus,Division> bestDivisions(List<Nucleus> fromList,List<Nucleus> toList){
        
        List<Nucleus> possibleParentList = possibleParents(fromList);
        
        // determine all possible divisions from each possible parent
        HashMap<Nucleus,Set<Division>> possibleFrom = new HashMap<>(); 
        for (Nucleus possibleParent : possibleParentList){
            Set<Division> possibleDivs = possibleDivisions(possibleParent,toList.toArray(new Nucleus[0]));
            if (!possibleDivs.isEmpty()){
                possibleFrom.put(possibleParent, possibleDivs);
            }
        }
        
        // determine all the divsions ending on each 'to' nucleus
        HashMap<Nucleus,Set<Division>> possibleTo = new HashMap<>();
        for (Nucleus fromNuc : possibleFrom.keySet()){
            Set<Division> divs = possibleFrom.get(fromNuc);
            for (Division div : divs){
                Nucleus to1 = div.child1;
                Nucleus to2 = div.child2;
                
                Set<Division> toDivs = possibleTo.get(to1);
                if (toDivs == null){
                    toDivs = new HashSet<>();
                    possibleTo.put(to1, toDivs);
                }
                toDivs.add(div);
                
                toDivs = possibleTo.get(to2);
                if (toDivs == null){
                    toDivs = new HashSet<>();
                    possibleTo.put(to2, toDivs);
                }
                toDivs.add(div);                
            }
        }
        // use the 'best' division for each 'to' nucleus
        HashMap<Nucleus,Division> ret = new HashMap<>();
        while (!possibleTo.isEmpty()){
            Nucleus first = possibleTo.keySet().iterator().next();
            Set<Division> divs = possibleTo.get(first);
            Division best = bestDivision(divs);
            ret.put(first,best);
            
            // remove any divisions that are no longer possible
            possibleTo.remove(best.child1);
            possibleTo.remove(best.child2);
        }
        

        return ret;
    }
    static private Division bestDivision(Set<Division> divs){
        Division ret = null;
        double minD = Double.MAX_VALUE;
        for (Division div : divs){
            double d = div.getDistance();
            if ( d < minD){
                minD = d;
                ret = div;
            }
        }
        return ret;
    }
    @Override
    public int compareTo(Object o) {
        Division otherDiv = (Division)o;
        return this.score.compareTo(otherDiv.score);
    }
    
    Nucleus parent;
    Nucleus child1;
    Nucleus child2;
    double dist;  // sum of dist1 + dist2  
    double dist1;  //distance from parent to child1
    double dist2;  //distance from parent to child2
    RealVector divAxis;  // division axis
    Double score;  // will be null if not a possible division
    
    static int timeThresh = 10;
    static double eccThresh = 0.5;
    static double parentEccThresh = .86;
    static double divDistanceThresh = 60.0;
//    static double parentToChildDistance = 27;
    static double parentToChildDistance = 39; // 30
    static double cosThresh = .8;
    static double volumeThresh = 3.0;
    static double legRatio = 12.0;
    static double intensityThresh = 5.0;  // ratio of average intensity 


}
