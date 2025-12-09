package com.osiansoftware.usermanagementsystem;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    private Integer scoreObtained;

    private Integer outOfMarks;

    @ManyToOne
    @JoinColumn(name = "excel_file_id")
    private ExcelFile excelFile;


}
