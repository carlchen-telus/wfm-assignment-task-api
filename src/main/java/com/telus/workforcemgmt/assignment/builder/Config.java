package com.telus.workforcemgmt.assignment.builder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class Config {

	 @Bean
	    public WebClient soapClient(){
	 
	        HttpClient httpClient = HttpClient.create()
	        		  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
	        		  .responseTimeout(Duration.ofMillis(5000))
	        		  .doOnConnected(conn -> 
	        		    conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
	        		      .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)))
	        		  .wiretap(WorkOrderBuilderSvc.class.getName(), LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
	        		  .followRedirect(true);


	       WebClient webClient = WebClient.builder()
	                .clientConnector(new ReactorClientHttpConnector(httpClient))
	                .build();

	        return webClient;
	    }
}
