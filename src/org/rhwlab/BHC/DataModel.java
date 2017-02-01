/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.PrintStream;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public interface DataModel {
    public Object likelihood();
    public DataModel mergeWith(DataModel other);
    public int getN();
    public void print(PrintStream stream);
    public String asString();
    public RealVector getMean();
    public RealMatrix getPrecision();
}
