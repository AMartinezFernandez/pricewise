package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.company.id = :companyId")
    java.util.List<User> findByCompanyId(@Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);

    long countByActiveTrue();

    long countByRole(com.alvaro.pricewise.entity.User.Role role);

    @Query("SELECT u.company.name AS companyName, COUNT(u) AS userCount " +
           "FROM User u WHERE u.company IS NOT NULL GROUP BY u.company.name")
    java.util.List<Object[]> countUsersGroupedByCompany();
}

