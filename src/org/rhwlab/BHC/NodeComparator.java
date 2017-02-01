/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.Comparator;

/**
 *
 * @author gevirl
 */
public class NodeComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {

        return ((Node)o1).compareTo(o2);
    }
    
}
