/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.HashMap;

/**
 *
 * @author gevirl
 */
// any specific criteria that is null, means that criteria is not to be used 
public class LinkCriteria {

    static public LinkCriteria getCriteria(String name){
        if (criteriaMap == null){
            init();
        }
        return criteriaMap.get(name);
    }
    static public void init() {
        criteriaMap = new HashMap<>();
        LinkCriteria c = new LinkCriteria();
        c.timeThresh = 10;
        c.childEccThresh = 0.6;
        c.parentEccThresh = 0.6;
        c.divDistanceThresh = 70.0;
        c.cosThresh = .8;
        c.volumeThresh = 4.0;
        c.legRatio = 10.0;
        c.intensityThresh = 5.0;
        criteriaMap.put("Early",c);
        
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
    // Eccentricty of an ellipse
    Integer timeThresh = 10;    // minumum time difference befor a cell can divide again
    Double childEccThresh = 0.6;  // minimum eccentricty of daughters (Eccentricty is ratio
    Double parentEccThresh = 0.6;  // minimum eccentricty of parent
    Double divDistanceThresh = 70.0;  // maximum distance between the two daughters of the division
    Double cosThresh = .8;
    Double volumeThresh = 4.0;  // maximum volume ratio between the two daughters
    Double legRatio = 10.0;  // maximum ratio of the leg distances (leg = distance from parent to child)
    Double intensityThresh = 5.0;    
    static HashMap<String,LinkCriteria> criteriaMap;
}
