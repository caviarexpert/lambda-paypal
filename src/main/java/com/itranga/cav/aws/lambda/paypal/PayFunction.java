package com.itranga.cav.aws.lambda.paypal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Details;
import com.paypal.api.payments.Item;
import com.paypal.api.payments.ItemList;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.ShippingAddress;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.itranga.aws.lambda.ServerlessInput;
import com.itranga.aws.lambda.ServerlessOutput;
import com.itranga.cav.aws.lambda.paypal.model.InvoiceDAO;
import com.itranga.cav.aws.lambda.paypal.model.PaymentDAO;
import com.itranga.cav.model.postmen.UpuAddress;


/**
 * Lambda function that triggered by the API Gateway event "POST /". It reads all the query parameters as the metadata for this
 * article and stores them to a DynamoDB table. It reads the payload as the content of the article and stores it to a S3 bucket.
 */
public class PayFunction implements RequestHandler<ServerlessInput, ServerlessOutput> {
	private static final Logger LOG = LoggerFactory.getLogger(PayFunction.class);
	private static APIContext apiContext;
	private static String PAYPAL_CLIENT_ID = System.getenv("PAYPAL_CLIENT_ID");
	private static String PAYPAL_CLIENT_SECRET = System.getenv("PAYPAL_CLIENT_SECRET");
	private static String PAYPAL_MODE = System.getenv("PAYPAL_MODE");
	private static String PAYPAL_CANCEL_URL = System.getenv("PAYPAL_CANCEL_URL");
	private static String PAYPAL_RETURN_URL = System.getenv("PAYPAL_RETURN_URL");
	static {
		String mode = PAYPAL_MODE!=null ? PAYPAL_MODE : "sandbox";
		if(PAYPAL_CLIENT_ID!=null && PAYPAL_CLIENT_SECRET!=null){
			apiContext = new APIContext(PAYPAL_CLIENT_ID, PAYPAL_CLIENT_SECRET, mode);
		}else{
			LOG.warn("PayFunction is malconfigured");
		}
	}
	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public ServerlessOutput handleRequest(ServerlessInput serverlessInput, Context context) {
		ServerlessOutput output = new ServerlessOutput();
		try {
			if(apiContext==null) throw new Exception("Paypal API context is NULL");
			Map<String, String> queries = serverlessInput.getQueryStringParameters();
			String json;
			if (queries == null || queries.get("paymentID") == null) {
				String content = serverlessInput.getBody();
				//InvoiceDAO invoiceDao = mapper.readValue(content, InvoiceDAO.class);
				//PaymentDAO newPayment = createPayment(invoiceDao);
				PaymentDAO newPayment = createPayment(new InvoiceDAO());
				json = mapper.writeValueAsString(newPayment);
				//json = "{\"result\":\"OK\"}";
			}else{
				String paymentId = queries.get("paymentID");
				String payerId = queries.get("payerId");
				PaymentDAO confirmPayment = confirmPayment(paymentId, payerId);
				json = mapper.writeValueAsString(confirmPayment);
			}
			output.setStatusCode(200);
			output.setBody(json);
		} catch (Exception e) {
			output.setStatusCode(500);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			output.setBody("{ \"error\" : \""+sw.toString()+"\"}");
		} finally {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			headers.put("Access-Control-Allow-Origin", "*");
			headers.put("Access-Control-Allow-Credentials", "true");
			output.setHeaders(headers);
		}
		return output;
	}
	
