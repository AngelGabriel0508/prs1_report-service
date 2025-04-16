package pe.edu.vallegrande.report_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("report_workshop")
public class ReportWorkshop {
    @Id
    private Integer id;
    @Column("report_id")
    private Integer reportId;
    @Column("workshop_id")
    private Integer workshopId;
    @Column("workshop_name")
    private String workshopName;
    private String description;
    @Column("image_url")
    private String[] imageUrl;
}
