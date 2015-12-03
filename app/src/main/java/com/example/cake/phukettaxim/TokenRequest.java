package com.example.cake.phukettaxim;

/**
 * Parameter container class
 */
public class TokenRequest {
	private String publicKey = null;
	private Card card = null;

	public TokenRequest(){}
	public TokenRequest(String publicKey) {
		this.publicKey = publicKey;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	public Card getCard() {
		return card;
	}
	public void setCard(Card card) {
		this.card = card;
	}
}
