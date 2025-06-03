package com.mc_host.api.service.clerk;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.user.ClerkUserEvent;

@Service
public class ClerkEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(ClerkEventProcessor.class.getName());

    public void processEvent(ClerkUserEvent event) {
        LOGGER.info(String.format("Processing %s %s", event.type(), event.userId()));

        switch (event.type()) {
            case "user.created":
                create(event);
                break;
            case "user.deleted":
                delete(event);
                break;
            case "user.updated":
                update(event);
                break;
            default:
                throw new IllegalStateException("Illegal clerk event type: " + event.type());
        }

    }

    private void create(ClerkUserEvent event) {
        LOGGER.info("New clerk user: " + event.userId());
        //create all accounts
    }

    private void delete(ClerkUserEvent event) {
        LOGGER.info("Delete clerk user: " + event.userId());
        // delete all accounts
    }

    private void update(ClerkUserEvent event) {
        LOGGER.info("Update clerk user: " + event.userId());
        // make any relevant updates to stripe, pterodactyl
    }
    
}