	private PaymentDAO createPayment(InvoiceDAO invoiceDao) throws PayPalRESTException{
		Details details = new Details();
		details.setSubtotal("120.00");
		details.setShipping("40.00");
		details.setTax("20.00");

		// ###Amount
		// Let's you specify a payment amount.
		Amount amount = new Amount();
		amount.setCurrency("EUR");
		// Total must be equal to sum of shipping, tax and subtotal.
		amount.setTotal("180");
		amount.setDetails(details);

		// ###Transaction
		// A transaction defines the contract of a
		// payment - what is the payment for and who
		// is fulfilling it. Transaction is created with
		// a `Payee` and `Amount` types
		Transaction transaction = new Transaction();
		transaction.setAmount(amount);
		//The soft descriptor that is used when charging this funding source. If the string's length is greater than the maximum allowed length, the string is truncated.
		// max 22
		transaction.setSoftDescriptor("Caviarexpert.eu store");
		//The invoice number to track this payment. max 127
		//transaction.setInvoiceNumber("3234234");
		//purchase order is number or id specific to this payment max 127
		//transaction.setPurchaseUnitReferenceId("order 12323");
		transaction.setReferenceId("my ref num 1223");
		//max 127 chars
		transaction
				.setDescription("Buying in Caviarexpert.eu");
		//max 255
		transaction.setNoteToPayee("Hi my money reciver!");

		// ### Items
		Item item = new Item();
		item.setName("Red caviar (keta) 200 g").setQuantity("4").setCurrency("EUR").setPrice("30.00");
		ItemList itemList = new ItemList();
		List<Item> items = new ArrayList<Item>();
		items.add(item);
		itemList.setItems(items);
		
		//UpuAddress uaddr = invoice.getShippingAddress();
		//TODO get UpuAddress from Invoice
		UpuAddress uaddr = new UpuAddress();
		uaddr.setAddressee("John Smith");
		uaddr.setDeliveryPoint("Flat 17");
		uaddr.setRoute("Nave street");
		uaddr.setStreetNumber("24");
		uaddr.setLocality("Paris");
		uaddr.setCountryCode("FR");
		uaddr.setPostalCode("75004");
		
		ShippingAddress shippingAddress = new ShippingAddress();
		shippingAddress.setRecipientName(uaddr.getAddressee());
		shippingAddress.setPhone("+33 4564479465");
		//shippingAddress.setLine1("Apartment 2");
		shippingAddress.setLine1(uaddr.getDeliveryPoint());
		//shippingAddress.setLine2("5 Rue Antoine Bourdelle");
		//FIXME according to national starndars (as in UPU)
		shippingAddress.setLine2(uaddr.getStreetNumber()+" "+uaddr.getRoute());
		shippingAddress.setPostalCode(uaddr.getPostalCode());
		shippingAddress.setCity(uaddr.getLocality());
		//shippingAddress.setState(uaddr.getSubLocality());
		shippingAddress.setCountryCode(uaddr.getCountryCode());
		
		itemList.setShippingAddress(shippingAddress);
		
		transaction.setItemList(itemList);
		
		
		// The Payment creation API requires a list of
		// Transaction; add the created `Transaction`
		// to a List
		List<Transaction> transactions = new ArrayList<Transaction>();
		transactions.add(transaction);

		// ###Payer
		// A resource representing a Payer that funds a payment
		// Payment Method
		// as 'paypal'
		Payer payer = new Payer();
		payer.setPaymentMethod("paypal");

		// ###Payment
		// A Payment Resource; create one using
		// the above types and intent as 'sale'
		Payment payment = new Payment();
		payment.setIntent("sale");
		payment.setPayer(payer);
		payment.setTransactions(transactions);
		payment.setNoteToPayer("Note from caviarexpert.eu");

		// ###Redirect URLs
		RedirectUrls redirectUrls = new RedirectUrls();
		String guid = UUID.randomUUID().toString().replaceAll("-", "");
		
		redirectUrls.setCancelUrl(PAYPAL_CANCEL_URL);
		redirectUrls.setReturnUrl(PAYPAL_RETURN_URL);
		payment.setRedirectUrls(redirectUrls);
	
		LOG.debug("Payment: {}", payment.toJSON());
		try {
			Payment createdPayment = payment.create(apiContext);
			LOG.info("Created payment with id = {} and status = {}", createdPayment.getId(), createdPayment.getState());
			// ###Payment Approval Url
			Iterator<Links> links = createdPayment.getLinks().iterator();
			while (links.hasNext()) {
				Links link = links.next();
				if (link.getRel().equalsIgnoreCase("approval_url")) {
					//request.setAttribute("redirectURL", link.getHref());
				}
			}
			//map.put(guid, createdPayment.getId());
			PaymentDAO returnPayment = new PaymentDAO();
			returnPayment.setPaymentID(createdPayment.getId());
			return returnPayment;
		} catch (PayPalRESTException e) {
			throw e;
			//ResultPrinter.addResult(req, resp, "Payment with PayPal", Payment.getLastRequest(), null, e.getMessage());
		}	
	}
	
	private PaymentDAO confirmPayment(String paymentId, String payerId) throws PayPalRESTException{
		LOG.info("Payment {} confirmed", paymentId);
		Payment payment = new Payment();
		payment.setId(paymentId);
		PaymentExecution paymentExecution = new PaymentExecution();
		paymentExecution.setPayerId(payerId);
		Payment createdPayment = payment.execute(apiContext, paymentExecution);
		PaymentDAO paymentDAO = new PaymentDAO();
		//return createdPayment.toJSON();
		paymentDAO.setPaymentID(createdPayment.getId());
		return paymentDAO;
	}
}