import http from './http';

export interface JobQuery {
  keyword?: string;
  city?: string;
  companyName?: string;
  experience?: string;
  education?: string;
  pageNum?: number;
  pageSize?: number;
}

export function fetchJobPage(data: JobQuery) {
  return http.post('/jobs/page', data);
}

export function fetchJobStatCity() {
  return http.get('/jobs/stat/city');
}

export function fetchJobStatCompany() {
  return http.get('/jobs/stat/company');
}

export function fetchJobStatSkill() {
  return http.get('/jobs/stat/skill');
}

export function predictSalary(params: {
  city?: string;
  experience?: string;
  education?: string;
  skills?: string;
}) {
  return http.get('/jobs/predict-salary', { params });
}

export function recommendJobs(params: { skills?: string; city?: string }) {
  return http.get('/jobs/recommend', { params });
}

