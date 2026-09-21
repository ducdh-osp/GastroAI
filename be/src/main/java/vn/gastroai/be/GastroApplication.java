package vn.gastroai.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * Datasource/JPA/Flyway auto-config mặc định bị tắt vì được khai báo thủ công
 * trong config.PostgresConfig và config.MysqlConfig (kiến trúc đa cơ sở dữ liệu,
 * mục 6 đề cương).
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class GastroApplication {

	public static void main(String[] args) {
		SpringApplication.run(GastroApplication.class, args);
	}

}
