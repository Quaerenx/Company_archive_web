package com.company.model;

public record CustomerDetailSet(
        CustomerDetailDTO production,
        CustomerDetailDTO staging,
        CustomerDetailDTO development) {
}
