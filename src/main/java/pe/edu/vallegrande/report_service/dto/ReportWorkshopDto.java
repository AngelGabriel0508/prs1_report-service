package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportWorkshopDto {
    private Integer id;
    private Integer reportId;
    private Integer workshopId;
    private String workshopName;
    private String description;
    private String[] imageUrl;
    // ReportWorkshopDto.java
    private String workshopStatus;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private String name;

}
