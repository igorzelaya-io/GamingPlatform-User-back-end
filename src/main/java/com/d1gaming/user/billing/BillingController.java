package com.d1gaming.user.billing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.transaction.D1Transaction;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping( value = "/billing")
@PreAuthorize("permitAll()")
public class BillingController {

	@Autowired
	private BillingService billingService;
	
	@PreAuthorize("hasRole('PLAYER')")
	@PostMapping( value = "/pay", params = "paymentSum")
	public ResponseEntity<?> makePayment(@RequestParam(required = true) String paymentSum){
		Map<String, Object>  payment = billingService.createPayment(paymentSum);
		if(payment.isEmpty()) {
			return new ResponseEntity<>(payment, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(payment, HttpStatus.OK);
	}
	
	@PreAuthorize("hasRole('PLAYER')")
	@PostMapping(value = "/complete", params="userId")
	public ResponseEntity<?> completePayment(HttpServletRequest request) throws InterruptedException, ExecutionException{
		Map<String, Object> map = new HashMap<>();
		map = billingService.completePayment(request);
		if(map.isEmpty()) {
			return new ResponseEntity<>(map, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
	
	@PostMapping(value = "/save", params= "userId")
	@PreAuthorize("hasRole('PLAYER')")
	public ResponseEntity<?> savePayment(@RequestParam(required = true)String userId, @RequestBody D1Transaction transaction) throws InterruptedException, ExecutionException{
		String response = billingService.addPaymentToUser(userId, transaction);
		if(response.equals("User not found")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
