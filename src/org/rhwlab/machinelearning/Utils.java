package org.rhwlab.machinelearning;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class Utils {
    static public Set<Pair> allPairsCombinations(Set source){
        HashSet<Pair> ret = new HashSet<>();
        
        Object[] array = source.toArray();
        for (int i=0 ; i<array.length-1 ; ++i){
            for (int j=i+1 ; j<array.length ; ++j){
                ret.add(new Pair(array[i],array[j]));
            }
        }
        return ret;
    }

    static private boolean exclude(Object obj,Object[] excludes){
        for (int i=0 ; i<excludes.length ; ++i){
            if (obj.equals(excludes[i])){
                return true;
            }
        }
        return false;
    }

    static public class Pair{

        public Pair(Object o1,Object o2){
            this.obj1 = o1;
            this.obj2 = o2;
        }
        public Nucleus[] getAsNucleusArray(){
            Nucleus[] ret = new Nucleus[2];
            ret[0] = (Nucleus)obj1;
            ret[1] = (Nucleus)obj2;
            return ret;
        }
        Object obj1;
        Object obj2;
    }
}
    
