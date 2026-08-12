package com.company.controller;

import com.company.model.MaintenanceRecordDTO;
import java.util.LinkedHashMap;
import java.util.Map;

record MaintenanceFormSubmission(
        MaintenanceRecordDTO record, Map<String, String> fieldErrors) {
    MaintenanceFormSubmission {
        fieldErrors = Map.copyOf(new LinkedHashMap<>(fieldErrors));
    }

    boolean valid() {
        return fieldErrors.isEmpty();
    }
}
