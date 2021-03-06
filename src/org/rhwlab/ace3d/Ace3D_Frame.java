package org.rhwlab.ace3d;

import ij.ImagePlus;
import ij.io.Opener;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;
import ij.process.LUT;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.LMS.views.BHCPanel;
import org.rhwlab.LMS.views.LabMan;
import org.rhwlab.ace3d.dialogs.BHCSubmitDialog;
import org.rhwlab.ace3d.dialogs.BHCTreeCutDialog;
import org.rhwlab.ace3d.dialogs.PanelDisplay;
import org.rhwlab.db.MySql;
import org.rhwlab.dispim.HDF5DirectoryImageSource;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TifDirectoryImageSource;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.BHCDirectory;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */

// To do: 
// fix listeners
// write diagnostic for cascading false divisions
// be able to view subtrees in the tree display
// link a single nuc automatically after expanding size

public class Ace3D_Frame extends JFrame implements PlugIn,ChangeListener  {
    public  Ace3D_Frame(String[] args)  {

        try {
            if (args.length > 0 && args[0].equals("LabMan")){
                InputStream[] xml = new InputStream[2];
                xml[1]=this.getClass().getResourceAsStream("/org/rhwlab/LMS/config/BHC.xml");
                xml[0]=this.getClass().getResourceAsStream("/org/rhwlab/LMS/config/diSPIM.xml");                
                labMan = new LabMan(xml);
                labMan.setVisible(true);
            }
        } catch (Exception exc){
            exc.printStackTrace();
        }
        
        this.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e){
 //               slicePanel.requestFocusInWindow();
                Ace3D_Frame.this.toFront();
                Ace3D_Frame.this.requestFocus();
            }            
        });
        
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        imagedEmbryo = new ImagedEmbryo();
        TimePointImage.setEmbryo(imagedEmbryo);
        
        contrastDialog = new DataSetsDialog(this,0,Short.MAX_VALUE);
        contrastDialog.setVisible(true);
        
        panel = new SynchronizedMultipleSlicePanel(this,3);
        panel.setEmbryo(imagedEmbryo);
//        imagedEmbryo.addListener(panel);
        this.add(panel);
        
        navFrame = new Navigation_Frame(imagedEmbryo,panel);       
        imagedEmbryo.addListener(navFrame);
        
        selectedNucFrame = new SelectedNucleusFrame(this,imagedEmbryo);
