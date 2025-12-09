package com.osiansoftware.usermanagementsystem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExcelFileRepository  extends JpaRepository<ExcelFile, Long>{
        Optional<ExcelFile> findByUser_Id(Long userId);
    }
