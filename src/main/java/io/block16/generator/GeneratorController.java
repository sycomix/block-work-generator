package io.block16.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.io.IOException;

@RestController
public class GeneratorController {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static String processedBlockKey = "listener::ListenerService::latestBlockProcessed";
    private static String updateBlockKey = "listener::ListenerService::updateBlockPosition";

    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOperations;

    private final ObjectMapper objectMapper;
    private final Web3j web3j;
    private int addedUpto;

    RateLimiter rateLimiter = RateLimiter.create(1000);

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

        this.addedUpto = this.valueOperations.get(processedBlockKey) != null ? (Integer) this.valueOperations.get(processedBlockKey) : -1;

        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedDelay = 10000)
    private void setUpdateBlocksToScan() {
        try {
            int current = this.valueOperations.get(updateBlockKey) != null ? (Integer) this.valueOperations.get(updateBlockKey) : 0;
            // Only do this if the value has changed
            if (current > 0) {
                for (int i = current; i > 0; i--) {
                    rateLimiter.acquire(1);
                    BlockWorkDto blockWorkDto = new BlockWorkDto();
                    blockWorkDto.setBlockNumber(i);
                    this.rabbitTemplate.convertAndSend(RabbitConfig.UPDATE_BLOCK_EXCHANGE, RabbitConfig.UPDATE_WORK_ROUTING_KEY, objectMapper.writeValueAsString(blockWorkDto));
                    if (i % 100000 == 0) {
                        this.valueOperations.set(updateBlockKey, i);
                    }
                }
                this.valueOperations.set(updateBlockKey, 0);
            }
        } catch (Exception e) {
            LOGGER.error("Could not add number to the rabbit server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check for blocks to add to the queue every 3 seconds.
     * FixedDelay waits a delay until the previous invocation finishes
     */
    @Scheduled(fixedDelay = 3000)
    private void getBlocksToScan() {
        try {
            EthBlockNumber ethBlockNumber = this.web3j.ethBlockNumber().send();
            LOGGER.info("Got blockNumber: " + ethBlockNumber.getBlockNumber().intValue());

            // Add all new blocks, up to the 2nd to last block
            if (this.addedUpto + 1 <= ethBlockNumber.getBlockNumber().intValue() - 1) {
                LOGGER.info("At: " + this.addedUpto + " going to: " + (ethBlockNumber.getBlockNumber().intValue() - 1));
                for(int i = this.addedUpto + 1; i < ethBlockNumber.getBlockNumber().intValue() - 1; i++) {
                    rateLimiter.acquire(1);
                    BlockWorkDto blockWorkDto = new BlockWorkDto();
                    blockWorkDto.setBlockNumber(i);
                    this.rabbitTemplate.convertAndSend(RabbitConfig.BLOCK_WORK_EXCHANGE, RabbitConfig.BLOCK_ROUTING_KEY, objectMapper.writeValueAsString(blockWorkDto));
                    this.addedUpto = i;
                }
            }
            this.valueOperations.set(processedBlockKey, this.addedUpto);
        } catch (IOException ex) {
            LOGGER.error("Could not get lastest block number from the node: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @GetMapping(path = "/update/{fromBlock}")
    public String setUpdateBlocks(@PathVariable Integer fromBlock) {
        if (fromBlock == null) {
            return "DID NOT SET\n";
        }
        this.valueOperations.set(updateBlockKey, fromBlock);
        return "SET\n";
    }

}
