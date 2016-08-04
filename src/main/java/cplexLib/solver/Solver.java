package cplexLib.solver;

import java.io.IOException;
import java.util.List;

import cplexLib.callbacks.BranchHandler;
import cplexLib.callbacks.NodeHandler;
import cplexLib.dataTypes.SubtreeMetaData;
import cplexLib.dataTypes.NodeAttachment;
import cplexLib.dataTypes.Solution;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex; 
import static cplexLib.constantsAndParams.Parameters.*;
import static cplexLib.constantsAndParams.Constants.*;
import cplexLib.dataTypes.LoggingInitializer;

public class Solver extends LoggingInitializer {
    
    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
    private SubtreeMetaData treeMetaData;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    //and the node handler
    private NodeHandler nodeHandler ;
         
    public Solver (IloCplex cplex , SubtreeMetaData metaData ) throws Exception{
            
        this.cplex=cplex;
        this.  treeMetaData=  metaData;
        
        branchHandler = new BranchHandler(      metaData   );
        nodeHandler = new  NodeHandler (    metaData) ;
        
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);   
        
        setSolverParams();  
    
    }
    
    public void setSolverParams() throws IloException {
        //depth first?
        if ( DEPTH_FIRST_SEARCH) cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, ZERO); 
        
        //MIP gap
        if ( RELATIVE_MIP_GAP>ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, RELATIVE_MIP_GAP);

        //others
    }
    
    public boolean isEntireTreeDiscardable() {
        return this.treeMetaData.isEntireTreeDiscardable();
    }
    
    public IloCplex.Status solve(double timeSliceInSeconds,     double bestKnownGlobalOptimum   ) 
            throws  Exception{
        
        //initialize logging 
        if (! isLoggingInitialized) initLogging(PARTITION_ID);
        
        //can we supply MIP  start along with bestKnownGlobalOptimum ?         
       
        branchHandler.refresh(bestKnownGlobalOptimum);  
       
        cplex.setParam(IloCplex.Param.TimeLimit, timeSliceInSeconds); 
        cplex.solve();
        
        //if (  isLoggingInitialized) logger.info("Result of solving is" + cplex.getStatus());
        return cplex.getStatus();
    }
    
}
