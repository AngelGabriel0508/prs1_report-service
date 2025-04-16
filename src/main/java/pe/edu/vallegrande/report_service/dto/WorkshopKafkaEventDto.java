package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkshopKafkaEventDto {
    private Integer id;
    private String name;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private String status;
}
