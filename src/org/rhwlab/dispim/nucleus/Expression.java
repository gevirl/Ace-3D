/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.List;
import java.util.Set;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.dispim.ImagedEmbryo;

/**
 *
 * @author gevirl
 */
// calculate expression on all nuclei
public class Expression implements Runnable {
    public Expression(ImagedEmbryo embryo){
        this.embryo = embryo;
    }
    @Override
    public void run() {
        List<String> datasets = Ace3D_Frame.datasetsSelected();
        if (!datasets.isEmpty()){
            Set<Integer> times = embryo.getNucleusFile().getAllTimes();
            for (Integer time : times){
                embryo.calculateExpression(datasets.get(0), time);
                System.out.printf("Expression completed for time: %d\n",time);
            }
        }        
    }
    ImagedEmbryo embryo;
}
