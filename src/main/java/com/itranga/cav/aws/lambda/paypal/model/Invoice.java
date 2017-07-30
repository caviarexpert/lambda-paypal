package com.itranga.cav.aws.lambda.paypal.model;

import java.util.Map;

import com.itranga.cav.model.postmen.Quotation;
import com.itranga.cav.model.postmen.UpuAddress;

public class Invoice {
	
	private String id;
	private UpuAddress shippingAddress;
	private Quotation shippingQuotation;
	//sku, quantity
	private Map<String, java.math.BigDecimal> cart;
	private String paymentId;

	public UpuAddress getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(UpuAddress shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

	public Quotation getShippingQuotation() {
		return shippingQuotation;
	}

	public void setShippingQuotation(Quotation shippingQuotation) {
		this.shippingQuotation = shippingQuotation;
	}

	public Map<String, java.math.BigDecimal> getCart() {
		return cart;
	}

	public void setCart(Map<String, java.math.BigDecimal> cart) {
		this.cart = cart;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	
}
