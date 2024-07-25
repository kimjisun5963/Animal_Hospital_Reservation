package com.example.restServer.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.restServer.entity.Support;

public interface SupportRepository extends JpaRepository<Support, Long> {
	
	public List<Support> findAllByCategory(String category);
	
	@Query(value = "SELECT * FROM support WHERE category = '불편신고' OR category = '기타문의' order by created_at desc", nativeQuery = true)
	public Page<Support> findAllByCategoryQna(Pageable pageable);

	@Query(value = "SELECT * FROM support WHERE (category = '불편신고' OR category = '기타문의') AND title LIKE %:keyword% order by created_at desc", nativeQuery = true)
	public Page<Support> findAllByCategoryQnaByAllCategoryKeyword(Pageable pageable,@Param("keyword")String keyword);
	
	@Query(value = "SELECT * FROM support WHERE category =:category AND title LIKE %:keyword%  order by created_at desc", nativeQuery = true)
	public Page<Support> findAllByCategoryQnaByCategoryKeyword(Pageable pageable,@Param("category")String category, @Param("keyword")String keyword);
	
	@Query(value = "SELECT * FROM support WHERE category =:category  order by created_at desc", nativeQuery = true)
	public Page<Support> findAllByCategoryQnaByCategory(Pageable pageable, @Param("category")String category);
	
	@Query(value="Select * from support where member_id=:userId;", nativeQuery = true)
	public List<Support> findAllByMemberId(@Param("userId") Long userId);
	
	@Query(value="Select * from support where member_id=:userId ORDER BY created_at DESC;", nativeQuery = true)
	public List<Support> findAllByMemberIdOrderByCreatedAt(@Param("userId") Long userId);
	
	@Query(value="Select * from support where (category = '불편신고' OR category = '기타문의') AND reply is null limit 5", nativeQuery = true)
	public List<Support> findAllByCategoryQnaNoAnswer();
	
}