//        selectedNucFrame.setVisible(true);        
        buildMenu();
        this.pack();
   
        String homeDir = System.getProperty("user.home");
        File propFile = new File(homeDir,"Ace3D_Frame.properties");
        props.open(propFile.getPath());
        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                try {
                    close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
//        imagedEmbryo.getNucleusFile().addListener(navFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(selectedNucFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(panel); 
        
        viPlot = new VolumeIntensityPlot(imagedEmbryo);
        viDialog = new PanelDisplay(viPlot);
    }

    public void close() throws Exception {
        props.save();
        System.exit(0);
    }
    @Override
    public void run(String string) {
        this.setSize(1464,627);
        this.setVisible(true);
        navFrame.run(null);
        this.selectedNucFrame.run(null);
    }
    

    final void buildMenu(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem mvr = new JMenuItem("Open Series Directory");
        fileMenu.add(mvr);
        mvr.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String vsProp = props.getProperty("MVRDir");
                if (vsProp != null){
                    dirChooser.setSelectedFile(new File(vsProp));
                } 
                if (dirChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){   
                    File sel = dirChooser.getSelectedFile();
                    File mvrDir = new File(sel,"MVR_STACKS");
                    
                    HDF5DirectoryImageSource source = new HDF5DirectoryImageSource(mvrDir,"exported_data","Segmented",imagedEmbryo,false);
                    boolean segFound = source.open();
                    if (segFound){
                        
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) ); 

                        int lineChannel = source.getChannel();
                        int expChannel = 1;
                        if (lineChannel == 1){
                            expChannel = 2;
                        }

                        File typical = new File(mvrDir,String.format("TP1_Ch%d_Ill0_Ang0,90.tif",lineChannel));
                        TifDirectoryImageSource lineSource = new TifDirectoryImageSource(typical.getPath(),"Lineaging",imagedEmbryo,true);             
                        panel.setTimeRange(Math.max(lineSource.getMinTime(),panel.getMinTime())
                            ,Math.min(lineSource.getMaxTime(),panel.getMaxTime()) ); 

                        typical = new File(mvrDir,String.format("TP1_Ch%d_Ill0_Ang0,90.tif",expChannel));
                        TifDirectoryImageSource expSource = new TifDirectoryImageSource(typical.getPath(),"Expressing",imagedEmbryo,false);             
                        panel.setTimeRange(Math.max(expSource.getMinTime(),panel.getMinTime())
                            ,Math.min(expSource.getMaxTime(),panel.getMaxTime()) );    

                        bhc  = new BHCDirectory(new File(sel,"BHC"));
                        Ace3D_Frame.this.setTitle(bhc.getDirectory().getParentFile().getName());
                        imagedEmbryo.getNucleusFile().setBHCTreeDirectory(bhc);                    

                        imagedEmbryo.notifyListeners();                    
                        props.setProperty("MVRDir",sel.getPath());
                        panel.changeTime(1);
                    }else {
                        imagedEmbryo.clearSources();
                        JOptionPane.showMessageDialog(Ace3D_Frame.this, "No Segmentation probability hdf5 files found");
                    }
                }             
            }
        });
        fileMenu.addSeparator();
      
        JMenuItem session = new JMenuItem("Open Existing Session");
        fileMenu.add(session);
        session.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String prop = props.getProperty("Session");
                if (prop != null){
                    fileChooser.setSelectedFile(new File(prop));
                }
                if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = fileChooser.getSelectedFile();
                    try {
                        openSession(sel);

                        
//                        ((NamedNucleusFile)imagedEmbryo.getNucleusFile()).divisionReport(System.out);
//                        ((NamedNucleusFile)imagedEmbryo.getNucleusFile()).timeLinkageReport(System.out);
                    } catch (Exception exc){
                        exc.printStackTrace();
                    }
                    props.setProperty("Session",sel.getPath());
                }
            }
        });
        
        JMenuItem saveAsSession = new JMenuItem("Save As Current Session");
        fileMenu.add(saveAsSession);
        saveAsSession.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAsSession();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });  
        
        JMenuItem saveSession = new JMenuItem("Save Current Session");
        fileMenu.add(saveSession);
        saveSession.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (sessionXML != null){
                        saveSession(sessionXML);
                    }else {
                        saveAsSession();
                    }

                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.addSeparator();

        JMenuItem saveTree = new JMenuItem("Save Tree Image As");
        saveTree.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser ch = new JFileChooser(bhc.getDirectory().getParentFile());
                    int retVal = ch.showSaveDialog(panel);
                    if (retVal == JFileChooser.APPROVE_OPTION){
                        ImageIO.write(navFrame.getTreeImage(),"png", ch.getSelectedFile()); 
                    }
                }catch (Exception exc){
                     exc.printStackTrace();
                }
            }
        });
        fileMenu.add(saveTree);
        fileMenu.addSeparator();
        
        JMenuItem calcExp = new JMenuItem("Calculate Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    Ace3D_Frame.this.imagedEmbryo.calculateExpression();
                }).start();
            }
        });
        fileMenu.add(calcExp);
        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    close();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(exit);

        JMenu segmenting = new JMenu("Segmenting");
        menuBar.add(segmenting);
        
        JMenuItem cutItem = new JMenuItem("Current Time Point");
        cutItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                bhc.open();
                try {
                    cutTree();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });       
        segmenting.add(cutItem); 
