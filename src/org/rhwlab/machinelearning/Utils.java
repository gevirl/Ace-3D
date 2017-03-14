/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public class Utils {
    static public Set<Pair> allPairsCombinations(Set source){
        HashSet<Pair> ret = new HashSet<>();
        if (source.size()==2){
            ret.add(new Utils.Pair(source));
            return ret;
        }
        
        for (Object obj : source){
            
        }
        return ret;
    }


    static class Pair{
        public Pair (Set<Object> source){
            Iterator iter = source.iterator();
            this.obj1 = iter.next();
            this.obj2 = iter.next();
        }
        public Pair(Object o1,Object o2){
            this.obj1 = o1;
            this.obj2 = o2;
        }
        Object obj1;
        Object obj2;
    }
}
    
