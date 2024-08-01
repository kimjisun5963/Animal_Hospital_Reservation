package com.example.restServer.service.user;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.restServer.entity.Coupon;
import com.example.restServer.entity.Doctor;
import com.example.restServer.entity.Member;
import com.example.restServer.entity.Pet;
import com.example.restServer.entity.Point;
import com.example.restServer.entity.Reservation;
import com.example.restServer.entity.UnavailableTime;
import com.example.restServer.repository.CouponRepository;
import com.example.restServer.repository.DoctorRepository;
import com.example.restServer.repository.MemberRepository;
import com.example.restServer.repository.PetRepository;
import com.example.restServer.repository.PointRepository;
import com.example.restServer.repository.ReservationRepository;
import com.example.restServer.repository.UnavailableTimeRepository;
import com.example.restServer.util.DateTimeUtil;

@Service
public class ReservationService {

    @Autowired 
    private MemberRepository memRepo;
    @Autowired 
    private ReservationRepository reservRepo;
    @Autowired 
    private PetRepository petRepo;
    @Autowired 
    private CouponRepository couponRepo;
    @Autowired 
    private DoctorRepository doctorRepo;
    @Autowired 
    private UnavailableTimeRepository unavailableTimeRepo;
    @Autowired 
    private PointRepository pointRepo;
    private final Map<String, LockInfo> slotLocks = new ConcurrentHashMap<>();
    private final long LOCK_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(30);

    
    //오래된 슬롯 삭제
	@Scheduled(fixedRate = 300000)
    private void clearOldSlot() {
        slotLocks.forEach((key, lockInfo) -> {
            if (System.currentTimeMillis() - lockInfo.timestamp > LOCK_EXPIRATION_TIME) {
                System.out.println(slotLocks);
                slotLocks.computeIfPresent(key, (k, v) -> null);
                System.out.println(slotLocks);
            }
        });
	}
	
	
    private static class LockInfo {
        ReentrantLock lock = new ReentrantLock();
        long timestamp = System.currentTimeMillis();
    }

    private ReentrantLock getLockForSlot(String doctorId, String date, String timeSlot) {
        String key = getSlotKey(doctorId, date, timeSlot);
        LockInfo lockInfo = slotLocks.computeIfAbsent(key, k -> new LockInfo());
        lockInfo.timestamp = System.currentTimeMillis();
        return lockInfo.lock;
    }

    // 메서드 내에서 호출
    private String getSlotKey(String doctorId, String date, String timeSlot) {
        return doctorId + "-" + date + "-" + timeSlot;
    }
    

    public Map<String, Object> getPetInfo(Long userId, Long hospitalId) {
        Member user = memRepo.findById(userId).get();
        List<Pet> petList = petRepo.findAllByMemberId(userId);
        List<Coupon> couponList = couponRepo.findCouponByUserAndHospital(userId, hospitalId);
        Integer point = pointRepo.findByUserIdRemainingPoints(userId);
        List<Integer> pointList = new ArrayList<>();
        pointList.add(point);
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("petList", petList);
        map.put("couponList", couponList);
        map.put("pointList", pointList);
        return map;
    }

    public Map<String, Object> getVetAvailInfo(Long hospitalId) {
        Map<String, Object> map = new HashMap<>();
        List<Doctor> docList = doctorRepo.findAllByHospitalId(hospitalId);
        for (Doctor doctor : docList) {
            Long docId = doctor.getId();
            String docName = doctor.getName();
            List<UnavailableTime> unavailList = unavailableTimeRepo.findAllByDoctorId(docId);
            System.out.println(unavailList);
            map.put(docId + "//" + docName, unavailList);
        }
        return map;
    }

    public Map<String, Object> getVetInfo(Long hospitalId) {
        Map<String, Object> map = new HashMap<>();
        Member vet = memRepo.findById(hospitalId).get();
        vet.setBusinessHours(DateTimeUtil.getBusinessHours(vet.getBusinessHours()));
        map.put(vet.getHospitalName() + "", vet);
        return map;
    }

