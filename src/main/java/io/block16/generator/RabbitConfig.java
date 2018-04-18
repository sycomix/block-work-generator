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

    @Bean
    public DirectExchange blockWorkExchange() {
        return new DirectExchange(BLOCK_WORK_EXCHANGE, true, false);
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
    public Queue queue() {
        return new Queue(BLOCK_WORK_QUEUE_NAME);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(blockWorkExchange()).with(BLOCK_ROUTING_KEY);
    }

    /*
    @Bean
    public AmqpLogMessageListener messageListener() {
        return new AmqpLogMessageListener();
    }

    @Bean
    public SimpleMessageListenerContainer listenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(BLOCK_WORK_QUEUE_NAME);

        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(messageListener(), new AmqpLogMessageConverter());
        listenerAdapter.setDefaultListenerMethod("handleLog");

        container.setMessageListener(listenerAdapter);
        return container;
    }
    */
}
