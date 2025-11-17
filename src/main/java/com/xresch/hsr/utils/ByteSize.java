package com.xresch.hsr.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**************************************************************************************************************
 * Enum used for calculating bytes.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public enum ByteSize {
	
	  B(1L)
	, KB(1024L)
	, MB(1_048_576L)
	, GB(1_073_741_824L)
	, TB(1_099_511_627_776L)
	;

	public final long bytesCount;
	
	/*******************************************************************
	 * Private Constructor
	 * 
	 *******************************************************************/
	private ByteSize(long bytesCount) {
		this.bytesCount = bytesCount;
	}
	
	/*******************************************************************
	 * Converts an amount of bytes to the chosen enum amount.
	 * 
	 * @param bytes the size in bytes
	 * 
	 *******************************************************************/
	public long convert(Long bytes){
		return bytes / bytesCount;
	}
	
	/*******************************************************************
	 * Converts an amount of bytes with the choosen decimal precision
	 * to the chosen enum amount.
	 * 
	 * @param bytes the size in bytes
	 * @param decimals the number of decimals
	 * 
	 *******************************************************************/
	public BigDecimal convert(long bytes, int decimals){
		return new BigDecimal( bytes / (bytesCount * 1.0) )
					.setScale(decimals, RoundingMode.HALF_UP)
				;
	}
	
	/*******************************************************************
	 * Converts a size of bytes into a bytes String with a suffix like
	 * KB, MB, GB or TB that can be read by a human being.
	 * 
	 * @param sizeBytes the size in bytes
	 * @param decimals the number of decimals
	 * 
	 *******************************************************************/
	public static String toHumanReadableBytesAuto(Long sizeBytes, int decimals){

		if(sizeBytes == null) { return "0 B"; }
		
	    String readable = null;

	    DecimalFormat dec = new DecimalFormat("0."+ "0".repeat(decimals) );

	    if ( 		sizeBytes >= ByteSize.TB.bytesCount ){	readable = dec.format( ByteSize.TB.convert(sizeBytes) ).concat(" TB");
	    } else if ( sizeBytes >= ByteSize.GB.bytesCount ){ 	readable = dec.format( ByteSize.GB.convert(sizeBytes) ).concat(" GB");
	    } else if ( sizeBytes >= ByteSize.MB.bytesCount ){	readable = dec.format( ByteSize.MB.convert(sizeBytes) ).concat(" MB");
	    } else if ( sizeBytes >= ByteSize.KB.bytesCount ){ 	readable = dec.format( ByteSize.KB.convert(sizeBytes) ).concat(" KB");
	    } else { 											readable = sizeBytes+" B";	}

	    return readable;

}
}
