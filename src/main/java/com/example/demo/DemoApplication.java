package com.example.demo;

import com.example.demo.entity.Customer;
import com.example.demo.entity.Customer.CustomerType;
import com.example.demo.entity.User;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;

import java.math.BigDecimal;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initPanasonic(UserRepository userRepository, CustomerRepository customerRepository) {
		return args -> {
			Optional<User> existingUser = userRepository.findByUsername("panasonic");
			if (existingUser.isEmpty()) {
				Customer panasonic = Customer.builder()
						.fullName("Đại diện Panasonic")
						.email("admin@panasonic.com")
						.phoneNumber("0123456789")
						.customerType(CustomerType.ENTERPRISE)
						.companyName("Công ty TNHH Panasonic Việt Nam")
						.taxCode("0101234567")
						.billingAddress("KCN Thăng Long, Đông Anh, Hà Nội")
						.creditLimit(new BigDecimal("100000000")) // 100M VND
						.build();
				customerRepository.saveAndFlush(panasonic);

				User user = User.builder()
						.username("panasonic")
						.password("123456")
						.fullName("Quản trị viên Panasonic")
						.email("admin@panasonic.com")
						.phone("0123456789")
						.role(User.Role.ENTERPRISE)
						.isActive(true)
						.customer(panasonic)
						.build();
				userRepository.saveAndFlush(user);
				System.out.println("Tự động tạo đối tác độc quyền Panasonic thành công!");
			}
		};
	}
}
