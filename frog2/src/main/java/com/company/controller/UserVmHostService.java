package com.company.controller;

import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import java.util.Objects;

final class UserVmHostService {
    enum SaveResult {
        SAVED,
        USER_REQUIRED,
        INVALID_IP,
        PURPOSE_REQUIRED,
        HOST_NOT_FOUND,
        DUPLICATE_OWN_IP,
        DUPLICATE_OTHER_IP,
        HOST_LIMIT_REACHED,
        WRITE_FAILED
    }

    private static final String ALLOWED_IP_PREFIX = "192.168.40.";

    private final UserVmHostDAO userVmHostDAO;

    UserVmHostService(UserVmHostDAO userVmHostDAO) {
        this.userVmHostDAO = Objects.requireNonNull(
                userVmHostDAO, "userVmHostDAO");
    }

    SaveResult save(UserVmHostDTO dto, String originalIp) {
        Objects.requireNonNull(dto, "dto");

        normalize(dto);
        String normalizedOriginalIp = normalizeValue(originalIp);

        if (dto.getOwnerUserId() == null) {
            return SaveResult.USER_REQUIRED;
        }
        if (!isAllowedIp(dto.getIp())) {
            return SaveResult.INVALID_IP;
        }
        if (dto.getPurpose() == null) {
            return SaveResult.PURPOSE_REQUIRED;
        }

        return switch (userVmHostDAO.saveNormalized(
                dto, normalizedOriginalIp)) {
            case SAVED -> SaveResult.SAVED;
            case HOST_NOT_FOUND -> SaveResult.HOST_NOT_FOUND;
            case DUPLICATE_OWN_IP -> SaveResult.DUPLICATE_OWN_IP;
            case DUPLICATE_OTHER_IP -> SaveResult.DUPLICATE_OTHER_IP;
            case HOST_LIMIT_REACHED -> SaveResult.HOST_LIMIT_REACHED;
            case WRITE_FAILED -> SaveResult.WRITE_FAILED;
        };
    }

    private void normalize(UserVmHostDTO dto) {
        dto.setOwnerUserId(normalizeValue(dto.getOwnerUserId()));
        dto.setOwnerUserName(normalizeValue(dto.getOwnerUserName()));
        dto.setIp(normalizeValue(dto.getIp()));
        dto.setPurpose(normalizeValue(dto.getPurpose()));
        dto.setOsInfo(normalizeValue(dto.getOsInfo()));
        dto.setVerticaVersion(normalizeValue(dto.getVerticaVersion()));
        dto.setRemoteHost(normalizeValue(dto.getRemoteHost()));
        dto.setNote(normalizeValue(dto.getNote()));
        dto.setStatus("ACTIVE");
    }

    private boolean isAllowedIp(String ip) {
        if (ip == null || !ip.startsWith(ALLOWED_IP_PREFIX)) {
            return false;
        }
        try {
            int lastOctet = Integer.parseInt(
                    ip.substring(ALLOWED_IP_PREFIX.length()));
            return lastOctet >= 1 && lastOctet <= 254;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
