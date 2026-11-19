import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export interface CompanyInsight {
  id: string;
  slug: string;
  displayName: string;
  industry: string;
  headquarters: string;
  employeeBand: string;
  overallRating: number;
  cultureScore: number;
  compensationScore: number;
  reviewCount: number;
  activeJobPostings: number;
  seoOrganicCtr: number;
  seoMonthlyImpressions: number;
  verifiedEmployer: boolean;
  updatedAt: string;
  jsonLdStructuredData: Record<string, unknown>;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

async function fetchCompanies(industry: string, minRating: number, q: string): Promise<PageResponse<CompanyInsight>> {
  const params = new URLSearchParams();
  if (industry) params.set('industry', industry);
  if (minRating > 0) params.set('minRating', minRating.toFixed(2));
  if (q.trim()) params.set('q', q.trim());

  const res = await fetch(`${API_BASE}/companies?${params.toString()}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to fetch company insights`);
  return res.json();
}

export const CompanyInsightsDashboard: React.FC = () => {
  const [industry, setIndustry] = useState<string>('');
  const [minRating, setMinRating] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['companies', industry, minRating, searchQuery],
    queryFn: () => fetchCompanies(industry, minRating, searchQuery),
    staleTime: 30_000,
  });

  const reviewMutation = useMutation({
    mutationFn: async (payload: { slug: string; rating: number; headline: string }) => {
      const res = await fetch(`${API_BASE}/companies/${payload.slug}/reviews`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          jobTitle: 'Senior Full Stack Software Engineer',
          employmentStatus: 'CURRENT_EMPLOYEE',
          rating: payload.rating,
          cultureRating: payload.rating,
          compensationRating: payload.rating,
          headline: payload.headline,
          pros: 'Strong engineering culture, high ownership, modern React + Spring Boot stack.',
          cons: 'Fast-paced cross-timezone coordination across global product squads.',
        }),
      });
      if (!res.ok) throw new Error('Failed to submit verified employer review');
      return res.json() as Promise<CompanyInsight>;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'] });
    },
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-10 font-sans">
      <header className="max-w-7xl mx-auto mb-8 flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <span className="inline-block text-xs font-bold uppercase tracking-widest text-blue-400 bg-blue-950/60 border border-blue-800/60 px-3 py-1 rounded-full mb-2">
            Company Insights &amp; Connections (CIC) • SEO Telemetry
          </span>
          <h1 className="text-3xl font-extrabold tracking-tight text-white">
            Employer Brand &amp; Organic Search Hub
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filter by company name..."
            aria-label="Filter by company name"
            className="bg-slate-900 border border-slate-700 rounded-lg px-4 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
          />
          <select
            value={industry}
            onChange={(e) => setIndustry(e.target.value)}
            aria-label="Filter by industry"
            className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white"
          >
            <option value="">All Industries</option>
            <option value="Cloud & AI">Cloud &amp; AI</option>
            <option value="FinTech">FinTech</option>
            <option value="HR Tech & Search">HR Tech &amp; Search</option>
          </select>
          <select
            value={minRating}
            onChange={(e) => setMinRating(Number(e.target.value))}
            aria-label="Minimum rating"
            className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white"
          >
            <option value={0}>Any Rating</option>
            <option value={4.0}>4.0+ Stars</option>
            <option value={4.5}>4.5+ Stars</option>
          </select>
        </div>
      </header>

      <main className="max-w-7xl mx-auto">
        {isLoading && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
            {[1, 2, 3].map((n) => (
              <div key={n} className="h-64 bg-slate-900/70 rounded-xl border border-slate-800" />
            ))}
          </div>
        )}

        {isError && (
          <div role="alert" className="bg-red-950/50 border border-red-800 text-red-200 p-4 rounded-xl">
            {(error as Error).message}
          </div>
        )}

        {data && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {data.content.map((company) => (
              <article
                key={company.id}
                className="bg-slate-900 border border-slate-800 hover:border-blue-500/60 rounded-xl p-6 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div>
                      <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        {company.displayName}
                        {company.verifiedEmployer && (
                          <span className="text-xs bg-emerald-950 text-emerald-300 border border-emerald-700/50 px-2 py-0.5 rounded-full">
                            Verified
                          </span>
                        )}
                      </h2>
                      <p className="text-xs text-slate-400">
                        {company.industry} • {company.headquarters}
                      </p>
                    </div>
                    <div className="bg-blue-600/20 border border-blue-500/40 text-blue-300 font-bold px-3 py-1 rounded-lg text-sm">
                      ★ {company.overallRating.toFixed(2)}
                    </div>
                  </div>

                  <dl className="grid grid-cols-3 gap-2 my-4 bg-slate-950/60 p-3 rounded-lg border border-slate-800/80 text-center">
                    <div>
                      <dt className="text-[11px] text-slate-400 uppercase">SEO Impr/Mo</dt>
                      <dd className="text-sm font-semibold text-white">
                        {(company.seoMonthlyImpressions / 1_000).toFixed(1)}k
                      </dd>
                    </div>
                    <div>
                      <dt className="text-[11px] text-slate-400 uppercase">Organic CTR</dt>
                      <dd className="text-sm font-semibold text-emerald-400">
                        {company.seoOrganicCtr.toFixed(2)}%
                      </dd>
                    </div>
                    <div>
                      <dt className="text-[11px] text-slate-400 uppercase">Open Roles</dt>
                      <dd className="text-sm font-semibold text-blue-400">
                        {company.activeJobPostings}
                      </dd>
                    </div>
                  </dl>
                </div>

                <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
                  <button
                    type="button"
                    onClick={() => setSelectedSlug(selectedSlug === company.slug ? null : company.slug)}
                    className="text-xs text-slate-300 hover:text-white underline"
                  >
                    {selectedSlug === company.slug ? 'Hide Schema.org JSON-LD' : 'Inspect SEO JSON-LD'}
                  </button>

                  <button
                    type="button"
                    disabled={reviewMutation.isPending}
                    onClick={() =>
                      reviewMutation.mutate({
                        slug: company.slug,
                        rating: 5.0,
                        headline: 'High-velocity engineering culture',
                      })
                    }
                    className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold px-3 py-1.5 rounded-lg transition"
                  >
                    + Quick 5★ Review
                  </button>
                </div>

                {selectedSlug === company.slug && (
                  <pre className="mt-4 bg-slate-950 p-3 rounded-lg text-[11px] text-emerald-300 overflow-x-auto border border-slate-800">
                    {JSON.stringify(company.jsonLdStructuredData, null, 2)}
                  </pre>
                )}
              </article>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};
