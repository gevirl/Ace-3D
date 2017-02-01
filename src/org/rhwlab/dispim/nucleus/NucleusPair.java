/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

/**
 *
 * @author gevirl
 */
public class NucleusPair implements Comparable {
    public NucleusPair(Nucleus nuc1,Nucleus nuc2) {
        this.nuc1 = nuc1;
        this.nuc2 = nuc2;
        this.shapeDistance = nuc1.distance(nuc2);
    }

    @Override
    public int compareTo(Object o) {
        NucleusPair other = (NucleusPair)o;
        return Double.compare(shapeDistance, other.shapeDistance);
    }    
    Nucleus nuc1;
    Nucleus nuc2;
    double shapeDistance;

}
