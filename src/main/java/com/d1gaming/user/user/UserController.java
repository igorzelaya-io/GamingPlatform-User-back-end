package com.d1gaming.user.user;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.request.UserTokenRequest;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.user.User;

@RestController
@RequestMapping("/userapi")
@CrossOrigin(origins = "localhost:4200")
@PreAuthorize("permitAll()")
public class UserController {
	
	@Autowired
	UserService userServ;
		
	@GetMapping(value = "/users/search",params="userName")
	public ResponseEntity<User> getUserByName(@RequestParam(value = "userName", required = true)final String userName) throws InterruptedException, ExecutionException{
		User user = userServ.getUserByName(userName);
		if(user == null) {
			return new ResponseEntity<User>(user, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<User>(user,HttpStatus.OK);
	}
	
	@GetMapping(value= "/users/search", params="userId")
	public ResponseEntity<User> getUserByID(@RequestParam(required = true)String userId) throws InterruptedException, ExecutionException{
		User searchedUser = userServ.getUserById(userId);
		if(searchedUser != null) {
			return new ResponseEntity<User>(searchedUser, HttpStatus.OK);
		}
		return new ResponseEntity<User>(searchedUser, HttpStatus.NOT_FOUND);
	}
	
	@GetMapping("/users")
	public ResponseEntity<List<User>> getAllUsers() throws InterruptedException, ExecutionException{
		List<User> ls = userServ.getAllUsers();
		if(ls.isEmpty()) {
			return new ResponseEntity<List<User>>(ls, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<User>>(ls, HttpStatus.OK);
	}
		
	@DeleteMapping(value = "/users/delete",params="userId")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> deleteUserById(@RequestParam(value="userId", required = true)String userId, 
												 @RequestParam(required = false, value="userField") String userField) throws InterruptedException, ExecutionException{
		if(userField != null) {
			String response = userServ.deleteUserField(userId, userField);
			if(response.equals("User not found.")) {
				return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);			
			}
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
		String response = userServ.deleteUserById(userId);
		if(response.equals("User not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	
	@PutMapping(value = "/users/update")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> updateUser(@RequestBody User user) throws InterruptedException, ExecutionException{
		String response = userServ.updateUser(user);
		if(response.equals("User not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PutMapping(value = "/users/update",params="userId")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> updateUserField(@RequestParam(required = true, value="userId")String userId, 
												  @RequestParam(required = true)String userField,
												  @RequestParam(required = true)String replaceValue) throws InterruptedException, ExecutionException{
		String response = userServ.updateUserField(userId, userField, replaceValue);
		if(response.equals("User not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("This field cannot be updated.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}	
	
	@PostMapping(value="/users/tokens/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> updateUserTokens(@RequestBody(required = true)UserTokenRequest userTokenRequest) throws InterruptedException, ExecutionException{
		String response = userServ.updateUserTokens(userTokenRequest.getUser(), userTokenRequest.getService());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
}
