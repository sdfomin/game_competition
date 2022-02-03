package ru.nekoguys.game.persistence

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@ComponentScan
@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories
@EnableTransactionManagement
class GamePersistenceConfig
