/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class NucleusPropertiesPanel extends JPanel implements InvalidationListener  {
    public NucleusPropertiesPanel() {
        this.setLayout(new GridLayout(19,2));
        this.add(new JLabel("Selected Nucleus"));
        this.add(name);

        this.add(new JLabel("Parent Nucleus"));
        this.add(parent);
        this.add(new JLabel("Child1 Nucleus"));
        this.add(child1);
        this.add(new JLabel("Child2 Nucleus"));
        this.add(child2);        
        this.add(new JLabel("Center"));
        this.add(center);
        this.add(new JLabel("a Radius"));
        this.add(aRadius);
        this.add(new JLabel("b Radius"));
        this.add(bRadius);
        this.add(new JLabel("c Radius"));
        this.add(cRadius);
        this.add(new JLabel("Eccentricity"));
        this.add(frob);
        this.add(new JLabel("Voxel Density"));
        this.add(density);
        this.add(new JLabel("Volume"));
        this.add(volume);
        this.add(new JLabel("Avg Intensity"));
        this.add(intensity);
        this.add(new JLabel("Intensity RSD"));
        this.add(intensityRSD); 
        this.add(new JLabel("Intensity Density"));
        this.add(intensityDensity);        
        this.add(new JLabel("In Cell"));
        this.add(cell);
        this.add(new JLabel("Root Cell"));
        this.add(root);        
        this.add(new JLabel("BHC Node"));
        this.add(sourceNode);
        this.add(new JLabel("Log Prob"));
        this.add(probability);
        cell.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                renameCell();
            }
        });
        this.add(new JLabel("Expression"));
        this.add(express);
    }

    public void renameCell(){
        Nucleus selected = embryo.selectedNucleus();
        Nucleus first = selected.firstNucleusInCell();
        TreeMap<Integer,Nucleus> desc = new TreeMap<>();
        first.descedentsInCell(desc);
        for (Nucleus d : desc.values()){
            d.setCellName(this.cell.getText().trim(),true);
        }
        ((NamedNucleusFile)embryo.getNucleusFile()).nameChildren(selected);
        embryo.notifyListeners();
    }
    @Override
    public void invalidated(Observable observable) {
        if (observable instanceof ImagedEmbryo ){
            embryo = (ImagedEmbryo)observable;
            Nucleus selected = embryo.selectedNucleus();
            if (selected == null) {
                return;
            }
            name.setText(selected.getName());
            root.setText(selected.getRoot());
            express.setText(String.format("%.2f",selected.getExpression()));
            double[] c = selected.getCenter();
            center.setText(String.format("(%d,%d,%d)",(int)c[0],(int)c[1],(int)c[2]));
            aRadius.setText(selected.getRadiusLabel(0));
            bRadius.setText(selected.getRadiusLabel(1));
            cRadius.setText(selected.getRadiusLabel(2));
            double[] ecc = selected.eccentricity();
            frob.setText(String.format("%.3f,%.3f,%.3f",ecc[0],ecc[1],ecc[2]));
            BHCNucleusData bhcNuc = (BHCNucleusData)selected.getNucleusData();
            density.setText(String.format("%f",bhcNuc.getVoxelDensity()));
            volume.setText(String.format("%f",bhcNuc.getVolume()));
            intensity.setText(String.format("%f", bhcNuc.getAverageIntensity()));
            intensityRSD.setText(String.format("%f", bhcNuc.getIntensityRSD()));
            intensityDensity.setText(String.format("%f", bhcNuc.getIntensityDensity()));
            probability.setText(Double.toString(Math.exp(bhcNuc.getPosteriorProb())));
            cell.setText(selected.getCellName());

            if (selected.getParent() != null){
                parent.setText(selected.getParent().getName());
            } else {
                parent.setText("Not linked");
            }
            Nucleus[] children = selected.nextNuclei();
            if (children.length<1 ){
                child1.setText("Not linked");
            } else {
                double dd = selected.distance(children[0]);
                child1.setText(String.format("%s(%f)",children[0].getName(),dd));
            }
            if (children.length <2){
                child2.setText("Not linked");
            } else {
                double dd = selected.distance(children[1]);
                child2.setText(String.format("%s(%f)",children[1].getName(),dd));
            } 
            sourceNode.setText(bhcNuc.getSourceNode());
        }
    }
    public String getChild1(){
        return child1.getText();
    }
    public String getChild2(){
        return child2.getText();
    }
    ImagedEmbryo embryo;
    static String initial = "None Selected";
    JLabel name = new JLabel(initial);
    JLabel root = new JLabel(initial);
    JLabel center = new JLabel(initial);
    JLabel aRadius = new JLabel(initial);
    JLabel bRadius = new JLabel(initial);
    JLabel cRadius = new JLabel(initial); 
    JLabel frob = new JLabel(initial);
    JTextField cell = new JTextField(initial);
    JLabel parent = new JLabel(initial);
    JLabel child1 = new JLabel(initial);
    JLabel child2 = new JLabel(initial);
    JLabel express = new JLabel(initial);
    JLabel density = new JLabel(initial);
    JLabel volume = new JLabel(initial);
    JLabel intensity = new JLabel(initial);
    JLabel intensityRSD = new JLabel(initial);
    JLabel intensityDensity = new JLabel(initial);
    JLabel sourceNode = new JLabel(initial);
    JLabel probability = new JLabel(initial);
}
