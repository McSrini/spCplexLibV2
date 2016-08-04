package cplexLib.dataTypes;
 
import java.io.IOException; 
 

import java.util.List;
import java.util.UUID;

import cplexLib.solver.Solver; 
import cplexLib.utilities.UtilityLibrary;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;
import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.PARTITION_ID;
import static cplexLib.constantsAndParams.Parameters.SAV_FILENAME;
import java.util.ArrayList;
import java.util.Map;

/**
 * 
 * @author Srini
 * 
 * This class is a wrapper around an ILO-CPLEX subtree being solved
 * 
 * note that this object is NOT SERIALIZABLE
 *
 */

public class ActiveSubtree extends LoggingInitializer {
  
    //the CPLEX object representing this partially solved tree 
    private  IloCplex cplex ;
    
    //meta data about the IloCplex object
    private SubtreeMetaData treeMetaData  ;
        
    //a solver object that is used to solve this tree few seconds at a time 
    private Solver solver ;    
    
    //a flag to indicate if end() has been called on this sub tree
    //Use this method to deallocate memory once this subtree is no longer needed
    private boolean ended = false;

    //Constructor
    public ActiveSubtree (  NodeAttachment attachment) throws  Exception  {
        
        //initialize the CPLEX object
        cplex= new IloCplex();   
        cplex.importModel(SAV_FILENAME);
        UtilityLibrary.merge(cplex, attachment); 
        
        IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        treeMetaData = new SubtreeMetaData(   attachment, lp.getNumVars());
        
        //get ourselves a solver
        solver = new Solver( cplex   , treeMetaData);
        
        
            
    }
    
    public void end(){
        if (!ended) cplex.end();
        ended=true;
    }
    
    public boolean hasEnded(){
        return         ended ;
    }
    
    public long getNumLeafNodes (boolean easy) {
        long count = ZERO;
        for ( NodeAttachmentMetadata nodeMetaData : this.treeMetaData.getMetadataForLeafNodesPendingSolution().values()){
            if (nodeMetaData.isEasy == easy) count++;
        }
        return count;
    }
    
    
    public String getGUID(){
        return this.treeMetaData.getGUID();
    }
    
    /**
     * 
     * Solve this subtree for some time
     * Subtree meta data will be updated by the solver.
     */
    public IloCplex.Status solve ( double timeSliceInSeconds,         double bestKnownGlobalOptimum )
            throws  Exception {
        
        //initialize logging 
        if (! isLoggingInitialized) initLogging(PARTITION_ID);
        
        logger.info("Solving subtree " + this.getGUID() + " for " + timeSliceInSeconds + " seconds.");
        //solve for some time
        IloCplex.Status  status = solver.solve( timeSliceInSeconds, bestKnownGlobalOptimum );
        logger.info("Solved subtree " + this.getGUID() + " status is " + this.getStatusString());
        return status;
        
    }
 
    public boolean isSolvedToCompletion() throws Exception {
        return   this.isOptimal()||this.isInError()   ||this.isUnFeasible()||this.isUnbounded();
        
    }
    
    public boolean isDiscardable() {
        //can we check the cutoff of the ILO-CPLEX object , and use the best known global optimum, in 
        //addition to this flag?
        return this.treeMetaData.isEntireTreeDiscardable();
    }
    
    public String toString(){
        String details =this.treeMetaData.getGUID() +NEWLINE;
        details += this.treeMetaData.getRootNodeAttachment().toString();
        return details;
        
    }
    
    public Solution getSolution () throws IloException {
        Solution soln = new Solution () ;
        
        soln.setError(isInError());
        soln.setOptimal(isOptimal());
        soln.setFeasible(isFeasible() );
        soln.setUnbounded(isUnbounded());
        soln.setUnFeasible(isUnFeasible());
        
        soln.setOptimumValue(getObjectiveValue());
        
        if (isOptimalOrFeasible()) UtilityLibrary.addVariablevaluesToSolution(cplex, soln);
        
        return soln;
    }
    
    public int getPendingChildNodeCount () {
        return this.treeMetaData.getLeafNodesPendingSolution().size();
    }
    
    public  Map<String, NodeAttachment>  getPendingChildNodes () {
        return this.treeMetaData.getLeafNodesPendingSolution();
    }
    
    public Map<String, NodeAttachmentMetadata> getMetadataForLeafNodesPendingSolution () {
        return this.treeMetaData.getMetadataForLeafNodesPendingSolution();
    }
    
    public NodeAttachment removeUnsolvedLeafNode (String nodeID) {        
        return this.treeMetaData.removeUnsolvedLeafNode(nodeID);
    }
    
    //farm out nodes until threshold of them are left
    public List <NodeAttachment> farmOutNodes (int threshold) {
         List <NodeAttachment> farmedOutNodes = new ArrayList <NodeAttachment>(); 
         while (getPendingChildNodeCount() > threshold) {
             farmedOutNodes.add(this.treeMetaData.removeUnsolvedLeafNode( ));
         }
         return farmedOutNodes;
    }
    
    public boolean isFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Feasible) ;
    }
    
    public boolean isUnFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Infeasible) ;
    }
    
    public boolean isOptimal() throws IloException {
        
        return cplex.getStatus().equals(IloCplex.Status.Optimal) ;
    }
    public boolean isOptimalOrFeasible() throws IloException {
        return isOptimal()|| isFeasible();
    }
    public boolean isUnbounded() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Unbounded) ;
    }
    
    public boolean isInError() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Error) ;
    }
  
    public double getObjectiveValue() throws IloException {
        double inferiorObjective = isMaximization?  MINUS_INFINITY:PLUS_INFINITY;
        return isFeasible() || isOptimal() ? cplex.getObjValue():inferiorObjective;
    }
        
    public String getStatusString () throws Exception{
        String status = "Unknown";
        if (isUnFeasible())   status =      "Infeasible";
        if (isFeasible()) status = "Feasible";
        if (isOptimal()) status = "optimal.";            
        if (isInError()) status = "error.";       
        if (isUnbounded()) status = "unbounded.";  
        if (this.isDiscardable()) status += " and also discardable.";  
        return status;
    }    
}
