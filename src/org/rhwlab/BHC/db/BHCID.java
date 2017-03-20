/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC.db;

import javax.swing.event.ChangeEvent;
import org.rhwlab.LMS.TextCell;

/**
 *
 * @author gevirl
 */
public class BHCID extends TextCell {
    public BHCID(){
        super();
        inputs.put("Name",null);
        inputs.put("Time",null);
        inputs.put("SegProb",null);
    }
    public void stateChanged(ChangeEvent event){
        String name = inputs.get("Name").getValueAsString();
        String time = inputs.get("Time").getValueAsString();
        String segProb = inputs.get("SegProb").getValueAsString();
        
        if (name.equals("")||time.equals("")||segProb.equals("")) return;
        
        this.setValue(String.format("%s_%s_%s",name,time,segProb));
    }
}
