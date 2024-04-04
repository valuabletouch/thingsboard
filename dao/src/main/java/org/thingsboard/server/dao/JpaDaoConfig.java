/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.sqlts.sql.JpaSqlTimeseriesDao;
import org.thingsboard.server.dao.util.TbAutoConfiguration;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

/**
 * @author Valerii Sosliuk
 */
@Configuration
@TbAutoConfiguration
@ComponentScan({ "org.thingsboard.server.dao.sql", "org.thingsboard.server.dao.attributes",
        "org.thingsboard.server.dao.cache", "org.thingsboard.server.cache" })
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        "org.thingsboard.server.dao.sql" }, entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "jpaTransactionManager")
@EntityScan(basePackages = { "org.thingsboard.server.dao.model.sql", "org.thingsboard.server.dao.model.sqlts" })
public class JpaDaoConfig {

    @Primary
    @Bean(name = "jpaDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "jpaDataSource")
    public DataSource dataSource(@Qualifier("jpaDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("jpaDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("org.thingsboard.server.dao.model.sql", "org.thingsboard.server.dao.model.sqlts")
                .persistenceUnit("thingsboard")
                .build();
    }

    @Primary
    @Bean(name = "jpaTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}