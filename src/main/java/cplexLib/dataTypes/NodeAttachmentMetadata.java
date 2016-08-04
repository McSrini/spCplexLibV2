/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cplexLib.dataTypes;

import static cplexLib.constantsAndParams.Constants.*;
import java.io.Serializable;

/**
 *
 * @author srini
 * 
 * details of node attachment
 * 
 */
public class NodeAttachmentMetadata implements Serializable {
        
    // distance From Original Node    , never changes
    public int distanceFromOriginalRoot =ZERO;   
    //    this is  the depth in the current subtree   , may change to 0 if node is migrated
    public int distanceFromSubtreeRoot=ZERO;  
        
    //easy nodes are close to being solved
    public boolean isEasy = false;
    
    //record time for LP  relaxation in milliseconds
    //we use System.currentTimeMillis() to measure time taken
    public double startTimeFor_LP_Relaxation_millisec= ZERO;
    public double endTimeFor_LP_Relaxation_millisec= ZERO;
    
    //this is the guid of the tree which contains this node
    //not needed in this library, but useful for Spark
    //
    //note the default plcaeholder value, which is overwritten with the subtree ID when an active subtree is asked to return all its leaf node meta data
    public String treeGuid = null;
    
    public String nodeID ;  
    
}
