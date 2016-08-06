/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cplexLib.dataTypes;

import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author srini
 */
public abstract class LoggingInitializer {
    protected Logger logger ;
    protected boolean isLoggingInitialized =false ;
    protected void initLogging(int partitionID) throws Exception{
        
        if (! isLoggingInitialized){
            
            //initialize the LOG4J logger
            logger=Logger.getLogger(this.getClass());
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n"); 
            RollingFileAppender rollingFileAppender = new RollingFileAppender(layout,LOG_FOLDER +this.getClass().getSimpleName()+partitionID+ LOG_FILE_EXTENSION );
            rollingFileAppender.setAppend(false);
             
            logger.addAppender(rollingFileAppender);
            logger.setAdditivity(false);
            
            isLoggingInitialized=true;
        }
         
    }
}
