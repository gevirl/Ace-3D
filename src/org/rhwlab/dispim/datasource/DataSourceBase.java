/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public abstract class DataSourceBase implements DataSource {
    // return coordinates of a voxel given a linear index 
    public long[] getCoords(long i){
        long[] coord = new long[dims.length];
        long index = i;
        for (int d =0 ; d<dims.length-1 ; ++d){
            coord[d] = index % dims[d];
            index = index - coord[d];
            index = index/dims[d];
        }
        coord[dims.length-1] = index;        
        return coord;    
    }


    @Override
    public int getN() {
        return (int)N;
    }
    
    @Override
    public int getD() {
        return dims.length;
    }
    
    // sample the data source
    // draw a random set of size m from the data source
    // using Floyd's Algorithm
    @Override
    public Set<Object> sample(int m){
        Random rnd = new Random();
        HashSet<Object> ret = new HashSet<>();
        for (long i=N-m ; i<N ; i++ ){
            long pos = (long)(rnd.nextDouble()*N);
            Object obj = get(pos);
            if (ret.contains(obj)){
                ret.add(get(i));
            } else {
                ret.add(obj);
            }
        }
        return ret;
    }
    
    public long[] getDims(){
        return dims;
    }
    long[] dims; 
    long N;
}
