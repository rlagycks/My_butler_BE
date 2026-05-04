package com.mybutler.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["mailTaskExecutor"])
    fun mailTaskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 100
            setThreadNamePrefix("mail-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }
    }

    @Bean(name = ["notificationTaskExecutor"])
    fun notificationTaskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 200
            setThreadNamePrefix("notification-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }
    }
}
