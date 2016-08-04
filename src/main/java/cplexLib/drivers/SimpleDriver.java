package cplexLib.drivers;

import cplexLib.callbacks.NodeHandler; 

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import cplexLib.dataTypes.ActiveSubtree;
import cplexLib.dataTypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.*;
import cplexLib.dataTypes.Solution;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * solve trees for 10 seconds each , until they exceed 10 thousand nodes - at which point distribute all but 10000 nodes away
 * 
 * remove any inferior or completed subtrees and update incumbent solution
 * 
 * print incumbent when no trees left
 * 
 */
public class SimpleDriver {
    
     private static Logger logger=Logger.getLogger(SimpleDriver.class);
    
    private static int treesLeft ( List<ActiveSubtree> activeSubtreeList) throws Exception {
        int treesLeft=  ZERO;
        for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
            ActiveSubtree tree = activeSubtreeList.get(index);
            if (tree.hasEnded()) continue ;           
            
            treesLeft++;
        }
         
        logger.info("Number of trees left  "+ treesLeft);
        return treesLeft;
    }

    public static void main(String[] args) throws Exception {
        
        
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");     
        logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER+SimpleDriver.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
                
        double bestKnownIncumbentValue = PLUS_INFINITY;
        Solution bestKnownSolution = new Solution();
         
        
        List <NodeAttachment> farmedOutNodes = new ArrayList <NodeAttachment>(); 
       
        int TIME_SLICE = TEN*SIX  ; //seconds
        int MAX_TREE_SIZE_AFTER_TRIMMING = MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE - TWO ; 
        
        //it appears to be better to keep migration to a minimum. 
        //In other words, better to have fewer fat trees, than lots of thin trees
        
        int ITER_NUMBER = ZERO;
         
        logger.info("Started at  "+ LocalDateTime.now());
       
        try {
            
            List<ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree>();
            activeSubtreeList.add(new ActiveSubtree( new NodeAttachment()));
           
            while (ZERO < treesLeft (  activeSubtreeList)) {
               
                
                if(ZERO==(ONE+ITER_NUMBER)%SIX){
                    //after six iterations, make a drastic increase to the time slice, and allowed size of each sub-tree
                    TIME_SLICE *= TEN;
                    MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE *=(THOUSAND*TWO);
                    MAX_TREE_SIZE_AFTER_TRIMMING = MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE - TEN ; 
                }
                
                logger.info("Starting Iteration  "+ ITER_NUMBER + " time slice is " + TIME_SLICE + " MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE " + MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE) ;    
                
                /*
                for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
                    ActiveSubtree tree = activeSubtreeList.get(index);
                    //free up memory for done trees
                    if ((tree.isDiscardable()||tree.isSolvedToCompletion())&& !tree.ended ) {
                        tree.end();
                    } 
                     
                }*/
                
                //solve active trees for time slice
                for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
                    ActiveSubtree tree = activeSubtreeList.get(index);
                    if (tree.hasEnded()     ) continue ;
                     
                    logger.info("solving tree " +tree.getGUID() +" with  unsolved kids =  "+            tree.getPendingChildNodeCount());
                    tree.solve( TIME_SLICE, bestKnownIncumbentValue);
                    logger.info("  tree has this many penging nodes after solving" +      tree.getPendingChildNodeCount());
                    
                    if (!tree.isDiscardable() && tree.isOptimalOrFeasible()) {
                        if (tree.getSolution().getObjectiveValue() < bestKnownIncumbentValue){
                            bestKnownIncumbentValue =tree.getSolution().getObjectiveValue() ;
                            logger.info(" best incumbent updated to "+   bestKnownIncumbentValue); 
                            bestKnownSolution =  tree.getSolution();
                        }
                    }
                    
                    //do farming
                    if (tree.isDiscardable()||tree.isSolvedToCompletion()){
                        tree.end();
                         logger.info("ending tree " +tree.getGUID());
                    } else {
                        //if the tree has more than a certain threshold of nodes, farm out nodes
                        if (MAX_TREE_SIZE_AFTER_TRIMMING< tree.getPendingChildNodeCount()) {
                            farmedOutNodes .addAll(tree.farmOutNodes(MAX_TREE_SIZE_AFTER_TRIMMING));
                        }
                    }
                }
                
                
                //convert farmed out nodes into subtrees and add into tree list
                for (int index = ZERO ; index < farmedOutNodes.size(); index ++){
                    NodeAttachment node = farmedOutNodes.get(index);
                    activeSubtreeList.add(new ActiveSubtree(node));
                }
                farmedOutNodes = new ArrayList <NodeAttachment>(); 
                
                //net iteration
                ITER_NUMBER++;
                
            }   
            
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        logger.info("Best solution is "+bestKnownSolution.toString());
        logger.info("Number of iterations is "+ ITER_NUMBER);
        logger.info("Completed at  "+ LocalDateTime.now());
        
    }//main
}//class
                
  

/**
 * 
 * 
 
0029 1.0
0028 0.0
0027 1.0
0026 1.0
0025 1.0
0024 0.0
0023 1.0
0022 1.0
0021 1.0
0020 0.0
0019 0.0
0018 1.0
0017 0.0
0016 1.0
0015 1.0
0014 0.0
0045 1.0
0013 0.0
0044 0.0
0012 0.0
0043 1.0
0011 1.0
0042 1.0
0010 1.0
0041 1.0
0040 1.0
0009 1.0
0008 1.0
0039 1.0
0007 -0.0
0038 0.0
0006 1.0
0037 1.0
0005 1.0
0036 0.0
0004 1.0
0003 1.0
0035 1.0
0002 1.0
0034 1.0
0001 1.0
0033 0.0
0032 0.0
0031 0.0
0030 1.0
 
 * 
 */

