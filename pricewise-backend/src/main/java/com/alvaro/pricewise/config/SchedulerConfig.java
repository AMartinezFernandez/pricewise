package com.alvaro.pricewise.config;

import java.time.Instant;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alvaro.pricewise.scheduler.PriceMonitorJob;

/**
 * Configuracion de Quartz Scheduler.
 */
@Configuration
public class SchedulerConfig {

    private static final String PRICE_MONITOR_JOB_IDENTITY = "priceMonitorJob";
    private static final String PRICE_MONITOR_TRIGGER_IDENTITY = "priceMonitorTrigger";

    @Bean
    public JobDetail priceMonitorJobDetail() {
        return JobBuilder.newJob(PriceMonitorJob.class)
                .withIdentity(PRICE_MONITOR_JOB_IDENTITY)
                .storeDurably()
                .withDescription("Job para actualizar precios automaticamente")
                .build();
    }

    @Bean
    public Trigger priceMonitorTrigger(JobDetail priceMonitorJobDetail) {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(6)
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(priceMonitorJobDetail)
                .withIdentity(PRICE_MONITOR_TRIGGER_IDENTITY)
                .withSchedule(scheduleBuilder)
                .startAt(Date.from(Instant.now().plusSeconds(60)))
                .build();
    }
}