/*        
        JMenuItem autoSeg = new JMenuItem("Auto - Between Curations");
        segmenting.add(autoSeg);
        autoSeg.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
 //                   ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoSegmentBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
*/
        segmenting.addSeparator();
     
        JMenuItem scatter = new JMenuItem("Intensity/Volume Plot");
        scatter.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    viDialog.setVisible(true);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        segmenting.add(scatter);

        JMenuItem lineplot = new JMenuItem("Nuclei Probability Plot");
        lineplot.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    
                    int time = getCurrentTime();
                    TreeMap<Integer,BHCTree> trees = bhc.getTrees(time);
                    BHCTree tree = null;
                    if (trees.size() > 1){
                        Object resp = JOptionPane.showInputDialog(null,"Choose segmentation threshold","thresholds ",
                                JOptionPane.INFORMATION_MESSAGE, null, trees.keySet().toArray(),trees.firstKey());
                        tree = trees.get(resp);
                    } else {
                        tree = trees.firstEntry().getValue();
                    }
                    SegmentationLinePlot plot = new SegmentationLinePlot();
                    plot.setTree(tree);
                    PanelDisplay dialog = new PanelDisplay(plot);
                    dialog.setVisible(true);    
                   
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        segmenting.add(lineplot);
        segmenting.addSeparator();
        
        JMenuItem remove = new JMenuItem("Remove Nuclei - Current Time");
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                LinkedNucleusFile nf = (LinkedNucleusFile)imagedEmbryo.getNucleusFile();
                nf.removeNuclei(getCurrentTime(), false);
                imagedEmbryo.notifyListeners();
            }
        });
        
        JMenuItem removeTail = new JMenuItem("Remove Nuclei - Current and Later Times");
        removeTail.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                
                for (int t = panel.getTime(); t <= panel.getMaxTime(); t++) {
                    LinkedNucleusFile nf = (LinkedNucleusFile)imagedEmbryo.getNucleusFile();
                    nf.removeNuclei(t, false);
                }
               imagedEmbryo.notifyListeners();
            }
        }); 
        
        segmenting.add(remove);
        segmenting.add(removeTail);
        JMenuItem removeAll = new JMenuItem("Remove All Nuclei");
        removeAll.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                int resp = JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to remove all the nuclei?");
                if (resp == JOptionPane.YES_OPTION){
                    Integer[] times = imagedEmbryo.getNucleusFile().getAllTimes().toArray(new Integer[0]);
                    for (int i=0 ; i<times.length ; ++i){
                        LinkedNucleusFile nf = (LinkedNucleusFile)imagedEmbryo.getNucleusFile();
                        nf.removeNuclei(i, false);
                    }
                    imagedEmbryo.notifyListeners();
                }
            }
            
        });
        //segmenting.add(removeAll); 
        segmenting.addSeparator();
/*        
        JMenuItem submitBHC = new JMenuItem("Submit to Grid");
        segmenting.add(submitBHC);
        submitBHC.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bhc.getDirectory() != null){
                    
                    int[] dims = TimePointImage.getIntDims();
                    if (bhc.getDirectory().getParent().endsWith("diSPIM")) {
                        submitDialog = new BHCSubmitDialog(Ace3D_Frame.this,bhc.getDirectory().getPath(),dims);                       
                    } else submitDialog = new BHCSubmitDialog(Ace3D_Frame.this,bhc.getDirectory().getParent(),dims);
                    submitDialog.setVisible(true);
                }
            }
        });
        
        JMenuItem bhcTable = new JMenuItem("BHC Table");
        segmenting.add(bhcTable);
        bhcTable.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (labMan != null){
                    labMan.setVisible(true);
                }
            }
        });
 */       
        JMenuItem backload = new JMenuItem("Sync BHC table");
        backload.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (labMan != null)backloadBHCTable();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        segmenting.add(backload);
        
        JMenu linking = new JMenu("Linking");
        menuBar.add(linking);

        JMenuItem searchlinkItem = new JMenuItem("Auto Link - Tree Search");
        linking.add(searchlinkItem);
        searchlinkItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int fromTime = -1;
                    while (fromTime == -1){
                        try {
                            int tp = imagedEmbryo.getNucleusFile().getAllTimes().size();
                            String ans = JOptionPane.showInputDialog(null, "Enter time to link back to", String.valueOf(tp));
                            if (ans == null){
                                return;
                            }
                            fromTime = Integer.valueOf(ans);
                        } catch (Exception exc){}
                    }
                    bhc.open();
                    TreeMap<Integer,Integer> probMap = mapTimesToThreshProbs(fromTime,getCurrentTime());
                    autolinkTree(probMap);

