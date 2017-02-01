/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Navigation_Frame extends JFrame implements PlugIn,InvalidationListener {
    public Navigation_Frame(ImagedEmbryo emb,SynchronizedMultipleSlicePanel p){
        super();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.setTitle("Navigation Trees");
        this.embryo = emb;
        this.panel = p;
        this.getContentPane().setLayout(new BorderLayout());
        
        treePanel = new NavigationTreePanel(embryo);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        
        headPanel = new NavigationHeaderPanel();
        headPanel.setTreePanel(treePanel);
        treePanel.setHeadPanel(headPanel);       
        this.add(headPanel,BorderLayout.NORTH); 
        
        rootsRoot = new DefaultMutableTreeNode("Roots",true);
        rootsTree = new JTree(rootsRoot);
        rootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);    
        rootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                ChangeEvent event = new ChangeEvent(rootsTree);
                treePanel.stateChanged(event);
            }
        });
        JScrollPane rootsScroll = new JScrollPane(rootsTree);

       
        nucsRoot = new DefaultMutableTreeNode("All Nuclei",true);
        nucsTree = new JTree(nucsRoot);
        nucsTree.setCellRenderer(new NucleusRenderer());
        nucsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        nucsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)nucsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    embryo.setSelectedNucleus(nuc);
                    panel.changeTime(nuc.getTime());
                    panel.changePosition(nuc.getCenter());
                } else{
                    String timeLabel = (String)node.getUserObject();
                    try {
                        int t = Integer.valueOf(timeLabel.substring(5,timeLabel.indexOf(' ')));
                        panel.changeTime(t);
                    } catch (Exception exc){}
                }
            }
        });
        JScrollPane nucsScroll = new JScrollPane(nucsTree);
        Dimension prefdim = nucsScroll.getPreferredSize();
        //prefdim.setSize(2*prefdim.width, prefdim.height);
        prefdim.setSize(200, 100);
        nucsScroll.setPreferredSize(prefdim);
        
        deathsRoot = new DefaultMutableTreeNode("Terminal Nuclei",true);  
        deathsTree = new JTree(deathsRoot);
        deathsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); 
        deathsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)deathsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    embryo.setSelectedNucleus(nuc);
                    panel.changeTime(nuc.getTime());
                    panel.changePosition(nuc.getCenter());
                }                
            }
        });
        JScrollPane deathsScroll = new JScrollPane(deathsTree);       
        JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,deathsScroll,rootsScroll);
        prefdim = leftPane.getPreferredSize();
        prefdim.setSize(2*prefdim.width, prefdim.height);
        leftPane.setPreferredSize(prefdim);
        leftPane.setDividerLocation(100);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,nucsScroll,leftPane);
        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,split,treeScroll);
        this.add(split2,BorderLayout.CENTER);

        pack();
        

    }

    @Override
    public void run(String arg) {
        this.setSize(1540, 531);
        this.setLocation(0, 627);
        this.setVisible(true);
    }


    @Override
    public void invalidated(Observable observable) {
        
        // get the selected roots
        TreePath[] selectedPaths = rootsTree.getSelectionPaths();
        nucsRoot.removeAllChildren();
        rootsRoot.removeAllChildren();
        deathsRoot.removeAllChildren();
        
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null) { 
            return;
        }
        int currentTime = Navigation_Frame.this.panel.getTime();
        Nucleus selectedNucleus = embryo.getNucleusFile().getSelected();
        DefaultMutableTreeNode currentTimeNode = null;
        DefaultMutableTreeNode selectedNode = null;
        Set<Integer> times = nucFile.getAllTimes();
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            DefaultMutableTreeNode timeNode = null;
            if (nucFile.isCurated(time)){
                timeNode = new DefaultMutableTreeNode(String.format("%d: Curated(%d)",time,nucs.size()));
            }else {
                timeNode = new DefaultMutableTreeNode(String.format("%d: Auto(%d)",time,nucs.size()));
            }
            nucsRoot.add(timeNode);
            for (Nucleus nuc : nucs){
                DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                timeNode.add(nucNode);  
                if (nuc.equals(selectedNucleus)){
                    selectedNode = nucNode;
                }
            }
            if (time == currentTime){
                currentTimeNode = timeNode;
            }
        }
        
        TreeMap<Integer,Set<Nucleus>> rootMap = embryo.getRootNuclei();
        for (Integer t : rootMap.keySet()){
            for (Nucleus root : rootMap.get(t)){
                addFirstNucToNode(root,rootsRoot);
            }
        }
        rootsTree.setModel(new DefaultTreeModel(rootsRoot));
        
        // reselect the previous selected nuclei
        ArrayList<TreePath> foundList = new ArrayList<>();
        if (selectedPaths != null){
            for (TreePath path : selectedPaths){
                DefaultMutableTreeNode lastNode= (DefaultMutableTreeNode)path.getLastPathComponent();
                String nucName = ((Nucleus)lastNode.getUserObject()).getName();
                DefaultMutableTreeNode found = (DefaultMutableTreeNode)this.findNucleus(nucName, rootsRoot);
                if (found != null){
                    foundList.add(new TreePath(found.getPath()));
                }
            }
            selectedPaths = foundList.toArray(new TreePath[0]);
            rootsTree.setSelectionPaths(selectedPaths);
        }
        
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getLeaves(time);
            if (!nucs.isEmpty()){
                DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(String.format("Time:%d",time));
                deathsRoot.add(timeNode);
                for (Nucleus nuc : nucs){
                    DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                    timeNode.add(nucNode);                
                }
            }
        }        
        
        DefaultTreeModel nucsModel = new DefaultTreeModel(nucsRoot);
        nucsTree.setModel(nucsModel);
        deathsTree.setModel(new DefaultTreeModel(deathsRoot));
        
        // make the current time visible
        TreeNode[] nodes = null;
        if (selectedNode != null && currentTime==selectedNucleus.getTime()){
            nodes = nucsModel.getPathToRoot(selectedNode);
        }        
        else if (currentTimeNode != null){           
            nodes = nucsModel.getPathToRoot(currentTimeNode);
        }
        if (nodes != null){
            TreePath path = new TreePath(nodes);
            nucsTree.setExpandsSelectedPaths(true);
            nucsTree.setSelectionPath(path);
            nucsTree.makeVisible(path);
            nucsTree.scrollPathToVisible(path);
        }
        treePanel.stateChanged(new ChangeEvent(rootsTree));
        this.invalidate();
        
    }
    private void addFirstNucToNode(Nucleus firstNuc,DefaultMutableTreeNode node){
        DefaultMutableTreeNode cellNode = new DefaultMutableTreeNode(firstNuc);
        node.add(cellNode);
        Nucleus lastNuc = firstNuc.lastNucleusOfCell();
        if (lastNuc.isDividing()){
            Nucleus[] next = lastNuc.nextNuclei();
            addFirstNucToNode(next[0],cellNode);
            addFirstNucToNode(next[1],cellNode);
        }

        
    }
    private TreeNode findNucleus(String name,DefaultMutableTreeNode node){
        Object obj = node.getUserObject();
        if (obj instanceof Nucleus){
            String nodeName = ((Nucleus)obj).getName();
            if (nodeName.equals(name)){
                return node;
            }
        }
        if (node.isLeaf()){
            return null;
        }
        for (int i=0 ; i<node.getChildCount() ; ++i){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            TreeNode ret = findNucleus(name,child);
            if (ret != null){
                return ret;
            }
        }
        return null;
    }
    ImagedEmbryo embryo;
    SynchronizedMultipleSlicePanel panel;
    NavigationHeaderPanel headPanel;
    NavigationTreePanel treePanel;
    
    DefaultMutableTreeNode rootsRoot;
    DefaultMutableTreeNode nucsRoot;
    DefaultMutableTreeNode deathsRoot;
    
    JTree rootsTree;
    JTree nucsTree;
    JTree deathsTree;
    
    public class NucleusRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus){

            NucleusRenderer comp = (NucleusRenderer)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object obj = ((DefaultMutableTreeNode)value).getUserObject();
            if (leaf && obj instanceof Nucleus){
                nuc = (Nucleus)obj;
                if (nuc.isDividing()){
                    comp.setForeground(Color.red);
                } else if (nuc.isLeaf()){
                    comp.setForeground(Color.blue);
                } else {
                    comp.setForeground(Color.black);
                } 
                int asjdfui=0;
            }            
            return comp;
        }
        
        Nucleus nuc;
    }
}
