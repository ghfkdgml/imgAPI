package com.project.imgapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.imgapi.entity.Project;

public interface ProjectRepository  extends JpaRepository<Project, Long>{
    
}