//                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLinkBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    if (!(exc instanceof NullPointerException)) {
                        exc.printStackTrace();
                    }
                }
            }
        });      
 

        JMenuItem decisionTreelinkItem = new JMenuItem("Auto Link - DecisionTree");
        linking.add(decisionTreelinkItem);
        decisionTreelinkItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int fromTime = -1;
                    while (fromTime == -1){
                        try {
                            int tp = imagedEmbryo.getNucleusFile().getAllTimes().size();
                            String ans = JOptionPane.showInputDialog(null, "Enter time to link back to", String.valueOf(tp));
                            if (ans == null){
                                return;
                            }
                            fromTime = Integer.valueOf(ans);
                        } catch (Exception exc){}
                    }
                    bhc.open();
                    TreeMap<Integer,Integer> probMap = mapTimesToThreshProbs(fromTime,getCurrentTime());
                    autolinkDecision(probMap);

//                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLinkBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    if (!(exc instanceof NullPointerException)) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        
        JMenuItem unlink = new JMenuItem("Unlink Current Time");
        linking.add(unlink);
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<Nucleus> nucs = imagedEmbryo.getNucleusFile().getNuclei(getCurrentTime());
                NucleusFile nucFile = imagedEmbryo.getNucleusFile();
                int count = 1;
                for (Nucleus nuc : nucs){
                    nucFile.unlinkNucleus(nuc, count == nucs.size());  // notify listeners at last nucleus
                    ++count;
                }
            }
        });        
        JMenu view = new JMenu("Annotations");
        menuBar.add(view);
        
        segmentedNuclei = new JCheckBoxMenuItem("Nuclei indicator");
        segmentedNuclei.setSelected(true);
        segmentedNuclei.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });
        view.add(segmentedNuclei);
        
        inactiveNuclei = new JCheckBoxMenuItem("Inactive Nuclei indicator");
        inactiveNuclei.setSelected(true);
        inactiveNuclei.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });
        view.add(inactiveNuclei);        
        
        divisionIndicator = new JCheckBoxMenuItem("Division indicator");
        divisionIndicator.setSelected(true);
        divisionIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(divisionIndicator);
        
        sisters = new JCheckBoxMenuItem("Sister indicator");
        sisters.setSelected(false);
        sisters.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(sisters);

        locationIndicator = new JCheckBoxMenuItem("Location indicator");
        locationIndicator.setSelected(true);
        locationIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(locationIndicator);
        
        selectedLabeled = new JCheckBoxMenuItem("Label the Selected Nucleus");
        selectedLabeled.setSelected(false);
        selectedLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(selectedLabeled);
        
        nucleiLabeled = new JCheckBoxMenuItem("Label All the Nuclei");
        nucleiLabeled.setSelected(false);
        nucleiLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(nucleiLabeled);
        this.setJMenuBar(menuBar);        
    }
    public TreeMap<Integer,Integer> mapTimesToThreshProbs(int fromTime,int toTime){
        // determine which probabilities to use for each time point
        TreeMap<Integer,Integer> map = new TreeMap();
        for (int t=fromTime ; t<=toTime ; ++t){
            Set<Integer> probs = bhc.getThresholdProbs(t);
            Integer[] probArray = probs.toArray(new Integer[0]);
            Arrays.sort(probArray, Collections.reverseOrder());
            if (probs.size()==1){
                map.put(t,probArray[0]);
            } else {
                // prompt for which to use
                Object resp = JOptionPane.showInputDialog(null,
                        String.format("For time = %d", t),"Choose probability threshold", JOptionPane.INFORMATION_MESSAGE, null, probArray, probArray[0]);
                if (resp == null) return null;
                map.put(t, (Integer)resp);
            }
        }   
        return map;
    }

    private void moveToTime(){
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the time:");
            if (timeStr == null) return;
            try {
                int time = Integer.valueOf(timeStr);
                panel.changeTime(time);
                valid = true;
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
    }
    private String setMinTime(){
        String ret = null;
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the first time value:");
            if (timeStr == null) return ret;  // null return means user cancelled input
            try {

                valid = true;
                ret = timeStr;
                
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
        return ret;        
    }
    public int getCurrentTime(){
        return panel.getTime();
    }
    
    // starting from BHC directory, no nucleus file exists yet
    private void openBHCDir(File sel)throws Exception {
        if (sel == null){
            String bhcProp = props.getProperty("BHC");
            if (bhcProp != null){
                fileChooser.setSelectedFile(new File(bhcProp));
            } 
            if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
                sel = fileChooser.getSelectedFile();

            }            
        }
        
        bhc  = new BHCDirectory(sel);
        imagedEmbryo.getNucleusFile().setBHCTreeDirectory(bhc);
        
        props.setProperty("BHC",sel.getPath());            
        
    } 
    private void openSession(File xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(xml);
        Element root = doc.getRootElement();   
        if (!root.getName().equals("Ace3DSession")){
            return;
        }
        
        Element bhcEle = root.getChild("BHCTreeDirectory");
        if (bhcEle != null){
            openBHCDir(new File(bhcEle.getAttributeValue("path")));
            
            
        }
        this.setTitle(bhc.getDirectory().getParentFile().getName());
        imagedEmbryo.fromXML(root.getChild("ImagedEmbryo"));
        for (ImageSource source : imagedEmbryo.getSources()){
            panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                ,Math.min(source.getMaxTime(),panel.getMaxTime()) );
        }
        imagedEmbryo.notifyListeners(); 
        if (labMan != null){
            ((BHCPanel)labMan.getPanel("BHC")).setEmbryo(bhc.getDirectory().getParentFile().getName());
        }
        Element dsEle = root.getChild("DataSets");
        for (Element props : dsEle.getChildren("DataSetProperties")){
            String name = props.getAttributeValue("Name");
            DataSetProperties p = new DataSetProperties(props);
            dataSetProperties.put(name, p);
            contrastDialog.setProperties(name, p);
        }
        this.sessionXML = xml;
        if (imagedEmbryo.getNucleusFile().getAllTimes().size() > 0) {
            panel.changeTime(imagedEmbryo.getNucleusFile().getAllTimes().size());
        }
        update_diSPIM_Status(xml);
//        imagedEmbryo.reportDivisionEccengtricty();
    }
    private void saveAsSession()throws Exception {
        JFileChooser sessionChooser=null;        
        if (sessionXML == null){

            if (bhc == null){
                sessionChooser = new JFileChooser();
            } else {
                sessionChooser = new JFileChooser(bhc.getDirectory());
            }

        }else {
            sessionChooser = new JFileChooser(sessionXML);
        }
        if (sessionChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            saveSession(sessionChooser.getSelectedFile());
        } else {
            return;
        }                    
    }
    private void saveSession(File xml)throws Exception {
        
        Element root = new Element("Ace3DSession");
        if (bhc != null){
            root.addContent(bhc.toXML());
        }
       
        root.addContent(imagedEmbryo.toXML());
        
        Element dsProps = new Element("DataSets");
        for (String ds : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(ds);
            Element dsEle = props.toXML();
            dsEle.setAttribute("Name", ds);
            dsProps.addContent(dsEle);
        }
        root.addContent(dsProps);
        
        OutputStream stream = new FileOutputStream(xml);       
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
        props.setProperty("Session",xml.getPath());
        sessionXML = xml;
        update_diSPIM_Status(xml);

    }
    private void update_diSPIM_Status(File xml)throws Exception{
        // update the active session for the series in the db
        if (labMan != null){
            int maxTime = ((LinkedNucleusFile)this.imagedEmbryo.getNucleusFile()).getLastTime();
            Set<Nucleus> nucs = this.imagedEmbryo.getNuclei(maxTime);
            String dispimName = bhc.getDirectory().getParentFile().getName();
            PreparedStatement state = MySql.getMySql().getStatement(
                    "Update diSPIM set Session = ? , EditedToTime=? , EditedToCells=? where Name = ?");
            state.setString(1, xml.getPath());
            state.setInt(2, maxTime);
            state.setInt(3, nucs.size());
            state.setString(4,dispimName);
            state.execute();
        }        
    }
    
    private void submitAllTimePoints()throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        String segTiff = props.getProperty("SegmentedTIFF");
        if (segTiff != null){
            fileChooser.setSelectedFile(new File(segTiff));
        }
        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            segTiff = fileChooser.getSelectedFile().getPath();
