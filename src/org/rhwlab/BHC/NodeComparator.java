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
