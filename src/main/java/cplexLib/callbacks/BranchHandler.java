package cplexLib.callbacks;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cplexLib.utilities.UtilityLibrary;
import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.*; 
import cplexLib.dataTypes.NodeAttachment;
import cplexLib.dataTypes.SubtreeMetaData;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * 1) accumulates branching conditions and any other variable bounds , into the kids
 * 2) discards nodes, or entire subtree,  which are inferior to already known incumbent
 * 3) implements distributed MIP gap by using the bestKnownGlobalOptimum
 *
 */
public class BranchHandler extends IloCplex.BranchCallback{
    
    private Logger logger ;
    private boolean isLoggingInitialized =false ;
      
 
    //meta data of the subtree which we are monitoring
    private SubtreeMetaData treeMetaData;
    
    //best known optimum is used to prune nodes
    private double bestKnownGlobalOptimum;
    
    public BranchHandler (SubtreeMetaData metaData) {
        this.  treeMetaData= metaData;       
    }
     
    public void refresh( double bestKnownGlobalOptimum) {
        this.bestKnownGlobalOptimum=bestKnownGlobalOptimum;
      
    } 
    
    /**
     * discard inferior nodes, or entire trees
     * Otherwise branch the 2 kids and accumulate variable bound information
     *   
     */
    protected void main() throws IloException {
        
        //initialize logging 
        if (! isLoggingInitialized) try {
            initLogging(PARTITION_ID);
        } catch (Exception ex) {
            //logging will not be available for this class
        }

        if ( getNbranches()> ZERO ){  
          
            //tree is branching
            
            //first check if entire tree can be discarded
            if (canTreeBeDiscarded() || (canNodeBeDiscarded()&&isSubtreeRoot())   ){
                
                //no point solving this tree any longer 
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is getting discarded "); 
                treeMetaData.setEntireTreeDiscardable();
                abort();
                
            } else  /*check if this node can be discarded*/ if (canNodeBeDiscarded()) {               
                // this node and its kids are useless
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is pruning inferior node "+         ((NodeAttachment)getNodeData()).nodeMetadata.nodeID); 
                prune();  
            } else      if (hasNodeMigratedAway()) {
                    //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is pruning migrated node "+         ((NodeAttachment)getNodeData()).nodeMetadata.nodeID);
                    prune();                    
            } else {
                
                //remove this node from the list of unsolved nodes, and   create its 2 kids
                             
                //First we must get the node data ,so that we can append the 
                //branching conditions and pass it on to the kids
                
                //get the node attachment for this node, any child nodes will accumulate the branching conditions
                NodeAttachment nodeData = (NodeAttachment) getNodeData();
                if (nodeData==null ) { //it will be null for subtree root
                    NodeAttachment subTreeRoot = treeMetaData.getRootNodeAttachment();
                    nodeData=new NodeAttachment (   subTreeRoot.isEasy(),  
                            subTreeRoot.getUpperBounds(), 
                            subTreeRoot.getLowerBounds(),  
                            subTreeRoot.getDepthFromOriginalRoot(), 
                            ZERO);         
                }
                //update the node attachment with end time                
                if (nodeData.getEndTimeFor_LP_Relaxation()<=ZERO)  {
                    nodeData.setEndTimeFor_LP_Relaxation(System.currentTimeMillis());
                    setNodeData(nodeData);
                }                
                
                //remove this node from the list of unsolved nodes
                if (nodeData.getDepthFromSubtreeRoot()>ZERO) {
                    treeMetaData.removeUnsolvedLeafNode(nodeData.nodeMetadata.nodeID);
                }
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                BranchDirection[ ][]  dirs = new  BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);

                //get bound tightenings
               // Map< IloNumVar,Double > upperBoundTightenings = findIntegerBounds(true);
               // Map< IloNumVar,Double > lowerBoundTightenings = findIntegerBounds(false);
                
                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {    
                    //apply the bound changes specific to this child
                    NodeAttachment thisChild  = UtilityLibrary.createChildNode( nodeData,
                            dirs[childNum], bounds[childNum], vars[childNum]  , isChildEasy() ); 

                    //apply bound tightenings

                    /*
                    for (Entry<IloNumVar, Double> entry : upperBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , true);
                    }
                    for (Entry<IloNumVar, Double> entry : lowerBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , false);
                    }
                    */

                    //   create the  kid,  and attach node data  to the kid
                    NodeId nodeID = makeBranch(childNum,thisChild );
                    //make a note of the new child in the meta data
                    thisChild.nodeMetadata.nodeID=nodeID.toString();
                    this.treeMetaData.addUnsolvedLeafNodes(nodeID.toString(), thisChild);

                }//end for 2 kids
                
                //check if number of unsolved kids has grown too large                  
                if (this.treeMetaData.getLeafNodesPendingSolution().size()> MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE) abort() ;
                
            } //and if else
        }//end getNbranches()> ZERO
    } //end main
    
    //node has migrated away if its not the subtree root, and its node ID is missing from the list of pending nodes
    private boolean hasNodeMigratedAway()  throws IloException {
        NodeAttachment nodedata  = (NodeAttachment)getNodeData() ;
        
        Set <String> pendingNodeIdSet = this.treeMetaData.getLeafNodesPendingSolution().keySet();
        return ( nodedata!=null) && (pendingNodeIdSet!=null) && (nodedata.getDepthFromSubtreeRoot()>ZERO) && 
                !pendingNodeIdSet.contains(nodedata.nodeMetadata.nodeID.toString());
    }
    
    private boolean canNodeBeDiscarded () throws IloException {
        boolean result = false;

        //get LP relax value
        double nodeObjValue = getObjValue();
        
        result = isMaximization  ? 
                    (nodeObjValue < getCutoff()) || (nodeObjValue <= bestKnownGlobalOptimum )  : 
                    (nodeObjValue > getCutoff()) || (nodeObjValue >= bestKnownGlobalOptimum );

        /*if (result) logger.debug(  " Discard node   " + bestKnownGlobalOptimum + " " +getCutoff() +
                " "+ nodeObjValue);*/
        return result;
    }
    
    //can this ILOCLPEX object  be discarded ?
    private boolean canTreeBeDiscarded(  ) throws IloException{     
        
        //check if objective and incumbent values are accurate even if some kids are 
        //not going to be solved here, because they have been migrated.
        //should be okay.
        
        double metric =  getBestObjValue() -bestKnownGlobalOptimum ;
        metric = metric /(EPSILON +bestKnownGlobalOptimum);
        
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        boolean mipHaltCondition =  RELATIVE_MIP_GAP >= Math.abs(metric)  ;
        
        //also halt if we cannot do better than bestKnownGlobalOptimum
        boolean inferiorityHaltConditionMax = isMaximization && 
                                              (bestKnownGlobalOptimum>=getIncumbentObjValue()) && 
                                              (bestKnownGlobalOptimum>=getBestObjValue());
        boolean inferiorityHaltConditionMin = !isMaximization && 
                                               (bestKnownGlobalOptimum<=getIncumbentObjValue()) && 
                                               (bestKnownGlobalOptimum<=getBestObjValue());
         
        /*if (inferiorityHaltConditionMax) logger.debug(  " Discard tree inferiorityHaltConditionMax " + bestKnownGlobalOptimum + " " +getIncumbentObjValue() +
                " "+ getBestObjValue());
        if (inferiorityHaltConditionMin) logger.debug(  " Discard tree inferiorityHaltConditionMin " + bestKnownGlobalOptimum + " " +getIncumbentObjValue() +
                " "+ getBestObjValue());*/
        return  mipHaltCondition || inferiorityHaltConditionMin|| inferiorityHaltConditionMax;       
      
    }
    
    private boolean isSubtreeRoot () throws IloException {
        
        boolean isRoot = true;
        
        if (getNodeData()!=null  ) {
            NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
            if (thisNodeData.getDepthFromSubtreeRoot()>ZERO) {
                
                isRoot = false;
                
            }
        }    
        
        return isRoot;
        
    }
    
    private boolean isChildEasy(){
        //fill up later
        return false;
    }
    
    private Map< IloNumVar,Double > findIntegerBounds (boolean isUpperBound) throws IloException {
        Map< IloNumVar,Double > results = new  HashMap< IloNumVar,Double >();
        IloNumVar[] modelIntVars = this.treeMetaData.getIntvars();
        
        double[] values  = isUpperBound ? getUBs(modelIntVars ): getLBs(modelIntVars);

        for (int index = ZERO ; index <modelIntVars.length; index ++ ){

            results.put( modelIntVars[index] , values[index]) ;
        }
        return results;
    }
    
    private void initLogging(int partitionID) throws Exception{
        
        if (! isLoggingInitialized){
            
            //initialize the LOG4J logger
            logger=Logger.getLogger(this.getClass());
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
            logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER +this.getClass().getSimpleName()+partitionID+ LOG_FILE_EXTENSION ));
            logger.setAdditivity(false);
            isLoggingInitialized=true;
        }
         
    } 
       
    
}
