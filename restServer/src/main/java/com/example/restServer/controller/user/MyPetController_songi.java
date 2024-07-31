package com.example.restServer.controller.user;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.restServer.dto.PetDto;
import com.example.restServer.entity.Member;
import com.example.restServer.entity.Pet;
import com.example.restServer.repository.MemberRepository;
import com.example.restServer.repository.PetRepository;
import com.example.restServer.repository.ReservationRepository;
import com.example.restServer.service.user.PetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;


@RestController
@RequestMapping("/user/mypage")
public class MyPetController_songi {
	
	@Autowired
    private PetService petService;
	
	
	@Autowired
	private PetRepository petRepository;
	
	@Autowired
	private MemberRepository memberRepo;
	
	@Autowired
	private ReservationRepository reserveRepo;
	
	
	@Value("${spring.servlet.multipart.location}")
    private String uploadPath;
	
	
	@PostMapping("/myPet")
	public ResponseEntity<?> myPetRegist(PetDto petDto , @RequestParam("photo") MultipartFile file, HttpServletRequest request ) {
		   String memberIdHeader = request.getHeader("MemberId");
		   String authHeader = request.getHeader("Authorization");

		   if (memberIdHeader == null || authHeader == null) {
	    	   String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
	    	    return ResponseEntity.badRequest().body(errorMessage);
	       }

		   Long memberId = Long.parseLong(memberIdHeader);
		   Member member = memberRepo.findById(memberId).get();
		
	    try {
	    
	        Pet pet = new Pet();
	        pet.setBirthdate(petDto.getBirthdate());
	        pet.setGender(petDto.getGender());
	        pet.setHealthIssues(petDto.getHealthIssues());
	        pet.setIsNeutered(petDto.getIsNeutered());
	        pet.setName(petDto.getName());
	        pet.setType(petDto.getType());
	        pet.setBigtype(petDto.getBigtype());
	        pet.setWeight(petDto.getWeight());
	        pet.setMember(member);
	        
	        String imgOriginName = petDto.getFileName();
	        String imgNewName = UUID.randomUUID().toString() + "_" + imgOriginName;
	        System.out.println(imgOriginName + imgNewName);
	        
	        
	        pet.setPhoto(imgNewName);
	        
	        File imgFile = new File(pet.getPhoto());
	        petDto.getPhoto().transferTo(imgFile);
	        
	        
	        
	        
	        System.out.println(pet);
	        // Pet 객체를 데이터베이스에 저장
	        petRepository.save(pet);

	        return ResponseEntity.ok("success");

	   
	    } catch (Exception e) {
	        // 기타 예외 처리
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to register pet: " + e.getMessage());
	    }
	}
	
	

	
	@GetMapping("/myPet")
	public ResponseEntity<?> myPetList(HttpServletRequest request) {
		
	   String memberIdHeader = request.getHeader("MemberId");
	   String authHeader = request.getHeader("Authorization");

	   if (memberIdHeader == null || authHeader == null) {
    	   String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
    	    return ResponseEntity.badRequest().body(errorMessage);
       }

	   Long memberId = Long.parseLong(memberIdHeader);
	   Member member = memberRepo.findById(memberId).get();


		List<Pet> petList = petRepository.findByMember(member);
		System.out.println("펫 목록 출력 : " + petList );
		
		return ResponseEntity.ok(petList);
	}
	
	
	
	@GetMapping("/myPet/{id}")
	public ResponseEntity<?> myPetDetail(@PathVariable("id") Long id, HttpServletRequest request) {
	   System.out.println("펫정보 불러오기 id값 출력: " + id);
	    
	   String memberIdHeader = request.getHeader("MemberId");
	   String authHeader = request.getHeader("Authorization");

	   if (memberIdHeader == null || authHeader == null) {
    	   String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
    	    return ResponseEntity.badRequest().body(errorMessage);
       }

	    
	    Optional<Pet> optionalPet = petRepository.findById(id);
	    if (optionalPet.isPresent()) {
	        Pet pet = optionalPet.get();
	        System.out.println("펫 상세정보 출력: " + pet);
	        return ResponseEntity.ok(pet);
	    } else {
	        System.out.println("펫을 찾을 수 없습니다: " + id);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	    }
	}
	
	
	@PutMapping("/myPet/{id}")
	public ResponseEntity<?> myPetEdit(@PathVariable("id")Long id,PetDto petDto, HttpServletRequest request) {
		 String memberIdHeader = request.getHeader("MemberId");
		   String authHeader = request.getHeader("Authorization");

		   if (memberIdHeader == null || authHeader == null) {
	    	   String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
	    	    return ResponseEntity.badRequest().body(errorMessage);
	       }
	       
	       
		try {
			Pet pet = petRepository.findById(id).get();
			
			pet.setBirthdate(petDto.getBirthdate());
	        pet.setGender(petDto.getGender());
	        pet.setHealthIssues(petDto.getHealthIssues());
	        pet.setIsNeutered(petDto.getIsNeutered());
	        pet.setName(petDto.getName());
	        pet.setType(petDto.getType());
	        pet.setBigtype(petDto.getBigtype());
	        pet.setWeight(petDto.getWeight());
			
			
	        petRepository.save(pet);
	        
	        return ResponseEntity.ok("success");
		}catch(Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed");
		}
		
	}
	

	@PutMapping("/myPetDelete/{id}")
	public ResponseEntity<String> myPetDelte(@PathVariable("id")Long id, HttpServletRequest request) {
	   String memberIdHeader = request.getHeader("MemberId");
	   String authHeader = request.getHeader("Authorization");

	   if (memberIdHeader == null || authHeader == null) {
    	   String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
    	    return ResponseEntity.badRequest().body(errorMessage);
       }

	   //Long memberId = Long.parseLong(memberIdHeader);
	   //Member member = memberRepo.findById(memberId).get();
		
		
//	    reserveRepo.deleteByPetId(id);
//		petRepository.deleteById(id);
	   
	   Pet pet = petRepository.findById(id).get();
	   pet.setStatus("삭제");
	   petRepository.save(pet);
	   
		
		return ResponseEntity.ok("삭제가 완료되었습니다."); 
		
	}
	
	 @PutMapping("/petPhoto")
	    public ResponseEntity<String> photoEdit(@RequestParam("petId") Long petId, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
	        String memberIdHeader = request.getHeader("MemberId");
	        String authHeader = request.getHeader("Authorization");
	        System.out.println("MemberId Header: " + memberIdHeader);
	        System.out.println("Authorization Header: " + authHeader);
	        
	        if (memberIdHeader == null || authHeader == null) {
	            String errorMessage = "MemberId 또는 Authorization 헤더가 없습니다.";
	            return ResponseEntity.badRequest().body(errorMessage);
	        }

	        try {
	            Optional<Pet> optionalPet = petRepository.findById(petId);
	            if (optionalPet.isEmpty()) {
	                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pet not found");
	            }

	            Pet pet = optionalPet.get();
	            
	            
	            String imgOriginName = file.getOriginalFilename();
	            String imgNewName = UUID.randomUUID().toString() + "_" + imgOriginName;
	            System.out.println(imgOriginName + imgNewName);

	            pet.setPhoto(imgNewName);

	            File imgFile = new File(uploadPath + imgNewName);
	            file.transferTo(imgFile);

	            petRepository.save(pet);

	            return ResponseEntity.ok("Photo updated successfully");

	        } catch (IOException e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update photo: " + e.getMessage());
	        }
	    }
	
}
