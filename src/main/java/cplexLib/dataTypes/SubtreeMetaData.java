package cplexLib.dataTypes;

import cplexLib.callbacks.BranchHandler;
import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.LOG_FOLDER;
import static cplexLib.constantsAndParams.Parameters.PARTITION_ID;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;

import java.util.*;
import java.util.Map.Entry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * This object holds all the meta data associated with an ActiveSubtree.
 * Since we cannot traverse the ILOCPLEX object at will, we store relevant information here.
 *
 */
public class SubtreeMetaData {
     
    //GUID used to identify the ActiveSubtree
    private final String guid ;
    
    //keep note of the root Node Attachment used to create this subtree
    private final NodeAttachment rootNodeAttachment ;
        
    //We keep track of the child nodes that were spawned, but not yet picked up for solving.
    //These are the nodes that are available for farming.
    //If  a node is selected from this list for migration, then it must be removed from this list since it will be solved outside this subtree.   
    //The map key is the node ID
    private Map<String, NodeAttachment> leafNodesPendingSolution = new HashMap<String, NodeAttachment>();
       
    //When a node is picked up in the solve callback from the list leafNodesPendingSolution, the node should be moved into the list 'nodeBeingSolved'
    //Note that this map will always have <= 1 entry.  Initially when we are solving the subtree root , this list
    // has no entry. After that , any child node picked up for solving is inserted here.
    private Map<NodeId, NodeAttachment> nodeBeingSolved = new HashMap<NodeId, NodeAttachment>();
    
    //note that this tree has been solved to completion if these conditions are met:
    // 1) the status is = optimal OR infeasible OR unbounded OR error
    // 2) the node callback went looking for kids to solve, and found that all kids were migrated away (i.e. leafNodesPendingSolution is empty)
    // private boolean allKidsMigratedAway  = false;  
     
    //not sure what CPLEX does when the LP relaxation is integer feasible, and option has been set to find all optimal solutions
    //In such cases, we will stop solving this node, whereas CPLEX may have gone on to find other optimal solutions
    
    //sometimes we find that the entire subtree can be discarded, because it cannot beat the incumbent 
    private boolean canDiscardEntireSubTree  = false;  
    
    //keep note of all the INT variables in the model
    // these are used  to find bound tightenings when spawning kids.
    private final IloNumVar[] intVars ; 
        
    
    public SubtreeMetaData( NodeAttachment attachment, IloNumVar[] intVars){
        guid = UUID.randomUUID().toString();
        rootNodeAttachment=attachment;
        this.intVars= intVars;
    }
    
    public String getGUID(){
        return this.guid;
    }
    
    public IloNumVar[]   getIntvars (){
        return intVars;
    }
    
    public NodeAttachment getRootNodeAttachment(){
        return rootNodeAttachment;
    }
    
   
    public void addUnsolvedLeafNodes (String nodeID, NodeAttachment attachment) {
        leafNodesPendingSolution.put(nodeID , attachment);
    }
    
    public NodeAttachment removeUnsolvedLeafNode (String nodeID) {        
        return leafNodesPendingSolution.remove(nodeID);
    }
    
    public NodeAttachment removeUnsolvedLeafNode ( ) {   
        List <String> nodeList = new ArrayList <String>();
        nodeList.addAll(        leafNodesPendingSolution.keySet());
        //logger.info("farming out node "+nodeList.get(ZERO) + " from tree " + this.guid);
        return leafNodesPendingSolution.remove(nodeList.get(ZERO));
    }
    
    
    public List<NodeAttachment> removeUnsolvedLeafNodes ( int count) {        
        List <String> nodeList = new ArrayList <String>();
        nodeList.addAll(        leafNodesPendingSolution.keySet());
         
        List <NodeAttachment> farmedOutNodes = new ArrayList <NodeAttachment>(); 
        while (count > ZERO && count <=nodeList.size()) {
            farmedOutNodes.add(removeUnsolvedLeafNode(nodeList.get(count-ONE)));
            count = count -ONE;
        }
        return farmedOutNodes;
    }
    
    public Map<String, NodeAttachment> getLeafNodesPendingSolution () {
        return Collections.unmodifiableMap(leafNodesPendingSolution);
    }
    
    public Map<String, NodeAttachmentMetadata> getMetadataForLeafNodesPendingSolution () {
        Map<String, NodeAttachmentMetadata> map = new HashMap<String, NodeAttachmentMetadata> ();
        for(Entry<String , NodeAttachment> entry:leafNodesPendingSolution.entrySet()){
            NodeAttachmentMetadata metadata =entry.getValue().nodeMetadata;
            metadata.treeGuid = this.getGUID();
            map.put(entry.getKey(),metadata );
        }
        
        return Collections.unmodifiableMap(map);
    }
    
    public void setEntireTreeDiscardable() {
        this.canDiscardEntireSubTree= true;
    }
    
    public boolean isEntireTreeDiscardable() {
        return this.canDiscardEntireSubTree ;
    }
    
    public void setNodeBeingSolved(NodeId nodeID, NodeAttachment attachment) {
        nodeBeingSolved.clear();
        nodeBeingSolved.put(nodeID, attachment);
    }
}
