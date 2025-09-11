package com.xresch.hsr.utils;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**************************************************************************************************************
 * Just a simple log helper class that provides features that should have been implemented by the framework
 * in the first place.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 * 
 **************************************************************************************************************/
public class HSRLog {
	
    public static void log(Logger logger, Level level, String message) {
        switch (level.levelInt) {
            case Level.TRACE_INT -> logger.trace(message);
            case Level.DEBUG_INT -> logger.debug(message);
            case Level.INFO_INT  -> logger.info(message);
            case Level.WARN_INT  -> logger.warn(message);
            case Level.ERROR_INT -> logger.error(message);
            default -> logger.info(message); // fallback
        }
    }
}


