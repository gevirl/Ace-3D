/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.Set;


/**
 *
 * @author gevirl
 */
public interface DataSource {
    public int getN();  // the number of data items
    public int getD();  // the number of dimensions
    public Object get(long i);  // return the ith data item
    public Set<Object> sample(int m);
    public long[] getDims();
}
