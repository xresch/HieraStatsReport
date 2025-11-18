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

	Level minLevel = Level.WARN;
	
	/***************************************************************************
	 * Default Constructor
	 * Uses WARN as the min Level
	 ***************************************************************************/
	public HSRLogInterceptorDefault() {
		
	}
	
	/***************************************************************************
	 * Constructor
	 * 
	 * @param minLevel the minimum level that should be added to the HSR report
	 ***************************************************************************/
	public HSRLogInterceptorDefault(Level minLevel) {
		this.minLevel = minLevel;
	}
	
	/***************************************************************************
	 * Constructor
	 * 
	 * @param minLevel the minimum level that should be added to the HSR report
	 ***************************************************************************/
    @Override
    public FilterReply decide(Marker marker,
                              Logger logger,
                              Level level,
                              String format,
                              Object[] params,
                              Throwable t) {

        //----------------------------------
    	// Check add to HSR
        if (!level.isGreaterOrEqual(minLevel)) {
            return FilterReply.NEUTRAL; // not loggable, nothing to do
        }

        //----------------------------------
    	// FormatMessage
        String formattedMsg = formatMessage(format, params, t);

        //----------------------------------
    	// Add to HSR
        HSR.addLogMessage(level, formattedMsg, t);

        //----------------------------------
    	// Do not block or modify log decision
        return FilterReply.NEUTRAL;  
    }
    
	/***************************************************************************
	 * 
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


