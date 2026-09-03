package com.company.controller;

import com.company.customerhistory.CustomerHistoryRepository;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.TroubleshootingDAO;
import java.util.List;
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
        return new CustomerActivityViewData(
                safely("maintenance", customerName, () -> maintenanceDAO
                        .getMaintenanceRecordsByCustomer(
                                customerName, 1, RECENT_LIMIT)
                        .items()),
                safely("history", customerName, () -> historyRepository
                        .findPage(customerName, "all", "", 1, RECENT_LIMIT)
                        .items()),
                safely("troubleshooting", customerName, () -> troubleshootingDAO
                        .getTroubleshootingPageByCustomer(
                                customerName, 1, RECENT_LIMIT)
                        .items()));
    }

    private static <T> List<T> safely(
            String source,
            String customerName,
            Supplier<List<T>> loader) {
        try {
            List<T> records = loader.get();
            return records == null ? List.of() : records;
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to load recent customer activity source={} customer={}",
                    source,
                    customerName,
                    exception);
            return List.of();
        }
    }
}
