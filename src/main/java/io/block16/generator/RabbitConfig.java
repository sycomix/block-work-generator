package io.block16.generator;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    static final String generatorQueueName = "generator";



    @Bean
    Queue queue() {
        return new Queue(generatorQueueName, true);
    }

    /*@Bean
    SimpleMessageListenerContainer container(
            ConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(generatorQueueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }*/

    /*@Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    } */
}
