package com.example.cake.phukettaxim;

@SuppressWarnings("serial")
public class OmiseException extends Exception {
	public OmiseException(String cause){
		super(cause);
	}
	public OmiseException(Exception e){
		super(e);
	}
}
