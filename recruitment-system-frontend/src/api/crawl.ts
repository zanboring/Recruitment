import http from './http';

export interface CrawlTaskPayload {
  sourceSite?: string;
  keyword?: string;
  city?: string;
}

export function createCrawlTask(data: CrawlTaskPayload) {
  return http.post('/crawl/task', data);
}

export function startCrawlTask(id: number) {
  return http.post(`/crawl/task/${id}/start`);
}

export function fetchCrawlTasks() {
  return http.get('/crawl/tasks');
}
