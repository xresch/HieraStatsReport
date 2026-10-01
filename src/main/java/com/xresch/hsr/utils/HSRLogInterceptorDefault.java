package com.xresch.hsr.utils;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import com.xresch.hsr.base.HSR;
import com.xresch.hsr.database.HSRDBInterface.LogStatement;
import com.xresch.hsr.stats.HSRStatsEngine;
import com.xresch.xrutils.base.XR;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.FilterReply;

/***************************************************************************
 * A default Log interceptor, that adds logs handled by logback to the HSR
 * reporting as messages and exceptions.
 * 
 * License: EPL-License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRLogInterceptorDefault extends TurboFilter {

	Level minLevelMessageReporting = Level.WARN;
	Level minLevelLogReporting = Level.INFO;

	private static TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(40);

	/***************************************************************************
	 * Constructor
	 * 
	 * @param minLevelMessageReporting the minimum level that should be added to the
	 *                                 HSR report as Messages
	 * @param minLevelLogReporting     the minimum level that should be reported as
	 *                                 logs
	 ***************************************************************************/
	public HSRLogInterceptorDefault(Level minLevelMessageReporting, Level minLevelLogReporting) {
		this.minLevelMessageReporting = minLevelMessageReporting;
		this.minLevelLogReporting = minLevelLogReporting;
		
		registerHSRLogAppender(minLevelLogReporting);
	}

	/***************************************************************************
	 * Check if the minimum level for logging is reached, if true, add the log
	 * message to the HSR report.
	 ***************************************************************************/
	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

		// ================================================
		// Check add to HSR as Message
		// ================================================
		String formattedMsg = formatMessage(format, params, t);
		if (level.isGreaterOrEqual(minLevelMessageReporting)) {
			HSR.addLogMessage(level, formattedMsg, t);
		}

//		// ================================================
//		// Report Logs
//		// ================================================
//		if (level.isGreaterOrEqual(minLevelLogReporting)) {
//			HSR.addLogStatement(level, formattedMsg, t);
//		}

		// ----------------------------------
		// Do not block or modify log decision
		return FilterReply.NEUTRAL;
	}

	/***************************************************************************
	 * Format the message.
	 ***************************************************************************/
	private String formatMessage(String format, Object[] params, Throwable t) {
		if (format == null)
			return null;

		if (params == null) {
			return format; // no params → raw format
		}

		// SLF4J-compatible formatter
		return MessageFormatter.arrayFormat(format, params).getMessage();
	}

	/***************************************************************************
	 * Format the message.
	 ***************************************************************************/
	public static void registerHSRLogAppender(Level minLevelLogReporting) {

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {

			@Override
			protected void append(ILoggingEvent event) {
				
				Level level = event.getLevel();
				//---------------------------------
				// Get Stacktrace
				if ( ! level.isGreaterOrEqual(minLevelLogReporting) ) {
					return;
				}

				//---------------------------------
				// Get Stacktrace
				// Force Logback to calculate the actual caller
				StackTraceElement[] callerData = event.getCallerData();
				
				String formattedMsg = event.getFormattedMessage();
				
				
				IThrowableProxy throwableProxy = event.getThrowableProxy();

				String stacktrace = null;
				try {
					if(throwableProxy != null) {
						 String exceptionClassName = throwableProxy.getClassName();
						 
						 //----------------------------------
						 // this is such an ugly approach
						 // because if this proxy class, I now have to do a conversion
						 StackTraceElementProxy[] proxyStack = throwableProxy.getStackTraceElementProxyArray();
						 
						 StackTraceElement[] expectionStacktrace = new StackTraceElement[proxyStack.length];
						 for(int i = 0; i < proxyStack.length; i++) {
							 expectionStacktrace[i] = proxyStack[i].getStackTraceElement();
						 }
						 
						 String newline = "\n\t";
						 String shortStacktrace = XR.Text.shortStacktrace(expectionStacktrace, newline);
						 
						 
						 stacktrace = exceptionClassName 
								 		+ ":"
								 		+ newline 
								 		+ shortStacktrace 
								 		;
					}
				}catch(Throwable t) {
					System.out.println("Error while creating Exception Stacktrace: "+t.getMessage());
				}
				
				

				//---------------------------------
				// Get Log Source
				StackTraceElement caller = callerData.length > 0 ? callerData[0] : null;

				String logSource = null;
				if (caller != null) {
					String className = caller.getClassName();
					String abbreviatedClass = abbreviator.abbreviate(className);
					String methodName = caller.getMethodName();
					int lineNumber = caller.getLineNumber();
					
					logSource = abbreviatedClass + "." + methodName + ":" + lineNumber;
				}
				
				//---------------------------------
				// Add Log to HSR
				LogStatement statement = new LogStatement(
							  event.getInstant().toEpochMilli()
							, HSRStatsEngine.getHostname()
							, level.toString()
							, logSource
							, formattedMsg
							, stacktrace
						);

				HSRStatsEngine.addLogStatement(statement);

			}
		};

		appender.setContext(context);
		appender.setName("HSR_LOG_APPENDER");
		appender.start();

		rootLogger.detachAppender("HSR_LOG_APPENDER");
		rootLogger.addAppender(appender);
	}
}
