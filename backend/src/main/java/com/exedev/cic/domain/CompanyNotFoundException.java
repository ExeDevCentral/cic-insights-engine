package com.exedev.cic.domain;

public class CompanyNotFoundException extends RuntimeException {

    private final String slug;

    public CompanyNotFoundException(String slug) {
        super("Company profile not found for slug: " + slug);
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }
}
