package com.company.controller;

import com.company.customerhistory.CustomerHistoryRepository;
import com.company.customerhistory.CustomerHistoryStorageException;
import com.company.model.DataAccessException;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.TroubleshootingDAO;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CustomerActivityQueryService implements CustomerActivityLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CustomerActivityQueryService.class);
    private static final int RECENT_LIMIT = 3;

    private final MaintenanceRecordDAO maintenanceDAO;
    private final CustomerHistoryRepository historyRepository;
    private final TroubleshootingDAO troubleshootingDAO;

    CustomerActivityQueryService(
            MaintenanceRecordDAO maintenanceDAO,
            CustomerHistoryRepository historyRepository,
            TroubleshootingDAO troubleshootingDAO) {
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.historyRepository = Objects.requireNonNull(
                historyRepository, "historyRepository");
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
    }

    @Override
    public CustomerActivityViewData load(String customerName) {
        Set<String> unavailableSources = new HashSet<>();
        return new CustomerActivityViewData(
                safely("maintenance", unavailableSources, () -> maintenanceDAO
                        .getMaintenanceRecordsByCustomer(
                                customerName, 1, RECENT_LIMIT)
                        .items()),
                safely("history", unavailableSources, () -> historyRepository
                        .findPage(customerName, "all", "", 1, RECENT_LIMIT)
                        .items()),
                safely("troubleshooting", unavailableSources, () -> troubleshootingDAO
                        .getTroubleshootingPageByCustomer(
                                customerName, 1, RECENT_LIMIT)
                        .items()),
                unavailableSources);
    }

    private static <T> List<T> safely(
            String source,
            Set<String> unavailableSources,
            Supplier<List<T>> loader) {
        try {
            List<T> records = loader.get();
            return records == null ? List.of() : records;
        } catch (DataAccessException | CustomerHistoryStorageException exception) {
            unavailableSources.add(source);
            LOGGER.warn(
                    "Unable to load recent customer activity source={} errorType={}",
                    source,
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }
}
