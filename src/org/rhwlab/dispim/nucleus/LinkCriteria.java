/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
// any specific criteria that is null, means that criteria is not to be used 
public class LinkCriteria {

    static public void init() {
        descriptions = new TreeMap<>();
        descriptions.put("timeLimit","Upper time for which the criteria are used");
        descriptions.put("timeThreshold","Minimum time possible between divsions");
        descriptions.put("childEccentricty","Minimum eccentricty of the child of a division");
        descriptions.put("parentEccentricity","Minimum eccentricty of the parent of a divsion");
        descriptions.put("divisionDistance","Maximum distance between two daughters of a division");
        descriptions.put("cosineThreshold","");
        descriptions.put("volumeRatioThreshold","Maximum possible volume ratio between daughters of a divsion");
        descriptions.put("legRatioThreshold","Maximum ratio of parent to daughter distances ");
        descriptions.put("intensityRatioThreshold","Maximum ratio of parent to daughter intensities");
        descriptions.put("","");
        
        
        
        criteriaByTime = new HashMap<>();
        LinkCriteria c = new LinkCriteria();
        c.timeLimit = 1000;
        c.timeThresh = 10;
        c.childEccThresh = 0.6;
        c.parentEccThresh = 0.6;
        c.divDistanceThresh = 70.0;
        c.cosThresh = .8;
        c.volumeThresh = 4.0;
        c.legRatio = 10.0;
        c.intensityThresh = 5.0;
        criteriaByTime.put(c.timeLimit,c);
        
    } 
    public Element toXML(){
        Element ret = new Element("LinkCriteria");       
        for (Entry entry : this.criteria.entrySet()){
            ret.setAttribute( entry.getKey().toString() , entry.getValue().toString() );
        }
        return ret;
    }
    
    public Integer getTimeThresh(){
        return this.timeThresh;
    }
    public Double getChildEccentricityThresh(){
        return this.childEccThresh;
    }
    public Double getParentEccentricityThresh(){
        return this.parentEccThresh;
    }    
    public Double getVolumeRatioThresh(){
        return this.volumeThresh;
    }
    public Double getLegRatioThresh(){
        return this.legRatio;
    }
    public Double getDivisionDistanceThresh(){
        return this.divDistanceThresh;
    }
    public Double getInensityRatioThresh(){
        return this.intensityThresh;
    }
    Integer timeLimit;  // the upper limit of time that this criteria is used
    Integer timeThresh = 10;    // minumum time difference befor a cell can divide again
    Double childEccThresh = 0.6;  // minimum eccentricty of daughters (Eccentricty is ratio
    Double parentEccThresh = 0.6;  // minimum eccentricty of parent
    Double divDistanceThresh = 70.0;  // maximum distance between the two daughters of the division
    Double cosThresh = .8;
    Double volumeThresh = 4.0;  // maximum volume ratio between the two daughters
    Double legRatio = 10.0;  // maximum ratio of the leg distances (leg = distance from parent to child)
    Double intensityThresh = 5.0;   
    TreeMap<String,Object> criteria;
    
    static HashMap<Integer,LinkCriteria> criteriaByTime;
    static TreeMap<String,String> descriptions;
}
