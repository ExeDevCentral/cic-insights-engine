-- V2__seed_initial_company_profiles.sql
-- Seed realistic employer insights and SEO telemetry for local development & live demos

INSERT INTO company_profiles (
    id, slug, display_name, industry, headquarters, employee_band,
    overall_rating, culture_score, compensation_score, review_count,
    active_job_postings, seo_organic_ctr, seo_monthly_impressions, verified_employer
) VALUES
(
    'a1111111-1111-4111-8111-111111111111',
    'indeed-global',
    'Indeed',
    'HR Tech & Search',
    'Austin, TX (Remote LATAM)',
    '10,000+',
    4.62, 4.70, 4.55, 1284,
    342, 7.85, 18450000, TRUE
),
(
    'b2222222-2222-4222-8222-222222222222',
    'stripe-payments',
    'Stripe',
    'FinTech',
    'South San Francisco, CA',
    '5,000-10,000',
    4.75, 4.68, 4.88, 940,
    185, 8.40, 9200000, TRUE
),
(
    'c3333333-3333-4333-8333-333333333333',
    'vercel-cloud',
    'Vercel',
    'Cloud & AI',
    'San Francisco, CA',
    '500-1,000',
    4.81, 4.85, 4.72, 412,
    64, 9.15, 6800000, TRUE
)
ON CONFLICT (slug) DO NOTHING;
