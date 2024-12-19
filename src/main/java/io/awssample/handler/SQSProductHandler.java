package io.awssample.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awssample.DaggerProductComponent;
import io.awssample.service.CreateProductUseCase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SQSProductHandler implements RequestHandler<SQSEvent, List<String>> {

    // first set TTL property
    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");
    }

//    private static final Logger logger = LoggerFactory.getLogger(SQSProductHandler.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CreateProductUseCase createProductUseCase;

    public SQSProductHandler() {
        DaggerProductComponent.builder().build().inject(this);
    }

    @Override
    public List<String> handleRequest(SQSEvent event, Context context) {
        // FIXME: use the context logger?
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: %s".formatted(event.getClass()));

        var messagesFound = new ArrayList<String>();
        for(SQSEvent.SQSMessage msg : event.getRecords()){
            messagesFound.add(msg.getBody());
            logger.log("SQS message received: ID %s, Body: %s, Receipt Handle: %s".formatted(msg.getMessageId(), msg.getBody(), msg.getReceiptHandle()));
        }

        for (String msg : messagesFound) {
            try {
                var request = objectMapper.readValue(msg, CreateProductRequest.class);
                createProductUseCase.create(request.name(), request.price());
            } catch (JsonProcessingException e) {
                // FIXME: handle this exception?
                logger.log(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return messagesFound;
    }
}
