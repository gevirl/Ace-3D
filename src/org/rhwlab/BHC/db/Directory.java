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
public class Directory extends TextCell {
    public Directory(){
        super();
        inputs.put("Name",null);
    }

    
    @Override
    public void stateChanged(ChangeEvent event){
        String diSPIMName  = inputs.get("Name").getValueAsString();
        if (diSPIMName.equals("")) return;
        
        this.setValue(String.format("/net/waterston/vol9/diSPIM/%s/BHC",diSPIMName));
    }
}