    public String makeReservation(Map<String, String> formData, Long userId) throws ParseException, ResponseStatusException {
    	ReentrantLock slotLock = getLockForSlot(formData.get("doctorId"), formData.get("date"), formData.get("time"));
        slotLock.lock();
        try { 
            Date now = new Date();
            LocalDateTime dateTime = DateTimeUtil.parseDateTime(formData);

            Reservation reservation = createReservation(formData, userId, dateTime);
            
             // 예약 중복 확인
            if (isDuplicateReservation(reservation)) {
            	 throw new ResponseStatusException(HttpStatus.CONFLICT, "중복예약이 발생했습니다.");
            }
            
            reservRepo.save(reservation);

            UnavailableTime unavailTime = createUnavailableTime(formData, reservation, dateTime);
            unavailableTimeRepo.save(unavailTime);
            return "success";
        } finally {
        	slotLock.unlock();
        	System.out.println(slotLocks);
        }
    }

    public String editReservation(Map<String, String> formData, Long userId) throws ParseException,ResponseStatusException {
    	ReentrantLock slotLock = getLockForSlot(formData.get("doctorId"), formData.get("date"), formData.get("time"));
        slotLock.lock();
    	try {
            Date now = new Date();
           
            Long reservId = Long.parseLong(formData.get("reservId"));
            LocalDateTime dateTime = DateTimeUtil.parseDateTime(formData);

            Reservation reservation = reservRepo.findById(reservId).get();
            
            deleteOriginalUnavailableTime(reservation);
            
            updateReservation(formData, reservation, dateTime, now);
            
            // 예약 중복 확인
            if (isDuplicateReservation(reservation)) {
            	 throw new ResponseStatusException(HttpStatus.CONFLICT, "중복예약이 발생했습니다.");
            }
            reservRepo.save(reservation);

            UnavailableTime unavailTime = createUnavailableTime(formData, reservation, dateTime);
            unavailableTimeRepo.save(unavailTime);
            return "success";
        } finally {
        	slotLock.unlock();
        }
    }
    
    private boolean isDuplicateReservation(Reservation reservation) {
        Long doctorId = reservation.getDoctor().getId();
        LocalDateTime dateTime = reservation.getReservationDatetime();
        UnavailableTime conflictingReservation = unavailableTimeRepo.findTimeByDoctorIdNDatetime(doctorId, DateTimeUtil.formatDate(dateTime), DateTimeUtil.formatTime1(dateTime));
        return conflictingReservation != null;
        		
    }

    public Reservation findReservInfo(Long reservId) {
        return reservRepo.findById(reservId).get();
    }

    private void deleteOriginalUnavailableTime(Reservation reservation) {
        LocalDateTime oriDateTime = reservation.getReservationDatetime();
        UnavailableTime oriUnavailTime = unavailableTimeRepo.findTimeByDoctorIdNDatetime(
            reservation.getDoctor().getId(),
            DateTimeUtil.formatDate(oriDateTime),
            DateTimeUtil.formatTime(oriDateTime.toLocalTime())
        );
        unavailableTimeRepo.delete(oriUnavailTime);
    }

    private Reservation createReservation(Map<String, String> formData, Long userId, LocalDateTime dateTime) {
        Date now = new Date();
        Reservation reservation = new Reservation();
        reservation.setUser(memRepo.findById(userId).get());
        reservation.setHospital(memRepo.findById(Long.parseLong(formData.get("hospitalId"))).get());
        reservation.setMemo(formData.get("memo"));
        reservation.setDoctor(doctorRepo.findById(Long.parseLong(formData.get("doctorId"))).get());
        reservation.setStatus("대기");
        reservation.setType(formData.get("type"));
        reservation.setPet(petRepo.findById(Long.parseLong(formData.get("pet"))).get());
        reservation.setReservationDatetime(dateTime);

        setReservationPoints(formData, reservation, userId, now);
        setReservationCoupon(formData, reservation, now);

        return reservation;
    }

    private void setReservationPoints(Map<String, String> formData, Reservation reservation, Long userId, Date now) {
        String point = formData.get("point");
        if (point != null && !point.equals("") && !point.equals("0")) {
            reservation.setPointsUsed(Integer.parseInt(point));
            Point usedPoint = new Point();
            usedPoint.setUser(memRepo.findById(userId).get());
            usedPoint.setUseDate(now);
            usedPoint.setPointsUsed(Integer.parseInt(point));
            usedPoint.setComment("예약포인트사용");
            pointRepo.save(usedPoint);
        }
    }

