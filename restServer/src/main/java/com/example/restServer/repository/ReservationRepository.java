package com.example.restServer.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.restServer.dto.UserReservationDto;
import com.example.restServer.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	@Query(value = "SELECT * FROM reservation WHERE hospital_id = :memberId AND status = :status ORDER BY reservation_datetime asc", nativeQuery = true)
	Page<Reservation> findAllByHospitalIdAndStatus(Pageable pageable, @Param("memberId")Long memberId, @Param("status")String status);
	
	@Query(value = "SELECT AVG(RATING) FROM reservation WHERE hospital_id = :hospitalId AND !ISNULL(RATING);", nativeQuery = true)
	Long findAvgReview(@Param("hospitalId") Long hospitalId);

	@Query(value = "SELECT * FROM reservation WHERE doctor_id = :doctorId AND STATUS != '취소'", nativeQuery = true)
	List<Reservation> findAllByDoctorId(@Param("doctorId")Long doctorId);
	
	@Query(value = "SELECT * FROM reservation WHERE hospital_id= :hospitalId AND !isnull(review) ORDER BY updated_at DESC", nativeQuery = true)
	List<Reservation> findReservWithReview (@Param("hospitalId")Long hospitalId);
	
	@Query(value = "SELECT re.*,h.hospital_name,\r\n"
			+ "p.name AS pet_name,p.type AS pet_type,p.photo,p.gender,p.birthdate,\r\n"
			+ "u.nickname,u.phone,\r\n"
			+ "c.name AS coupon_name,\r\n"
			+ "d.name AS doctor_name\r\n"
			+ "from reservation re\r\n"
			+ "LEFT JOIN member h\r\n"
			+ "ON re.hospital_id = h.id\r\n"
			+ "LEFT JOIN pet p\r\n"
			+ "ON re.pet_id = p.id\r\n"
			+ "LEFT JOIN member u\r\n"
			+ "ON re.user_id = u.id\r\n"
			+ "LEFT JOIN coupon c\r\n"
			+ "ON re.coupon_id = c.id\r\n"
			+ "LEFT JOIN doctor d\r\n"
			+ "ON re.doctor_id = d.id\r\n"
			+ "WHERE re.user_id = :userId order by re.reservation_datetime desc;", nativeQuery = true )
	List<UserReservationDto> findReserListByUserId(@Param("userId")Long userId);

	List<Reservation> findByHospitalId(Long memberId);

	List<Reservation> findByDoctorIdAndReservationDatetime(Long doctorId, LocalDateTime reservationDatetime);
	
	@Query(value = "SELECT r.*\r\n"
			+ "FROM reservation r\r\n"
			+ "JOIN (\r\n"
			+ "    SELECT pet_id, MAX(reservation_datetime) AS max_reservation_datetime\r\n"
			+ "    FROM reservation\r\n"
			+ "    WHERE hospital_id =:hospitalId\r\n"
			+ "    GROUP BY pet_id\r\n"
			+ ") latest_reservations\r\n"
			+ "ON r.pet_id = latest_reservations.pet_id\r\n"
			+ "AND r.reservation_datetime = latest_reservations.max_reservation_datetime\r\n"
			+ "WHERE r.hospital_id =:hospitalId ORDER BY r.reservation_datetime desc", nativeQuery = true)
	Page<Reservation> findByCustomerList(Pageable pageable, @Param("hospitalId")Long hospitalId);
	
}