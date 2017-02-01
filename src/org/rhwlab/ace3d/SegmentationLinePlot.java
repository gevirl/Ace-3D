/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.BHC.NucleusLogNode;

/**
 *
 * @author gevirl
 */
public class SegmentationLinePlot extends JPanel {
    public void setTree(BHCTree tree){
        XYSeriesCollection collect = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        collect.addSeries(series);
        
        TreeMap<Integer,TreeSet<NucleusLogNode>> map =tree.allTreeCuts(500);
        
        for (Integer i : map.keySet()){
            TreeSet<NucleusLogNode> nodes = map.get(i);
            double lnP = nodes.first().getLogPosterior();
            series.add((double)i,Math.exp(lnP));

        }
        int t = tree.getTime();
        int nu = tree.getNu();

        JFreeChart chart = ChartFactory.createXYLineChart
                (String.format("Time=%d,nu=%d,alpha=%e",tree.getTime(),tree.getNu(),tree.getAlpha()),
                        "Index","Probability", collect, PlotOrientation.VERTICAL,false,true,true);
        XYPlot plot = (XYPlot)chart.getPlot();

        ChartPanel panel = new ChartPanel(chart);
        this.add(panel);        
    }    
}
