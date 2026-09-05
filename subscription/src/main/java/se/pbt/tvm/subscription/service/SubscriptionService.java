package se.pbt.tvm.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.pbt.tvm.core.subscription.SchedulePreset;
import se.pbt.tvm.subscription.config.SubscriptionStorageProperties;
import se.pbt.tvm.subscription.format.SubscriptionFormatter;
import se.pbt.tvm.subscription.model.Subscription;
import se.pbt.tvm.subscription.persistence.SubscriptionStorage;
import se.pbt.tvm.subscription.policy.SubscriptionIdGenerator;
import se.pbt.tvm.subscription.policy.SubscriptionValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates core subscription operations such as creation,
 * validation, listing, and removal.
 * <p>
 * This service ensures that subscriptions are structurally valid,
 * uniquely identified, and safely persisted through the storage layer.
 * All operations share a single storage path from {@link SubscriptionStorageProperties}.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionStorage storage;
    private final SubscriptionIdGenerator idGenerator;
    private final SubscriptionValidator validator;
    private final SubscriptionFormatter formatter;
    private final SubscriptionStorageProperties storageProperties;

    /**
     * Validates and saves a new subscription.
     * <p>
     * Ensures the subscription passes all validation checks,
     * assigns a unique ID, and persists it to storage.
     */
    public SaveResult save(Subscription subscription) {
        if (subscription == null) {
            return SaveResult.fail("Subscription cannot be null.");
        }

        try {
            List<Subscription> existing = Optional.ofNullable(storage.loadSubscriptions(storageProperties.getPath()))
                    .orElseGet(ArrayList::new);

            var error = validator.validate(subscription, existing);
            if (error.isPresent()) {
                return SaveResult.fail(error.get());
            }

            subscription.setId(idGenerator.generateUniqueId(subscription, existing));
            existing.add(subscription);
            storage.saveSubscriptions(existing, storageProperties.getPath());

            return SaveResult.ok("Subscription created with id: " + subscription.getId());
        } catch (Exception e) {
            return SaveResult.fail("Failed to save subscription: " + e.getMessage());
        }
    }

    /**
     * Returns all subscriptions belonging to a given chat,
     * formatted for display or user output.
     */
    public List<String> listByChatId(long chatId) {
        try {
            List<Subscription> all = Optional.ofNullable(storage.loadSubscriptions(storageProperties.getPath()))
                    .orElseGet(List::of);

            return all.stream()
                    .filter(s -> s.getChatId() == chatId)
                    .map(formatter::format)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list subscriptions: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all enabled subscriptions due for the given schedule preset.
     * <p>
     * Used by the dispatch scheduler to determine which subscribers should
     * receive news for a given firing cron.
     */
    public List<Subscription> findEnabledBySchedule(SchedulePreset preset) {
        List<Subscription> all = Optional.ofNullable(storage.loadSubscriptions(storageProperties.getPath()))
                .orElseGet(List::of);

        return all.stream()
                .filter(Subscription::isEnabled)
                .filter(s -> s.getSchedule() == preset)
                .toList();
    }

    /**
     * Removes a subscription from storage by matching ID or keyword within a chat.
     * <p>
     * Performs a case-insensitive comparison and updates storage if a match is found.
     */
    public boolean removeByIdOrKeyword(long chatId, String arg) {
        if (arg == null || arg.isBlank()) {
            return false;
        }

        try {
            List<Subscription> all = Optional.ofNullable(storage.loadSubscriptions(storageProperties.getPath()))
                    .orElseGet(ArrayList::new);

            String target = arg.trim().toLowerCase();
            List<Subscription> remaining = new ArrayList<>();
            boolean removed = false;

            for (Subscription s : all) {
                boolean matchesChat = s.getChatId() == chatId;
                boolean matchesId = s.getId() != null && s.getId().equalsIgnoreCase(target);
                boolean matchesKeyword = s.getFilter() != null &&
                        s.getFilter().getKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(target));

                if (matchesChat && (matchesId || matchesKeyword)) {
                    removed = true;
                    continue;
                }
                remaining.add(s);
            }

            if (removed) {
                storage.saveSubscriptions(remaining, storageProperties.getPath());
            }

            return removed;
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Represents the result of a save operation.
     */
    public record SaveResult(boolean success, String message) {
        public static SaveResult ok(String msg)  { return new SaveResult(true, msg); }
        public static SaveResult fail(String msg){ return new SaveResult(false, msg); }
    }
}
