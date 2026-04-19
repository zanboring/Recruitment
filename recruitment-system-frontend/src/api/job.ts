import http from './http';

export interface JobQuery {
  keyword?: string;
  city?: string;
  companyName?: string;
  experience?: string;
  education?: string;
  status?: string;
  sourceSite?: string;
  minSalary?: number;
  maxSalary?: number;
  pageNum?: number;
  pageSize?: number;
}

export interface JobItem {
  id: number;
  companyId?: number;
  title: string;
  companyName: string;
  sourceSite: string;
  jobKey: string;
  jobStatus: 'NEW' | 'ACTIVE' | 'OFFLINE';
  city: string;
  experience: string;
  education: string;
  minSalary?: number;
  maxSalary?: number;
  salaryUnit: string;
  skills: string;
  jobDesc?: string;
  publishTime?: string;
  lastSeenAt?: string;
  createdAt: string;
}

export interface JobStatItem {
  name: string;
  count: number;
  avgSalary?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

export function fetchJobPage(data: JobQuery): Promise<PageResult<JobItem>> {
  return http.post('/jobs/page', data);
}

export function fetchJobStatCity(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/city');
}

export function fetchJobStatCompany(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/company');
}

export function fetchJobStatSkill(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/skill');
}

export function fetchJobStatSalaryRange(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/salary-range');
}

export function fetchJobStatEducation(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/education');
}

export function fetchJobStatExperience(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/experience');
}

export function fetchJobStatStatus(): Promise<JobStatItem[]> {
  return http.get('/jobs/stat/status');
}

export function predictSalary(params: {
  city?: string;
  experience?: string;
  education?: string;
  skills?: string;
}): Promise<number> {
  return http.get('/jobs/predict-salary', { params });
}

export function recommendJobs(params: { skills?: string; city?: string }): Promise<JobItem[]> {
  return http.get('/jobs/recommend', { params });
}

export function fetchAnalysisSummary(): Promise<string> {
  return http.get('/jobs/analysis/summary');
}

export function fetchTopTitles(): Promise<JobStatItem[]> {
  return http.get('/jobs/analysis/top-titles');
}

export interface AIAnalysisResult {
  summary: string;
  qualityJobs: QualityJob[];
  trendAnalysis: TrendAnalysis;
  skillDemands: SkillDemand[];
  salaryAnalysis: SalaryAnalysis;
  suggestions: string[];
}

export interface QualityJob {
  id: number;
  title: string;
  companyName: string;
  city: string;
  salary: string;
  skills: string;
  recommendReason: string;
}

export interface TrendAnalysis {
  trendText: string;
  hotCity: string;
  hotSkill: string;
  hotTitle: string;
}

export interface SkillDemand {
  skill: string;
  count: number;
  level: '非常热门' | '热门' | '一般';
}

export interface SalaryAnalysis {
  avgSalary: string;
  topSalary: string;
  salaryRange: string;
}

export function aiAnalysis(data?: JobQuery): Promise<AIAnalysisResult> {
  return http.post('/jobs/ai-analysis', data || {});
}

