package io.block16.generator;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static String BLOCK_WORK_EXCHANGE = "listener.block.exchange";
    public static String BLOCK_WORK_QUEUE_NAME = "listener.block.queue";
    public static String BLOCK_ROUTING_KEY = "";

    public static String UPDATE_BLOCK_EXCHANGE = "listener.update.exchange";
    public static String UPDATE_BLOCK_QUEUE = "listener.update.queue";
    public static String UPDATE_ROUTING_KEY = "";

    @Value("${amqp.port:5672}")
    private int port = 5672;

    @Value("${amqp.username:guest}")
    private String username = "guest";

    @Value("${amqp.password:guest}")
    private String password = "guest";

    @Value("${amqp.vhost:/}")
    private String virtualHost = "/";

    @Value("${amqp.host:localhost}")
    private String host = "localhost";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        // connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setPort(port);
        return connectionFactory;
    }

    /**
     * @return the admin bean that can declare queues etc.
     */
    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public DirectExchange blockWorkExchange() {
        return new DirectExchange(BLOCK_WORK_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange updateWorkExchange() { return new DirectExchange(UPDATE_BLOCK_EXCHANGE, true, false); }

    @Bean
    public Queue blockWorkQueue() {
        return new Queue(BLOCK_WORK_QUEUE_NAME);
    }

    @Bean
    public Queue updateQueue() {
        return new Queue(UPDATE_BLOCK_QUEUE);
    }

    @Bean
    public Binding blockBinding() {
        return BindingBuilder.bind(blockWorkQueue()).to(blockWorkExchange()).with(BLOCK_ROUTING_KEY);
    }

    @Bean
    public Binding updateBinding() {
        return BindingBuilder.bind(updateQueue()).to(updateWorkExchange()).with(UPDATE_ROUTING_KEY);
    }
}
