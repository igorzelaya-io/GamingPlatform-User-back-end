package com.d1gaming.user.user;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.mapstruct.Mapper;

import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserView;

@Mapper(componentModel = "spring")
public abstract class UserViewMapper{
	
	private UserService userServ;
	
	public UserViewMapper() {
	
	}
	
	public abstract UserView toUserView(User user);
	
	public abstract List<UserView> toUserView(List<User> users);
	
	public UserView toUserViewById(String userId) throws InterruptedException, ExecutionException {
		if(userId == null) {
			return null;
		}
		return toUserView(userServ.getUserById(userId));
	}

}
