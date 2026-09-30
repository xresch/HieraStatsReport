package com.xresch.hsr.utils;

import org.slf4j.Marker;

import org.slf4j.helpers.MessageFormatter;

import com.xresch.hsr.base.HSR;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/***************************************************************************
 * A default Log interceptor, that adds logs handled by logback to the 
 * HSR reporting as messages and exceptions.
 * 
 * License: EPL-License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRLogInterceptorDefault extends TurboFilter {

	Level minLevelMessageReporting = Level.WARN;
	Level minLevelLogReporting = Level.INFO;
	
	/***************************************************************************
	 * Default Constructor
	 * Uses WARN as the min Level
	 ***************************************************************************/
	public HSRLogInterceptorDefault() {
		
	}
	
	/***************************************************************************
	 * Constructor
	 * 
	 * @param minLevelMessageReporting the minimum level that should be added to the HSR report as Messages
	 * @param minLevelLogReporting the minimum level that should be reported as logs
	 ***************************************************************************/
	public HSRLogInterceptorDefault(Level minLevelMessageReporting, Level minLevelLogReporting) {
		this.minLevelMessageReporting = minLevelMessageReporting;
		this.minLevelLogReporting = minLevelLogReporting;
	}
	
	/***************************************************************************
	 * Check if the minimum level for logging is reached, if true, add the 
	 * log message to the HSR report.
	 ***************************************************************************/
    @Override
    public FilterReply decide(Marker marker,
                              Logger logger,
                              Level level,
                              String format,
                              Object[] params,
                              Throwable t) {

        //================================================
    	// Check add to HSR as Message
    	//================================================
    	String formattedMsg = formatMessage(format, params, t);
        if ( level.isGreaterOrEqual(minLevelMessageReporting) ) {
            HSR.addLogMessage(level, formattedMsg, t);
        }

        //================================================
    	// Report Logs
    	//================================================
        if ( level.isGreaterOrEqual(minLevelLogReporting) ) {
            HSR.addLogStatement(level, formattedMsg, t);
        }

        //----------------------------------
    	// Do not block or modify log decision
        return FilterReply.NEUTRAL;  
    }
    
	/***************************************************************************
	 * Format the message.
	 ***************************************************************************/
    private String formatMessage(String format, Object[] params, Throwable t) {
        if (format == null) return null;

        if (params == null) {
            return format; // no params → raw format
        }

        // SLF4J-compatible formatter
        return MessageFormatter.arrayFormat(format, params).getMessage();
    }
}


