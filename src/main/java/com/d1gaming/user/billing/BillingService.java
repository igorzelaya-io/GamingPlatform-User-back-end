package com.d1gaming.user.billing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.d1gaming.library.transaction.D1Transaction;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

@Service
public class BillingService {
	
	@Value("d1gaming.app.clientId")
	private String clientId = "";
	
	@Value("d1gaming.app.clientSecret")
	private String clientSecret = "";
	
	@Autowired
	private Firestore firestore;
	
	public Map<String, Object> createPayment(String sum){
		Map<String, Object> response = new HashMap<>();
		
		Amount amount = new Amount();
		amount.setCurrency("USD");
		amount.setTotal(sum);
		
		Transaction transaction = new Transaction();
		transaction.setAmount(amount);
		
		Payer payer = new Payer();
		payer.setPaymentMethod("paypal");
	
		Payment payment = new Payment();
		payment.setIntent("sale");
		payment.setPayer(payer);
		payment.setTransactions(Arrays.asList(transaction));
		
		RedirectUrls redirectUrls = new RedirectUrls();
//		Validate URLS. 
		redirectUrls.setCancelUrl("http://localhost:4200/checkout");
		redirectUrls.setReturnUrl("http://localhost:4200/checkout");
		
		payment.setRedirectUrls(redirectUrls);
		
		Payment createdPayment = null;
		try {
			String redirectUrl = " ";
			APIContext context = new APIContext(this.clientId, this.clientSecret, "sandbox");
			createdPayment = payment.create(context);
			if(createdPayment != null) {
				List<Links> linksLs = createdPayment.getLinks();
				for(Links link : linksLs) {
					if(link.getRel().equals("approval_url")) {
						redirectUrl = link.getHref();
						break;
					}
				}
				response.put("status","success");
				response.put("redirect_url", redirectUrl);
				
			}
		}
		catch(PayPalRESTException e) {
			System.out.println("Error happened during payment creation!");
		}
		return response;
	}
	
	public Map<String, Object> completePayment(HttpServletRequest request) throws InterruptedException, ExecutionException{
		Map<String, Object> response = new HashMap<>();
		Payment payment = new Payment();
		payment.setId(request.getParameter("paymentId"));
		
		PaymentExecution paymentExecution = new PaymentExecution();
		paymentExecution.setPayerId(request.getParameter("payerId"));
		try {
			APIContext context = new APIContext(this.clientId, this.clientSecret, "sandbox");
			Payment createdPayment = payment.execute(context, paymentExecution);
			if(createdPayment != null){
				response.put("status", "success");
				response.put("paymnet", createdPayment);
			}
		}
		catch(PayPalRESTException e) {
			System.err.println(e.getDetails());
		}
		return response;
	}
	
	public String addPaymentToUser(String userId, D1Transaction transaction) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		if(!userReference.get().get().exists()) {
			return "User not found.";
		}
		DocumentReference reference = firestore.collection("users").document(userId).collection("userBilling").add(transaction).get();
		String transactionId = reference.getId();
		WriteBatch batch = firestore.batch();
		batch.update(reference, "transactionId", transactionId);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
		WriteResult transactionCreation = firestore.collection("transactions").document(transactionId).set(transaction).get();
		System.out.println("Transaction created at: " + transactionCreation.getUpdateTime());
		return "Transaction added Successfully.";
	}
}