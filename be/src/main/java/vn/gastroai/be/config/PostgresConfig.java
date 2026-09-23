package vn.gastroai.be.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Datasource "chính" — dữ liệu bệnh nhân, chat, RAG (mục 6 đề cương).
 * Song song với config/MysqlConfig (dữ liệu quản trị nội bộ).
 * Các bean đánh dấu @Primary vì có 2 bộ DataSource/EntityManagerFactory/TransactionManager
 * cùng tồn tại trong context (Postgres + MySQL) — bất kỳ chỗ nào autowire kiểu chung chung
 * (vd DataSource, EntityManagerFactory) mà không chỉ định @Qualifier cụ thể sẽ cần 1 bean
 * @Primary để Spring không báo lỗi NoUniqueBeanDefinitionException.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "vn.gastroai.be.infrastructure.persistence.postgres",
        entityManagerFactoryRef = "postgresEntityManagerFactory",
        transactionManagerRef = "postgresTransactionManager"
)
public class PostgresConfig {

    @Primary
    @Bean(name = "postgresDataSourceProperties")
    @ConfigurationProperties("app.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource(
            @Qualifier("postgresDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "postgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(
            @Qualifier("postgresDataSource") DataSource dataSource) {
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        return new EntityManagerFactoryBuilder(
                new HibernateJpaVendorAdapter(), ds -> jpaProperties, null)
                .dataSource(dataSource)
                // domain.admin thuoc MySQL (config/MysqlConfig) - khong scan o day
                .packages(
                        "vn.gastroai.be.domain.auth",
                        "vn.gastroai.be.domain.patient",
                        "vn.gastroai.be.domain.chat",
                        "vn.gastroai.be.domain.rag",
                        "vn.gastroai.be.domain.triage",
                        "vn.gastroai.be.domain.notification")
                .persistenceUnit("postgres")
                .build();
    }

    @Primary
    @Bean(name = "postgresTransactionManager")
    public PlatformTransactionManager postgresTransactionManager(
            @Qualifier("postgresEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    @Primary
    @Bean(name = "postgresFlyway", initMethod = "migrate")
    public Flyway postgresFlyway(@Qualifier("postgresDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/postgres")
                .load();
    }
}
