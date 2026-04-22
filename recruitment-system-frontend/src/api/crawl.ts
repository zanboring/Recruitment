import http from './http';

export interface CrawlTaskPayload {
  sourceSite?: string;
  keyword?: string;
  city?: string;
}

export interface CrawlTask {
  id: number;
  sourceSite: string;
  keyword: string;
  city: string;
  status: 'PENDING' | 'RUNNING' | 'FINISHED' | 'FAILED';
  jobCount: number;
  message: string;
  createdAt: string;
  finishedAt?: string;
}

export function createCrawlTask(data: CrawlTaskPayload): Promise<number> {
  return http.post('/crawl/task', data);
}

export function startCrawlTask(id: number): Promise<void> {
  return http.post(`/crawl/task/${id}/start`);
}

export function fetchCrawlTasks(): Promise<CrawlTask[]> {
  return http.get('/crawl/tasks');
}
