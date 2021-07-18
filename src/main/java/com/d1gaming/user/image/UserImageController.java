package com.d1gaming.user.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin( origins = "localhost:4200")
@RequestMapping(value = "/userimagesapi")
@PreAuthorize("permitAll()")
public class UserImageController {

	@Autowired
	private UserImageService userImageService;
//
//	@PostMapping(value = "/images")
//	public ResponseEntity<?> saveImage(@RequestBody(required=true)ImageModel userImage) throws IOException, InterruptedException, ExecutionException{
//		String response = userImageService.saveUserImage(userImage.getImageModelId(), userImage.getImageBytes());
//		if(response.equals("User not found.")) {
//			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
//		}
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}
//
//	@GetMapping(value = "/images")
//	public ResponseEntity<?> getUserImage(@RequestParam(required = true) String userId) throws InterruptedException, ExecutionException{
//		Optional<ImageModel> userImage = userImageService.getUserImage(userId);
//		if(userImage == null ) {
//			return new ResponseEntity<>(userImage, HttpStatus.NO_CONTENT);
//		}
//		return new ResponseEntity<>(userImage, HttpStatus.OK);
//	}
}
