package io.block16.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.io.IOException;

@RestController
public class GeneratorController {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static String processedBlockKey = "listener::ListenerService::latestBlockProcessed";
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOperations;
    private final Web3j web3j;
    private int addedUpto;

    @Autowired
    public GeneratorController(
            final RabbitTemplate rabbitTemplate,
            final RedisTemplate<String, Object> redisTemplate,
            final Web3j web3j
    ) {
        this.rabbitTemplate = rabbitTemplate;

        this.redisTemplate = redisTemplate;
        this.valueOperations = this.redisTemplate.opsForValue();
        this.web3j = web3j;

        int lastBlockNum = this.valueOperations.get(processedBlockKey) != null ? (Integer) this.valueOperations.get(processedBlockKey) : -1;
        this.addedUpto = lastBlockNum;
    }
    /**
     * Check for blocks to add to the queue every 5 seconds.
     * FixedDelay waits a delay until the previous invocation finishes
     */
    @Scheduled(fixedDelay = 3000)
    private void getBlocksToScan() {
        try {
            EthBlockNumber ethBlockNumber = this.web3j.ethBlockNumber().send();

            // Add all new blocks, up to the 2nd to last block
            if (this.addedUpto + 1 <= ethBlockNumber.getBlockNumber().intValue() - 1) {
                for(int i = this.addedUpto + 1; i < ethBlockNumber.getBlockNumber().intValue() - 1; i++) {
                    BlockWorkDto blockWorkDto = new BlockWorkDto();
                    blockWorkDto.setBlockNumber(i);
                    this.rabbitTemplate.convertAndSend();
                    this.addedUpto = i;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not get lastest block number from the node: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
