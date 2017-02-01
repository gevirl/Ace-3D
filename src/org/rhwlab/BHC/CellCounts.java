/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author gevirl
 */
public class CellCounts {
    public CellCounts()throws Exception {
        if (cellCounts == null){
            cellCounts = new ArrayList<>();
            InputStream s = this.getClass().getClassLoader().getResourceAsStream("CellsByTime.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(s));
            String line = reader.readLine();
            while(line != null){
                String[] tokens = line.split(",");
                cellCounts.add(Integer.valueOf(tokens[1]));
                line = reader.readLine();
            }
        }
    }
    public int getCellCount(int timeInMinutes){
        double t = 33.0 + (timeInMinutes-16.0)/1.1;
        return cellCounts.get((int)t - 1);
    }
    static ArrayList<Integer> cellCounts;
}
