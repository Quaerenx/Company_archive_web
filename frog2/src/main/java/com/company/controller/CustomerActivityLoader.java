package com.company.controller;

@FunctionalInterface
interface CustomerActivityLoader {
    CustomerActivityViewData load(String customerName);

    static CustomerActivityLoader empty() {
        return ignored -> CustomerActivityViewData.empty();
    }
}
