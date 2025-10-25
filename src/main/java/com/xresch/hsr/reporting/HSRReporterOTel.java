package com.xresch.hsr.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.xresch.hsr.stats.HSRRecordStats;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecordStats.HSRMetric;

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
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterOTel implements HSRReporter {

	private final PeriodicMetricReader reader;
    private final SdkMeterProvider meterProvider;
    private final MetricExporter metricExporter;

    static final AttributeKey<String> TEST = AttributeKey.stringKey("test");
    static final AttributeKey<String> USECASE = AttributeKey.stringKey("usecase");
    static final AttributeKey<String> PATH = AttributeKey.stringKey("path");
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
					//.setInterval(Metric.ofSeconds(reportingIntervalSeconds))
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
        

        
        meter = openTelemetry.getMeter("hsr");

    }
    
	/****************************************************************************
	 * 
	 ****************************************************************************/
    @Override
    public void reportRecords(ArrayList<HSRRecordStats> records) {
        
        for (HSRRecordStats record : records) {
        	String metricName = record.name().replaceAll("[^A-Za-z0-9_./\\-]", "_");
        	
        	metricName = "hsr_"+metricName;

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
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords, ArrayList<HSRTestSettings> testSettings) {
		// do nothing
		
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
			
	    	for(HSRMetric metric : HSRMetric.values()) {
				
	    		
				DoubleGauge gauge = metricsMap.get(metric);
				
				if(record.hasDataOK()) {
					 BigDecimal value = record.getValue(HSRRecordState.ok, metric);
					 gauge.set(
							   value.longValue()
							 , Attributes.builder()
									   .put(TEST, record.test())
									   .put(USECASE, record.usecase())
									   .put(PATH, record.path())
									   .put(NAME, record.name())
									   .put(METRIC, metric.toString())
									   .put( CODE, record.code())
									   .put(TYPE, record.type().toString())
									   .put(STATUS, HSRRecordState.ok.toString())
									   .build()
							);
				}
				
				if(record.hasDataNOK()) {
					BigDecimal value = record.getValue(HSRRecordState.nok, metric);
					gauge.set(
							   value.longValue()
							 , Attributes.builder()
								   .put(TEST, record.test())
								   .put(USECASE, record.usecase())
								   .put(PATH, record.path())
								   .put(NAME, record.name())
								   .put(METRIC, metric.toString())
								   .put( CODE, record.code())
								   .put(TYPE, record.type().toString())
								   .put(STATUS, HSRRecordState.nok.toString())
								   .build()
							);
				}
								
			}
	    }
	}
}
