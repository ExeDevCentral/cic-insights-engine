-- V1__init_company_insights_schema.sql
-- Domain: Company Insights & Connections (CIC) - Employer Reputation & SEO Telemetry

CREATE TABLE IF NOT EXISTS company_profiles (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    industry VARCHAR(100) NOT NULL,
    headquarters VARCHAR(150) NOT NULL,
    employee_band VARCHAR(40) NOT NULL,
    overall_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    culture_score NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    compensation_score NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    review_count INTEGER NOT NULL DEFAULT 0,
    active_job_postings INTEGER NOT NULL DEFAULT 0,
    seo_organic_CTR NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    seo_monthly_impressions BIGINT NOT NULL DEFAULT 0,
    verified_employer BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_overall_rating CHECK (overall_rating >= 0.00 AND overall_rating <= 5.00),
    CONSTRAINT chk_review_count CHECK (review_count >= 0)
);

CREATE INDEX idx_company_profiles_industry_rating
    ON company_profiles (industry, overall_rating DESC);

CREATE INDEX idx_company_profiles_seo_impressions
    ON company_profiles (seo_monthly_impressions DESC);

CREATE TABLE IF NOT EXISTS employer_reviews (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company_profiles(id) ON DELETE CASCADE,
    job_title VARCHAR(140) NOT NULL,
    employment_status VARCHAR(40) NOT NULL,
    rating NUMERIC(3, 2) NOT NULL,
    culture_rating NUMERIC(3, 2) NOT NULL,
    compensation_rating NUMERIC(3, 2) NOT NULL,
    headline VARCHAR(240) NOT NULL,
    pros TEXT NOT NULL,
    cons TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_rating CHECK (rating >= 1.00 AND rating <= 5.00)
);

CREATE INDEX idx_employer_reviews_company_created
    ON employer_reviews (company_id, created_at DESC);
