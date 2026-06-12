package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class StudyProjectBackendApplicationTests {

	@Test
	void contextLoads() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		String password = encoder.encode("123456");

		System.out.println(password);

		// 测试一下 123456 能不能匹配加密后的密码
		System.out.println(encoder.matches("123456", password));
	}

}
