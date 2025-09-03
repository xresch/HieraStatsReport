package com.xresch.hierastatsreport.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import com.xresch.hierastatsreport.stats.HSRRecordStats;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordState;
import com.xresch.hierastatsreport.stats.HSRRecordStats.RecordMetric;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

/**************************************************************************************************************
 * This reporter writes data to an OpenTelemetry Endpoint.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRReporterOTel implements HSRReporter {

	private final PeriodicMetricReader reader;
    private final SdkMeterProvider meterProvider;
    private final MetricExporter metricExporter;

    static final AttributeKey<String> SIMULATION = AttributeKey.stringKey("simulation");
    static final AttributeKey<String> SCENARIO = AttributeKey.stringKey("scenario");
    static final AttributeKey<String> GROUPS = AttributeKey.stringKey("groups");
    static final AttributeKey<String> NAME = AttributeKey.stringKey("name");
    static final AttributeKey<String> METRIC = AttributeKey.stringKey("metric");
    static final AttributeKey<String> STATUS = AttributeKey.stringKey("status");
    static final AttributeKey<String> TYPE = AttributeKey.stringKey("type");
    static final AttributeKey<String> CODE = AttributeKey.stringKey("code");
    
    // metricPathFull and holder
    private final HashMap<String, OTelMetricsHolder> metricsHolderMap = new HashMap<>();
    
    private  Meter meter;

	/****************************************************************************
	 * 
	 ****************************************************************************/
    public HSRReporterOTel(String otlpEndpointURL, int reportingIntervalSeconds) {
        this.metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(otlpEndpointURL)
                .build();

        reader = 
        		PeriodicMetricReader
					.builder(metricExporter)
					// will be force flushed
					//.setInterval(Duration.ofSeconds(reportingIntervalSeconds))
					.build();
        
        this.meterProvider = 
        		SdkMeterProvider
        			.builder()
        			.registerMetricReader(reader)
        			.build();
        
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider) 
                .build();

        GlobalOpenTelemetry.set(openTelemetry);
        

        
        meter = openTelemetry.getMeter("gatlytron");

    }
    
	/****************************************************************************
	 * 
	 ****************************************************************************/
    @Override
    public void reportRecords(ArrayList<HSRRecordStats> records) {
        
        for (HSRRecordStats record : records) {
        	String metricName = record.getMetricName().replaceAll("[^A-Za-z0-9_./\\-]", "_");
        	
        	metricName = "gtron_"+metricName;

        	if( ! metricsHolderMap.containsKey(metricName) ) {
        		metricsHolderMap.put(metricName, new OTelMetricsHolder(meter, metricName) );
        	}
        	
        	OTelMetricsHolder holder = metricsHolderMap.get(metricName);
        	holder.addValues(record);
        	
        }
       
        reader.forceFlush();

    }

    
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	private class OTelMetricsHolder {
		
	    // metricName and related Gauge
	    private final HashMap<String, DoubleGauge> metricsMap = new HashMap<>();
	    
	    /********************************************************
	     * 
	     ********************************************************/
		public OTelMetricsHolder(Meter meter, String metricName) {
			
	        for(String metric : HSRRecordStats.metricNames) {
	        	
	        	String description = metric+" response time";
	        	String unit = "ms";
	        	if(metric.equals("count")) { 
	        		description = "number of requests";
	        		unit = "count";
	        	}else if(metricName.startsWith("users")) {
	        		description = "number of users";
	        		unit = "count";
	        	}
	        	
	        	DoubleGauge gauge = 
	        			meter.gaugeBuilder(metricName+"_"+metric)
			                .setDescription(description)
			                .setUnit(unit)
			                .build();
	        	
	        	metricsMap.put(metric, gauge);
	        	
	        }

		}
		
	    /********************************************************
	     * 
	     ********************************************************/
	    public void addValues(HSRRecordStats record) {
			
	    	for(RecordMetric metric : RecordMetric.values()) {
				
	    		
				DoubleGauge gauge = metricsMap.get(metric);
				
				if(record.hasDataOK()) {
					 BigDecimal value = record.getValue(HSRRecordState.ok, metric);
					 gauge.set(
							   value.longValue()
							 , Attributes.builder()
									   .put(SIMULATION, record.getSimulation())
									   .put(SCENARIO, record.getScenario())
									   .put(GROUPS, record.getGroupsPath())
									   .put(NAME, record.getMetricName())
									   .put(METRIC, metric.toString())
									   .put( CODE, record.getCode())
									   .put(TYPE, record.getType().threeLetters())
									   .put(STATUS, HSRRecordState.ok.toString())
									   .build()
							);
				}
				
				if(record.hasDataNOK()) {
					BigDecimal value = record.getValue(HSRRecordState.nok, metric);
					gauge.set(
							   value.longValue()
							 , Attributes.builder()
								   .put(SIMULATION, record.getSimulation())
								   .put(SCENARIO, record.getScenario())
								   .put(GROUPS, record.getGroupsPath())
								   .put(NAME, record.getMetricName())
								   .put(METRIC, metric.toString())
								   .put( CODE, record.getCode())
								   .put(TYPE, record.getType().threeLetters())
								   .put(STATUS, HSRRecordState.nok.toString())
								   .build()
							);
				}
								
			}
	    }
	}
}