//            Nuclei_Identification.submitTimePoints(segTiff,1,2);
            props.setProperty("SegmentedTIFF",segTiff);
        }
    }
    private void cutTree()throws Exception {
        int time = this.getCurrentTime();
        BHCDirectory bhcTree = imagedEmbryo.getNucleusFile().getTreeDirectory();
        if (treeCutDialog == null){
            treeCutDialog = new BHCTreeCutDialog(this,this.imagedEmbryo);
        }
        treeCutDialog.setBHCTrees(bhcTree,time);
        treeCutDialog.setVisible(true);
        if (treeCutDialog.isOK()){
            double nextThresh = treeCutDialog.getThresh();
 //           bhcNucFile.setThreshold(nextThresh);
        }
    }
 



   public void backloadBHCTable()throws Exception {
        LinkedNucleusFile nucFile = (LinkedNucleusFile)imagedEmbryo.getNucleusFile();
        String diSpimName = bhc.getDirectory().getParentFile().getName();
        String sql = String.format("Select * from diSPIM where Name=\"%s\"",diSpimName);
        ResultSet rs = MySql.getMySql().execute(sql);
        if (!rs.next()) return;
        int timePoints = rs.getInt("TimePoints");
        
        PreparedStatement insertBHC = MySql.getMySql().getStatement("insert into BHC (LogConcentration,Variance,DegreesFreedom,diSPIMName,BHCTime,SegmentProbThresh,BHCID,BoundingBox) "
                + "values (?,?,?,?,?,?,?,?)");
        insertBHC.setString(4,diSpimName)
                ;
        PreparedStatement insertLMS = MySql.getMySql().getStatement("insert into LMSTracking (ID,DBTable,Project,Status) values (?,?,?,?) ");
           insertLMS.setString(2,"BHC");
           insertLMS.setString(3, "Imaging");        
        
        PreparedStatement updateState = MySql.getMySql().getStatement("update LMSTracking set Status=? where ID=? and DBTable=? and Project=? ");
        updateState.setString(3, "BHC");
        updateState.setString(4, "Imaging");
        
        PreparedStatement selectState = MySql.getMySql().getStatement("select * from BHC B left join LMSTracking T on B.BHCID=T.ID where B.BHCTime=? and B.diSPIMName = ?");
        selectState.setString(2, diSpimName);
        
        String bounding = null;
        
        for (int t =1 ; t<=timePoints ; ++t){
            if (t==251){
                int aushdf=0;
            }
            insertBHC.setInt(5,t);
            selectState.setInt(1, t);
            ResultSet selectRS = selectState.executeQuery();
            if (selectRS.next()){
                // process each exisiting BHC record for the timepoint
                do {
                    int probThresh = selectRS.getInt("SegmentProbThresh");
                    String bhcID = selectRS.getString("BHCID");
                    updateState.setString(2, bhcID);
                    File file = bhc.getTreeFile(t, probThresh);
                    if (!file.exists()){
                        updateState.setString(1, "Pending");
                    }else {
                        Integer probUsed = nucFile.getThresholdProb(t);
                        if (probUsed != null && probThresh == probUsed){
                            updateState.setString(1, "Active");
                        } else{
                            updateState.setString(1, "Complete");
                        }
                    }
                    updateState.execute();
                } while (selectRS.next());
            }else {
                // no BHC records for this timepoint , backfill the BHC table
                Set<Integer> probs = bhc.getThresholdProbs(t);
                if (probs.isEmpty()){
                    // no BHC runs to backfill for this timepoint, add a default record
                    int p = 50;
                    File bhcFile = bhc.firstBHCFile();
                    if (bounding==null){
                        bounding = boundingBox(bhcFile);
                    }                     
                    String id = org.rhwlab.LMS.diSPIM.BHCID.formID(diSpimName, t, p);
                    insertLMS.setString(1,id);
                    insertLMS.setString(4, "Pending");
                    insertLMS.execute();
                    
                    insertBHC.setString(6,Integer.toString(p));
                    insertBHC.setString(7,id);
                    insertBHC.setString(8, bounding);
                    insertBHC.setInt(3,10); 
                    insertBHC.setInt(1,10);
                    insertBHC.setDouble(2, 20.0);
                    insertBHC.execute();
                }
                else {
                    // backfilling previous BHC runs
                    for (int p : probs){
                        File bhcFile = bhc.getTreeFile(t, p);
                        if (bounding==null){
                            bounding = boundingBox(bhcFile);
                        }                        
                        BufferedReader reader = new BufferedReader(new FileReader(bhcFile));
                        String line = reader.readLine();
                        String[] tokens= line.split(" ");                        
                        
                        insertBHC.setString(6,Integer.toString(p));
                        String id = org.rhwlab.LMS.diSPIM.BHCID.formID(diSpimName, t, p);
                        insertBHC.setString(7,id);
                        insertBHC.setString(8, bounding);

                        insertLMS.setString(1,id);
                        Integer probUsed = nucFile.getThresholdProb(t);
                        if (probUsed != null && p == probUsed){
                            insertLMS.setString(4, "Active");
                        } else {
                            insertLMS.setString(4,"Complete");
                        }
                        for (int i=0 ; i<tokens.length; ++i){
                            if (tokens[i].startsWith("nu=")){
                                String[] values = tokens[i].split("\"");
                                insertBHC.setInt(3, Integer.valueOf(values[1]));
                            }
                            else if (tokens[i].startsWith("alpha")){
                                String[] values = tokens[i].split("\"");
                                insertBHC.setInt(1, (int)Math.log10(Double.valueOf(values[1])));
                            }
                            else if (tokens[i].startsWith("s=")){
                                insertBHC.setDouble(2, Double.valueOf(tokens[i+1]));
                            }
                        }
                        insertBHC.execute();
                        insertLMS.execute();
                    }
                }
            }
        }
    }
   // determine the bounding box used for micro clustering, given the bhc tree file 
    private String boundingBox(File bhcFile)throws Exception {
        
        TreeMap<String,Integer> boxMap = new TreeMap<>();
        
        // open the micro cluster xml and see if it contains the bounding box
        File clusterFile = new File(bhcFile.getParent(),bhcFile.getName().replace("BHCTree","Clusters"));
        if (clusterFile.exists()){
            BufferedReader reader = new BufferedReader(new FileReader(clusterFile));
            String line = reader.readLine();
            reader.close();

            String[] tokens = line.split(" ");
            for (String token : tokens){
                if (token.startsWith("xmin")||token.startsWith("ymin")||token.startsWith("zmin")||token.startsWith("xmax")||token.startsWith("ymax")||token.startsWith("zmax")){
                    String[] vals = token.split("\"");
                    boxMap.put(vals[0],new Integer(vals[1]));
                }
            }
        }
        
        if (boxMap.isEmpty()){
            // did not find a bounding box in the micro cluster file, using 0.05 tohead  0.95 of the dimensions of the images
            File MVR = new File(bhcFile.getParentFile().getParentFile(),"MVR_STACKS");
            String tiff = new File(MVR,bhcFile.getName().substring(0,bhcFile.getName().indexOf("_Probabilities"))+".tif").getPath();
            tiff = tiff.replace("0_90","0,90");
            ImagePlus ip = new Opener().openImage(tiff);
            boxMap.put("xmin",(int)(0.05*ip.getWidth()));
            boxMap.put("xmax",(int)(0.95*ip.getWidth()));
            boxMap.put("ymin",(int)(0.05*ip.getHeight()));
            boxMap.put("ymax",(int)(0.95*ip.getHeight()));
            boxMap.put("zmin",(int)(0.05*ip.getNSlices()));
            boxMap.put("zmax",(int)(0.95*ip.getNSlices()));   
            ip.close();
        }
        
        //make a json object of the bounding box
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Entry<String,Integer> entry : boxMap.entrySet()){
            builder.add(entry.getKey(),entry.getValue());
        }
        return builder.build().toString();
        
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        panel.stateChanged(e);
    }   

    static public boolean labelNuclei(){
        return nucleiLabeled.getState();
    }
    static public boolean labelSelectedNucleus(){
        return selectedLabeled.getState();
    }
    static public boolean sistersIndicated(){
        return sisters.getState();
    }
    static public boolean divisionsIndicated(){
        return divisionIndicator.getState();
    }    
    static public boolean nucleiIndicated(){
        return segmentedNuclei.getState();
    }
    static public boolean inactiveIndicated(){
        return inactiveNuclei.getState();
    }    
    static public boolean locationIndicated(){
        return locationIndicator.getState();
    }

    static public List<String> datasetsSelected(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(key);
            if (props.selected){
                ret.add(key);
            }
        }
        return ret;
    }
    static public List<String> getAllDatsets(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            ret.add(key);
        }
        return ret;        
    }
    static public DataSetProperties getProperties(String dataSet){
        return dataSetProperties.get(dataSet);
    }
    static public void setProperties(String dataset,DataSetProperties ps){
        dataSetProperties.put(dataset, ps);
    }
    
    static public LUT getLUT(String dataSet){
        return dataSetLuts.get(dataSet);
    }
    static public DataSetsDialog getDataSetsDialog(){
        return contrastDialog;
    }
    public ImagedEmbryo getEmbryo(){
        return this.imagedEmbryo;
    }
    
    public void autolinkTree(TreeMap<Integer,Integer> probMap) {
        Set<Integer> timesSet = probMap.navigableKeySet();
        Integer[] timesArray = timesSet.toArray(new Integer[0]);
        Integer[] probsArray = new Integer[timesArray.length];
        for (int i=0 ; i<timesArray.length;++i){
            probsArray[i] = probMap.get(timesArray[i]);
        }

        new Thread(() -> {
            try {
                ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).bestMatchAutoLink(timesArray,probsArray,minimumVolume); // minvolume of 50
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }).start();
    }
    
    public void autolinkDecision(TreeMap<Integer,Integer> probMap) {
        Set<Integer> timesSet = probMap.navigableKeySet();
        Integer[] timesArray = timesSet.toArray(new Integer[0]);
        Integer[] probsArray = new Integer[timesArray.length];
        for (int i=0 ; i<timesArray.length;++i){
            probsArray[i] = probMap.get(timesArray[i]);
        }

        new Thread(() -> {
            try {
                ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).decisionTreeAutoLink(timesArray,probsArray,Ace3D_Frame.minimumVolume,neighborhoodradius); // minvolume of 50
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }).start();
    }
    
    File sessionXML;
    Properties props = new Properties();
    JMenu dataset;
    JMenu contrast;
    JMenu lutMenu;
    JMenu colorMenu;
    
    ImagedEmbryo imagedEmbryo;
    
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame selectedNucFrame;
    JFileChooser fileChooser = new JFileChooser();
    JFileChooser dirChooser = new JFileChooser();
    static DataSetsDialog contrastDialog;
    Navigation_Frame navFrame;
    LookUpTables lookUpTables = new LookUpTables();
    BHCTreeCutDialog treeCutDialog;
    BHCSubmitDialog submitDialog;
    BHCDirectory bhc;
    VolumeIntensityPlot viPlot;
    PanelDisplay viDialog;
    static public LabMan labMan;
    
    
    static JCheckBoxMenuItem segmentedNuclei;
    static JCheckBoxMenuItem inactiveNuclei;
    static JCheckBoxMenuItem sisters;
    static JCheckBoxMenuItem locationIndicator;
    static JCheckBoxMenuItem divisionIndicator;
    static JCheckBoxMenuItem nucleiLabeled;
    static JCheckBoxMenuItem selectedLabeled;
    static JMenuItem[] colorChoices;
    static TreeMap<String,LUT> dataSetLuts = new TreeMap<>();
    
    static TreeMap<String,DataSetProperties> dataSetProperties = new TreeMap<>();
    
    static public int minimumVolume = 1000;  // minimum volume of a remnant nucleus
    static public double neighborhoodradius = 50;
    static public void main(String[] args) {
        EventQueue.invokeLater(new Runnable(){
            @Override
            public void run() {
                Interpreter.batchMode=true;
                //ImageJ ij = new ImageJ(ImageJ.NO_SHOW);
                try {
                    Ace3D_Frame frame = new Ace3D_Frame(args);
                    frame.run(null);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
    }     


}
