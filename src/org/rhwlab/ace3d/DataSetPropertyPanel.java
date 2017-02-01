/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Expression;

/**
 *
 * @author gevirl
 */
public class DataSetPropertyPanel extends JPanel {
    public DataSetPropertyPanel(Ace3D_Frame frame,String dataset,int sliderMin,int sliderMax){
        this.frame = frame;
        this.dataset = dataset;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(dataset)); 
        
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridLayout(4,1));
        
        
        JPanel minPanel = new JPanel();
        minPanel.setLayout(new BoxLayout(minPanel,BoxLayout.X_AXIS));
        
        minPanel.add(new JLabel("Minimum"));
        minField = new JTextField(Integer.toString(sliderMin));
        minField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                minSlider.setValue(Integer.valueOf(minField.getText().trim()));
            }
        });        
        minPanel.add(minField);
        
        minSlider = new JSlider(sliderMin,sliderMax,sliderMin);
        minSlider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider)e.getSource();
                int v = slider.getValue();
                if (v > maxSlider.getValue()){
                    minSlider.setValue(maxSlider.getValue()-1);
                }
                if (!slider.getValueIsAdjusting()){
                    minField.setText(Integer.toString(v));
                    notifyChange();
                }
                
            }
        });
        sliderPanel.add(minPanel);
        sliderPanel.add(minSlider);
        
        JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new BoxLayout(maxPanel,BoxLayout.X_AXIS));
        
        maxPanel.add(new JLabel("Maximum"));
        maxField = new JTextField(Integer.toString(sliderMax));
        maxField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                maxSlider.setValue(Integer.valueOf(maxField.getText().trim()));
            }
        });
        maxPanel.add(maxField);
        
        maxSlider = new JSlider(sliderMin,sliderMax,sliderMax);
        maxSlider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider)e.getSource();
                int v = slider.getValue();
                if (v < minSlider.getValue()){
                    maxSlider.setValue(minSlider.getValue()+1);
                }
                if (!slider.getValueIsAdjusting()){
                    maxField.setText(Integer.toString(v));
                    notifyChange();
                }                
            }
        });
        sliderPanel.add(maxPanel);
        sliderPanel.add(maxSlider);        
        setAllEnabled(false);
                
        autoButton = new JRadioButton("Auto");
        autoButton.setSelected(false);
        this.setAllEnabled(true);
        autoButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoButton.isSelected()){
                    setAllEnabled(false);
                } else {
                    setAllEnabled(true);
                }
                notifyChange();
            }
        });
        this.add(autoButton,BorderLayout.NORTH);
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(autoButton,BorderLayout.NORTH);
        centerPanel.add(sliderPanel,BorderLayout.CENTER);
        this.add(centerPanel,BorderLayout.CENTER);
        
        colorButton = new JButton("Color");
        colorButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Color c = JColorChooser.showDialog(frame, dataset, color);
                if (c != null){
                    color = c;
                    colorButton.setBackground(c);
                    notifyChange();
                }
            }
        });

        this.add(colorButton,BorderLayout.EAST);
        
        JPanel datasetPanel = new JPanel();
        datasetPanel.setLayout(new BoxLayout(datasetPanel,BoxLayout.Y_AXIS));
        box = new JCheckBox();
        box.setSelected(true);   
        box.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean result = box.isSelected();
                notifyChange();
            }
        });
        datasetPanel.add(box);
        JButton express = new JButton("Calc Exp");
        express.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                ImagedEmbryo emb = frame.getEmbryo();
                Expression express = new Expression(emb);
                Thread thread = new Thread(express,"Expression");
                thread.start();
  //              emb.calculateExpression();
            }
        });
        datasetPanel.add(express);
        
        this.add(datasetPanel,BorderLayout.WEST);
    }
    private void setAllEnabled(boolean value){
        minSlider.setEnabled(value);
        maxSlider.setEnabled(value);
        minField.setEnabled(value);
        maxField.setEnabled(value);        
    }  
    public void notifyChange(){
        
        DataSetProperties props = getProperties();
        Ace3D_Frame.setProperties(dataset, props);
        frame.stateChanged(new ChangeEvent(this));
 
    } 
    public JCheckBox getCheckBox(){
        return box;
    }
    public DataSetProperties getProperties(){
        return new DataSetProperties(autoButton.isSelected(),(float)minSlider.getValue(),(float)maxSlider.getValue(),color,box.isSelected());
    }
    public void setProperties(DataSetProperties props){
        autoButton.setSelected(props.autoContrast);
        minSlider.setValue((int)props.min);
        maxSlider.setValue((int)props.max);
        box.setSelected(props.selected);
        color = props.color;
        colorButton.setBackground(props.color);
        this.notifyChange();
    }
    Ace3D_Frame frame;
    String dataset;
    JRadioButton autoButton;
    JSlider minSlider;
    JSlider maxSlider;
    JTextField minField;
    JTextField maxField;
    JButton colorButton;
    public Color color = Color.white;
    JCheckBox box;
}
