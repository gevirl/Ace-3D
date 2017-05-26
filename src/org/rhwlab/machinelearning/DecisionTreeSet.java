/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.io.File;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author gevirl
 */
// a set of decision tree indexed by time
public class DecisionTreeSet {
    public DecisionTreeSet(String prefix,File directory,TrainingSet training) throws Exception {
        Pattern pattern = Pattern.compile(String.format("%s(\\d{3}).xml",prefix));
        
        File[] files = directory.listFiles();
        for (File file : files){
            String name = file.getName();
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()){
                int index = Integer.valueOf(matcher.group(1));
                DecisionTree tree = new DecisionTree(file.getPath(),training);
                map.put(index, tree);
            }
        }
    }
    public DecisionTreeNode classify(int time,Comparable[] data){
        Entry<Integer,DecisionTree> entry = map.ceilingEntry(time);
        if (entry == null) {
            entry = map.lastEntry();
        }
        return entry.getValue().classify(data);
    }
    public DecisionTreeNode highestProbability(int time,Comparable[] data){
        Entry<Integer,DecisionTree> entry = map.ceilingEntry(time);
        if (entry == null) {
            entry = map.lastEntry();
        }
        return entry.getValue().highestProbability(data);
    }    
    public static void main(String[] args) {
    }
    TreeMap<Integer,DecisionTree> map = new TreeMap<>();
}
