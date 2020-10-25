package com.d1gaming.user;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
	
	@Autowired
	UserService userServ;
	
	@GetMapping(value = "/users", params="userName")
	public ResponseEntity<Object> getUserByName(@RequestParam(value = "userName", required = true)final String userName) throws InterruptedException, ExecutionException{
		if(userName == null) {
			return new ResponseEntity<>("Invalid Input",HttpStatus.BAD_REQUEST);
		}
		User user = userServ.getUserById(userName);
		if(user == null) {
			return new ResponseEntity<>("User Not Found", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(user,HttpStatus.OK);
	}
	
	@GetMapping("/users")
	public ResponseEntity<List<User>> getAllUsers() throws InterruptedException, ExecutionException{
		List<User> ls = userServ.getAllUsers();
		if(ls.isEmpty()) {
			return new ResponseEntity<List<User>>(ls, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<User>>(ls, HttpStatus.OK);
	}
	
	@PostMapping("/users")
	public ResponseEntity<Object> saveUser(@RequestBody User user) throws InterruptedException, ExecutionException{
		String response = userServ.saveUser(user);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	@DeleteMapping(value = "/users", params="userId")
	public ResponseEntity<Object> deleteUserById(@RequestParam(value="userId", required = true)String userId, @RequestParam(required = false, value="userField") String userField) throws InterruptedException, ExecutionException{
		if(userField != null) {
			String response = userServ.deleteUserField(userId, userField);
			if(response.equals("User not found.")) {
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);			
			}
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		String response = userServ.deleteUserById(userId);
		if(response.equals("User not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "/users")
	public ResponseEntity<Object> updateUser(@RequestBody User user) throws InterruptedException, ExecutionException{
		String response = userServ.updateUser(user);
		if(response.equals("User not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "/users", params="userId")
	public ResponseEntity<Object> updateUserField(@RequestParam(required = true, value="userId")String userId, @RequestParam(required = true)String userField, @RequestParam(required = true)String replaceValue ) throws InterruptedException, ExecutionException{
		String response = userServ.updateUserField(userId, userField, replaceValue);
		if(response.equals("User not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		else if(response.equals("This field cannot be updated.")) {
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
