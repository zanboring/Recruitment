package com.example.recruitment.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DataService {

    void importJobs(MultipartFile file);

    void exportJobs(HttpServletResponse response, Integer limit);
}

