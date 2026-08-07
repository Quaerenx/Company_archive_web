package com.company.model;

import java.util.Objects;

public record CustomerPage(
        PageResult<CustomerDTO> result,
        CustomerCounts counts) {

    public CustomerPage {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(counts, "counts");
    }
}
