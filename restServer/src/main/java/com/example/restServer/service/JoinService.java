package com.example.restServer.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.restServer.dto.IMemberEditDto;
import com.example.restServer.dto.JoinHospitalDto;
import com.example.restServer.dto.MemberDto;
import com.example.restServer.dto.MemberEditDto;
import com.example.restServer.entity.Doctor;
import com.example.restServer.entity.Login;
import com.example.restServer.entity.Member;
import com.example.restServer.repository.DoctorRepository;
import com.example.restServer.repository.LoginRepository;
import com.example.restServer.repository.MemberRepository;

@Service
public class JoinService {
	
	@Value("${spring.servlet.multipart.location}")
	private String uploadPath;
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	LoginRepository loginRepository;
	
	@Autowired
	DoctorRepository doctorRepository;
	
	
	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	
	public void joinUser(MemberDto memberDto) {
		Member member = new Member();
		
		member.setName(memberDto.getName());
		member.setAddress(memberDto.getAddress());
		String pNum = memberDto.getPhone();
		String pNumNew= phoneCheck(pNum);
		member.setPhone(pNumNew);
	
		member.setNickname(memberDto.getNickname());
		member.setRole(memberDto.getRole());
		member.setEmail(memberDto.getEmail());
		member.setStatus("승인");
		if(loginRepository.existsByUsername(memberDto.getUsername())) {
			return;
		}
		memberRepository.save(member);
		
		Member result = memberRepository.findByNickname(memberDto.getNickname());
		System.out.println(result);
		Login login = new Login();
		
		login.setMember(result);
		login.setUsername(memberDto.getUsername());
		String password = bCryptPasswordEncoder.encode(memberDto.getPassword());
		login.setPassword(password);
		login.setRole(memberDto.getRole());
		//login.setIsApproved(true);
		
		loginRepository.save(login); 
	
		
		
	}
	
	
	public void joinHospital(JoinHospitalDto joinHospitalDto) {
		Member member = new Member();
		member.setAddress(joinHospitalDto.getAddress());
		String pNum = joinHospitalDto.getPhone();
		String pNumNew= phoneCheck(pNum);
		member.setPhone(pNumNew);
		
		member.setBusinessNumber(joinHospitalDto.getBusinessNumber());
		member.setHospitalName(joinHospitalDto.getHospitalName());
		member.setRepresentative(joinHospitalDto.getRepresentative());
		member.setBusinessHours(joinHospitalDto.getBusinessHours());
		member.setPartnership(joinHospitalDto.getPartnership());
		member.setIntroduction(joinHospitalDto.getIntroduction());
		member.setRole(joinHospitalDto.getRole());
		member.setEmail(joinHospitalDto.getEmail());
		member.setStatus("대기");
		
		if(joinHospitalDto.getFile() != null && !joinHospitalDto.getFile().isEmpty() ) {
			String originName = joinHospitalDto.getFileName();
			String newName = UUID.randomUUID().toString() + originName;
			member.setLogo(newName);
			File file = new File(newName);
			
			try {
				joinHospitalDto.getFile().transferTo(file);
				System.out.println("파일 업로드 성공....");
				
				//썸네일 생성
				//String thumbnailSaveName = "s_" + newName;
				//board.setThumbnailName(thumbnailSaveName);
				
				//File thumbfile = new File(uploadPath + thumbnailSaveName);
				//File ufile = new File(uploadPath + newName);
				
				//Thumbnails.of(ufile).size(100,100).toFile(thumbfile);
				System.out.println(joinHospitalDto);
				
				
			}catch(Exception e){
				
			}
		}
		
		member = memberRepository.save(member);
		
		//System.out.println(member);
		Login login = new Login();
		login.setMember(member);
		login.setUsername(joinHospitalDto.getUsername());
		String password = bCryptPasswordEncoder.encode(joinHospitalDto.getPassword());
		login.setPassword(password);
		login.setRole(joinHospitalDto.getRole());
		loginRepository.save(login); 
		
		String doctor_ =joinHospitalDto.getDoctorNamesField();
		System.out.println("doctor_:"+doctor_);
		String[] doctors = doctor_.split("//");
		if(!(doctors[0].equals(""))){
			for(String d : doctors) {
				System.out.println("doctor:"+d);
				Doctor doctor = new Doctor();
				doctor.setHospital(member);
				doctor.setName(d);
				doctorRepository.save(doctor);
			}
		}
	}
	
	//유저정보 가져오기
	public List<IMemberEditDto> getEditUserInfo(String memberId) {
		List<IMemberEditDto> data = new ArrayList<>();
		data =loginRepository.findByMemberIdEditUser(memberId);
		
		return data;
	}
	
	//유저정보수정
	public void updateEditUserInfo(MemberEditDto memberEditDto) {
		System.out.println("유저정보수정서비스 들어옴");
		if(memberEditDto.getPasswordCheckBox().equals("true")) {
			System.out.println("유저정보수정서비스 새 비밀번호 if문 들어옴");
			Login login = loginRepository.findByUsername(memberEditDto.getUsername());
			String password = bCryptPasswordEncoder.encode(memberEditDto.getPasswordNew());
			login.setPassword(password);
			loginRepository.save(login);
			//비밀번호 잘 바뀌는지 확인 해야함!!!!!!!!
		}
		Member member = memberRepository.findById(memberEditDto.getMemberId()).get();
		member.setAddress(memberEditDto.getAddress());
		String pNum = memberEditDto.getPhone();
		String pNumNew= phoneCheck(pNum);
		member.setPhone(pNumNew);
		
		member.setEmail(memberEditDto.getEmail());
		member.setName(memberEditDto.getName());
		member.setNickname(memberEditDto.getNickname());
		memberRepository.save(member);
	}
	
	
	//병원정보 가져오기
	public List<IMemberEditDto> getEditHospitalInfo(String memberId) {
		List<IMemberEditDto> data = new ArrayList<>();
		data = loginRepository.findByMemberIdEditHospital(memberId);
		
		return data;
	}
	
