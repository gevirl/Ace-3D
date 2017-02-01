/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.SelectedNucleus;

/**
 *
 * @author gevirl
 */
public class VolumeIntensityPlot extends JPanel implements ChangeListener {
    public VolumeIntensityPlot(ImagedEmbryo emb){
        this.embryo=emb;
        collect = new XYSeriesCollection();   
        series = new XYSeries("Nuclei");
        collect.addSeries(series);        
        chart = ChartFactory.createScatterPlot
                ("","Volume","AvgIntensity", collect, PlotOrientation.VERTICAL,false,true,true);
        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotHeight(4);
        renderer.setDotWidth(4);
//        plot.setRenderer(renderer);
        ChartPanel panel = new ChartPanel(chart);
        panel.addChartMouseListener(new ChartMouseListener(){
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof XYItemEntity){
                    XYItemEntity xyEntity = (XYItemEntity)entity;
                    NucleusPlotDataItem item = (NucleusPlotDataItem)series.getDataItem(xyEntity.getItem());
                    embryo.setSelectedNucleus(item.nuc);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
     
            }
        });
        this.add(panel);  
        embryo.getNucleusFile().addSelectionOberver(this);
    }
    public void setNuclei(Set<Nucleus> nucs){
        series.clear();
        for (Nucleus nuc : nucs){
            BHCNucleusData bhcNuc = (BHCNucleusData)nuc.getNucleusData();
            NucleusPlotDataItem item = new NucleusPlotDataItem(bhcNuc.getVolume(),bhcNuc.getAverageIntensity(),nuc);
            series.add(item);
        }
    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        SelectedNucleus selNuc = (SelectedNucleus)observable;
        Nucleus nuc = selNuc.getSelected();
        if (nuc == null){
            return;
        }
 //       if ((nuc.getTime() != time  && this.isVisible()) || time==-1){
            time = nuc.getTime();
            setNuclei(embryo.getNuclei(time));
            chart.setTitle(String.format("Time: %d , Nuclei: %d",time,series.getItemCount()));
//        }
    }
    
    public class NucleusPlotDataItem extends XYDataItem {
        public NucleusPlotDataItem(double x,double y,Nucleus nuc){
            super(x,y);
            this.nuc = nuc;
        }
        public Nucleus nuc;
    }
    ImagedEmbryo embryo;
    XYSeriesCollection collect;
    JFreeChart chart;
    XYSeries series;
    int time=-1;
}
