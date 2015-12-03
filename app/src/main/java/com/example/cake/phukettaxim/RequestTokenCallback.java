package com.example.cake.phukettaxim;
public interface RequestTokenCallback extends OmiseCallback{
	
	/**
	 * Its call if token succeeded to get.
	 * @param token
	 */
	public void onRequestSucceeded(final Token token);
	
	/**
	 * Its call if token failed to get.
	 * @param errorCode is define in co.omise.OmiseCallback
	 */
	public void onRequestFailed(final int errorCode);
}
