package com.recursive.application;

import com.recursive.domain.LicenseRepository;

public class LicenseService {

    private final LicenseRepository repository;

    public LicenseService(LicenseRepository repository) {
        this.repository = repository;
    }

    public boolean isAccepted() {
        return repository.isAccepted();
    }

    public void accept() {
        repository.accept();
    }
}