    private void setReservationCoupon(Map<String, String> formData, Reservation reservation, Date now) {
        if (!formData.get("coupon").equals("쿠폰사용 안함")) {
            Coupon coupon = couponRepo.findById(Long.parseLong(formData.get("coupon"))).get();
            coupon.setIsUsed(true);
            coupon.setUseDate(now);
            reservation.setCoupon(coupon);
            couponRepo.save(coupon);
        }
    }

    private UnavailableTime createUnavailableTime(Map<String, String> formData, Reservation reservation, LocalDateTime dateTime) throws ParseException {
        UnavailableTime unavailTime = new UnavailableTime();
        unavailTime.setDoctor(doctorRepo.findById(Long.parseLong(formData.get("doctorId"))).get());
        unavailTime.setHospital(doctorRepo.findById(Long.parseLong(formData.get("doctorId"))).get().getHospital());
        unavailTime.setComment("진료예약");
        unavailTime.setDate(DateTimeUtil.parseDate(formData.get("date")));
        unavailTime.setTime(DateTimeUtil.parseTimeHourMins(formData.get("time")));
        return unavailTime;
    }

    private void updateReservation(Map<String, String> formData, Reservation reservation, LocalDateTime dateTime, Date now) {
        reservation.setUser(reservation.getUser());
        reservation.setHospital(reservation.getHospital());
        reservation.setDoctor(doctorRepo.findById(Long.parseLong(formData.get("doctorId"))).get());
        reservation.setStatus("대기");
        reservation.setType(formData.get("type"));
        reservation.setPet(petRepo.findById(Long.parseLong(formData.get("pet"))).get());
        reservation.setReservationDatetime(dateTime);

        if (!formData.get("memo").equals("")) {
            reservation.setMemo(formData.get("memo"));
        }

        updateReservationPoints(formData, reservation, now);
        updateReservationCoupon(formData, reservation, now);
    }

    private void updateReservationPoints(Map<String, String> formData, Reservation reservation, Date now) {
        if (formData.get("point").equals("") || formData.get("point").equals("0")) {
            if (reservation.getPointsUsed() != null) {
                Point newPoint = new Point();
                newPoint.setUser(reservation.getUser());
                newPoint.setPointsAccumulated(reservation.getPointsUsed());
                newPoint.setAccumulationDate(now);
                newPoint.setComment("예약취소포인트반환");
                pointRepo.save(newPoint);
            }
            reservation.setPointsUsed(null);
        } else {
            if (reservation.getPointsUsed() != null) {
                Point newPoint = new Point();
                newPoint.setUser(reservation.getUser());
                newPoint.setPointsAccumulated(reservation.getPointsUsed());
                newPoint.setAccumulationDate(now);
                newPoint.setComment("예약취소포인트반환");
                pointRepo.save(newPoint);
            }

            reservation.setPointsUsed(Integer.parseInt(formData.get("point")));
            Point usedPoint = new Point();
            usedPoint.setUser(reservation.getUser());
            usedPoint.setUseDate(now);
            usedPoint.setPointsUsed(Integer.parseInt(formData.get("point")));
            usedPoint.setComment("예약포인트사용");
            pointRepo.save(usedPoint);
        }
    }

    private void updateReservationCoupon(Map<String, String> formData, Reservation reservation, Date now) {
        if (formData.get("coupon").equals("쿠폰사용 안함")) {
            if (reservation.getCoupon() != null) {
                Coupon cp = reservation.getCoupon();
                cp.setIsUsed(false);
                cp.setUseDate(null);
                couponRepo.save(cp);
            }
            reservation.setCoupon(null);
        } else {
            if (reservation.getCoupon() != null) {
                Coupon cp = reservation.getCoupon();
                cp.setIsUsed(false);
                cp.setUseDate(null);
                couponRepo.save(cp);
            }
            Coupon coupon = couponRepo.findById(Long.parseLong(formData.get("coupon"))).get();
            coupon.setIsUsed(true);
            coupon.setUseDate(now);
            reservation.setCoupon(coupon);
            couponRepo.save(coupon);
        }
    }
}