	//병원정보수정
		public void updateEditHospitalInfo(MemberEditDto memberEditDto) {
			System.out.println("병원정보수정서비스 들어옴");
			if(memberEditDto.getPasswordCheckBox().equals("true")) {
				System.out.println("병원정보수정서비스 새 비밀번호 if문 들어옴");
				Login login = loginRepository.findByUsername(memberEditDto.getUsername());
				String password = bCryptPasswordEncoder.encode(memberEditDto.getPasswordNew());
				login.setPassword(password);
				loginRepository.save(login);
				//비밀번호 잘 바뀌는지 확인 해야함!!!!!!!!
			}
			Member member = memberRepository.findById(memberEditDto.getMemberId()).get();
			String pNum = memberEditDto.getPhone();
			String pNumNew= phoneCheck(pNum);
			member.setPhone(pNumNew);
			
			member.setEmail(memberEditDto.getEmail());
			member.setRepresentative(memberEditDto.getRepresentative());
			member.setBusinessHours(memberEditDto.getBusinessHours());
			member.setPartnership(memberEditDto.getPartnership());
			member.setIntroduction(memberEditDto.getIntroduction());
			//이미지 파일 처리
			if(memberEditDto.getFile() != null && !memberEditDto.getFile().isEmpty()) {
				//기존 사진 삭제
				String existingLogo = member.getLogo();
				if(existingLogo !=null && !(existingLogo.equals(""))) {
					File existingFile = new File(uploadPath + existingLogo);
					if(existingFile.exists()) {
						existingFile.delete();
						System.out.println("기존 파일 삭제 성공: "+ existingLogo);
					}
				}
				
				//새로운 사진 저장
				String originName = memberEditDto.getFileName();
				String newName = UUID.randomUUID().toString() + originName;
				member.setLogo(newName);
				File file = new File(newName);
				
				try {
					memberEditDto.getFile().transferTo(file);
					System.out.println("파일 업로드 성공....");
					
					//썸네일 생성
					//String thumbnailSaveName = "s_" + newName;
					//board.setThumbnailName(thumbnailSaveName);
					
					//File thumbfile = new File(uploadPath + thumbnailSaveName);
					File ufile = new File(uploadPath + newName);
					
					//Thumbnails.of(ufile).size(100,100).toFile(thumbfile);
					System.out.println(memberEditDto);
					
					
				}catch(Exception e){
					
				}
				
			}
			memberRepository.save(member);
			
			//동물병원의사 처리
			List<Doctor> doctors = doctorRepository.findAllByHospitalId(memberEditDto.getMemberId());
			String[] newDoctors =memberEditDto.getDoctorNamesField().split("//");
			int newDoctorsCnt =newDoctors.length;
			 int doctorCnt = doctors.size();
			 System.out.println("기존 의사수 :"+ doctorCnt);
			 System.out.println("수정 의사수 :"+ newDoctorsCnt);
			boolean[] working = new boolean[doctorCnt];
			boolean[] newD = new boolean[newDoctorsCnt];
			for(int i= 0;i<doctorCnt;i++) {
				for(int j =0 ; j<newDoctorsCnt ;j++) {
					System.out.println(doctors.get(i).getName());
					System.out.println(newDoctors[j]);
					if(doctors.get(i).getName().equals(newDoctors[j])) {
						System.out.println("같은 이름 :"+newDoctors[j]);
						System.out.println(doctors.get(i));
						if(doctors.get(i).getStatus()==null || !doctors.get(i).getStatus().equals("퇴사")) {
							
							working[i]=true;
							newD[j]=true;
						}
						
					
					}
				}
			}
			for(int i= 0;i<working.length;i++) {
				System.out.println(working[i]);
				if(!working[i]) {
					doctors.get(i).setStatus("퇴사");
					doctorRepository.save(doctors.get(i));
				}
			}
			for(int i= 0;i<newD.length;i++) {
				if(!newD[i]) {
					Doctor doc = new Doctor();
					doc.setHospital(member);
					doc.setName(newDoctors[i]);
					doctorRepository.save(doc);
				}
			}
			
			//List<String> newDoctors = memberEditDto.getDoctorNamesField().
			
			
		}
		
		//전화번호 변환 함수
		public String phoneCheck(String pNum) {
			String pNumNew=pNum; 
			if(!pNum.contains("-")) {
				System.out.println("전화번호 이프 들어옴");
				if(pNum.length()==11) {
					String part1 = pNum.substring(0, 3);  // "010"
			        String part2 = pNum.substring(3, 7);  // "1111"
			        String part3 = pNum.substring(7);     // "2222"
			        pNumNew=part1 + "-" + part2 + "-" +part3;
				}else if(pNum.length() <11) {
					if(pNum.startsWith("02")) {
						String part1 = pNum.substring(0, 2);  // "02"
				        String part2 = pNum.substring(2, 5);  // "111"
				        String part3 = pNum.substring(5);     // "2222"
				        pNumNew=part1 + "-" + part2 + "-" +part3;
					}else {
						String part1 = pNum.substring(0, 3);  // "051"
				        String part2 = pNum.substring(3, 6);  // "111"
				        String part3 = pNum.substring(6);     // "2222"
				        pNumNew=part1 + "-" + part2 + "-" +part3;
					}
				}
			}
			return pNumNew;
		}
	

}
