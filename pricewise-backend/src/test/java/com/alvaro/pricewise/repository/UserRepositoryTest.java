package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        company = entityManager.persistAndFlush(Company.builder()
                .name("Test Company")
                .businessType("ecommerce")
                .build());
    }

    private User createUser(String username, String email) {
        return entityManager.persistAndFlush(User.builder()
                .username(username)
                .email(email)
                .password("encodedPassword")
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .active(true)
                .build());
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("Encuentra usuario por email con empresa cargada (FETCH JOIN)")
        void findByEmail_existing_returnsUserWithCompany() {
            createUser("alvaro", "alvaro@email.com");

            Optional<User> found = userRepository.findByEmail("alvaro@email.com");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("alvaro");
            assertThat(found.get().getCompany()).isNotNull();
            assertThat(found.get().getCompany().getName()).isEqualTo("Test Company");
        }

        @Test
        @DisplayName("No encuentra usuario con email inexistente")
        void findByEmail_nonExisting_returnsEmpty() {
            Optional<User> found = userRepository.findByEmail("no-existe@email.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsernameTests {

        @Test
        @DisplayName("Encuentra usuario por username")
        void findByUsername_existing_returnsUser() {
            createUser("maria", "maria@email.com");

            Optional<User> found = userRepository.findByUsername("maria");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("maria@email.com");
        }

        @Test
        @DisplayName("No encuentra username inexistente")
        void findByUsername_nonExisting_returnsEmpty() {
            Optional<User> found = userRepository.findByUsername("fantasma");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail / existsByUsername")
    class ExistsTests {

        @Test
        @DisplayName("existsByEmail devuelve true para email existente")
        void existsByEmail_existing_returnsTrue() {
            createUser("test", "test@email.com");

            assertThat(userRepository.existsByEmail("test@email.com")).isTrue();
        }

        @Test
        @DisplayName("existsByEmail devuelve false para email inexistente")
        void existsByEmail_nonExisting_returnsFalse() {
            assertThat(userRepository.existsByEmail("nada@email.com")).isFalse();
        }

        @Test
        @DisplayName("existsByUsername devuelve true para username existente")
        void existsByUsername_existing_returnsTrue() {
            createUser("admin", "admin@email.com");

            assertThat(userRepository.existsByUsername("admin")).isTrue();
        }

        @Test
        @DisplayName("existsByUsername devuelve false para username inexistente")
        void existsByUsername_nonExisting_returnsFalse() {
            assertThat(userRepository.existsByUsername("nadie")).isFalse();
        }
    }

    @Nested
    @DisplayName("findByEmailOrUsername")
    class FindByEmailOrUsernameTests {

        @Test
        @DisplayName("Encuentra usuario por email (FETCH JOIN)")
        void findByEmailOrUsername_byEmail() {
            createUser("pedro", "pedro@email.com");

            Optional<User> found = userRepository.findByEmailOrUsername("pedro@email.com", "pedro@email.com");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("pedro");
            assertThat(found.get().getCompany()).isNotNull();
        }

        @Test
        @DisplayName("Encuentra usuario por username (FETCH JOIN)")
        void findByEmailOrUsername_byUsername() {
            createUser("laura", "laura@email.com");

            Optional<User> found = userRepository.findByEmailOrUsername("laura", "laura");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("laura@email.com");
        }

        @Test
        @DisplayName("No encuentra usuario que no existe")
        void findByEmailOrUsername_nonExisting_returnsEmpty() {
            Optional<User> found = userRepository.findByEmailOrUsername("no@existe.com", "nadie");

            assertThat(found).isEmpty();
        }
    }
}
