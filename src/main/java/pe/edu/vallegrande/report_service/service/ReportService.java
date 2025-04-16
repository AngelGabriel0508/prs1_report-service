package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepo;
    private final ReportWorkshopRepository workshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;

    /**
     * 🔹 Obtener reportes con talleres filtrados por fecha + otros filtros
     */
    public Flux<ReportWithWorkshopsDto> findFilteredReports(String status, String trimester, Integer year, LocalDate dateStart, LocalDate dateEnd) {
        Flux<Report> baseQuery = (status != null) ? reportRepo.findByStatus(status) : reportRepo.findAll();

        if (trimester != null) {
            baseQuery = baseQuery.filter(r -> trimester.equalsIgnoreCase(r.getTrimester()));
        }
        if (year != null) {
            baseQuery = baseQuery.filter(r -> year.equals(r.getYear()));
        }

        return baseQuery.flatMap(report ->
                workshopRepo.findByReportId(report.getId())
                        .flatMap(rw ->
                                workshopCacheRepo.findById(rw.getWorkshopId())
                                        .filter(wc -> {
                                            boolean inRange = true;
                                            if (dateStart != null) {
                                                inRange = !wc.getDateStart().isBefore(dateStart);
                                            }
                                            if (dateEnd != null) {
                                                inRange = inRange && !wc.getDateEnd().isAfter(dateEnd);
                                            }
                                            return inRange;
                                        })
                                        .map(wc -> {
                                            ReportWorkshopDto dto = toWorkshopDto(rw);
                                            // 👉 Enriquecer DTO con datos del taller original
                                            dto.setWorkshopStatus(wc.getStatus());
                                            dto.setDateStart(wc.getDateStart());
                                            dto.setDateEnd(wc.getDateEnd());
                                            dto.setName(wc.getName());
                                            return dto;
                                        })
                        )
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(toDto(report));
                            dto.setWorkshops(workshops);
                            return dto;
                        })
        );
    }

    /**
     * 🔹 Obtener por ID con talleres filtrados por fechas
     */
    public Mono<ReportWithWorkshopsDto> findByIdWithDateFilter(Integer id, LocalDate dateStart, LocalDate dateEnd) {
        Mono<Report> reportMono = reportRepo.findById(id);

        Flux<ReportWorkshopDto> workshopsFlux = workshopRepo.findByReportId(id)
                .flatMap(rw ->
                        workshopCacheRepo.findById(rw.getWorkshopId())
                                .filter(wc -> {
                                    boolean inRange = true;
                                    if (dateStart != null) {
                                        inRange = !wc.getDateStart().isBefore(dateStart);
                                    }
                                    if (dateEnd != null) {
                                        inRange = inRange && !wc.getDateEnd().isAfter(dateEnd);
                                    }
                                    return inRange;
                                })
                                .map(wc -> {
                                    ReportWorkshopDto dto = toWorkshopDto(rw);
                                    // Enriquecer el DTO con datos del cache
                                    dto.setWorkshopStatus(wc.getStatus());
                                    dto.setDateStart(wc.getDateStart());
                                    dto.setDateEnd(wc.getDateEnd());
                                    dto.setName(wc.getName());
                                    return dto;
                                })
                );

        return Mono.zip(reportMono, workshopsFlux.collectList(), (report, workshops) -> {
            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
            dto.setReport(toDto(report));
            dto.setWorkshops(workshops);
            return dto;
        });
    }


    /**
     * 🔹 Crear reporte con talleres
     */
    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        Report report = fromDto(dto.getReport());
        report.setStatus("A"); // 🔐 Forzar estado activo al crear

        List<ReportWorkshop> workshops = dto.getWorkshops().stream()
                .map(this::fromWorkshopDto)
                .collect(Collectors.toList());

        return reportRepo.save(report)
                .flatMap(saved -> {
                    workshops.forEach(w -> w.setReportId(saved.getId()));
                    return workshopRepo.saveAll(workshops)
                            .collectList()
                            .map(savedWorkshops -> {
                                ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                response.setReport(toDto(saved));
                                response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).collect(Collectors.toList()));
                                log.info("✅ Reporte creado: {}", response);
                                return response;
                            });
                });
    }


    /**
     * 🔹 Editar reporte y reemplazar talleres
     */
    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportRepo.findById(id)
                .flatMap(existing -> {
                    existing.setYear(dto.getReport().getYear());
                    existing.setTrimester(dto.getReport().getTrimester());
                    existing.setDescription(dto.getReport().getDescription());
                    existing.setSchedule(dto.getReport().getSchedule());

                    return reportRepo.save(existing)
                            .flatMap(saved -> {
                                List<ReportWorkshop> workshops = dto.getWorkshops().stream()
                                        .map(w -> {
                                            ReportWorkshop rw = fromWorkshopDto(w);
                                            rw.setReportId(saved.getId());
                                            rw.setId(null); // ✅ Forzar INSERT, evitar UPDATE fallido
                                            return rw;
                                        }).toList();

                                return workshopRepo.deleteByReportId(id)
                                        .then(workshopRepo.saveAll(workshops).collectList())
                                        .map(savedWorkshops -> {
                                            ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                            response.setReport(toDto(saved));
                                            response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                            log.info("✏️ Reporte actualizado: {}", response);
                                            return response;
                                        });
                            });
                });
    }


    /**
     * 🔹 Restaurar reporte (status = A)
     */
    public Mono<Void> restore(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("A");
                    return reportRepo.save(r).then();
                });
    }


    /**
     * 🔹 Eliminación lógica (status = I)
     */
    public Mono<Void> deleteLogic(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("I");
                    return reportRepo.save(r).then();
                });
    }

    // ======================= MAPEO DTO =======================

    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setYear(r.getYear());
        dto.setTrimester(r.getTrimester());
        dto.setDescription(r.getDescription());
        dto.setSchedule(r.getSchedule());
        dto.setStatus(r.getStatus());
        return dto;
    }

    private Report fromDto(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .description(dto.getDescription())
                .schedule(dto.getSchedule())
                .status("A") // Siempre lo forzamos al crear
                .build();
    }

    private ReportWorkshopDto toWorkshopDto(ReportWorkshop rw) {
        ReportWorkshopDto dto = new ReportWorkshopDto();
        dto.setId(rw.getId());
        dto.setReportId(rw.getReportId());
        dto.setWorkshopId(rw.getWorkshopId());
        dto.setWorkshopName(rw.getWorkshopName());
        dto.setDescription(rw.getDescription());
        dto.setImageUrl(rw.getImageUrl());
        return dto;
    }

    private ReportWorkshop fromWorkshopDto(ReportWorkshopDto dto) {
        return ReportWorkshop.builder()
                .id(dto.getId())
                .reportId(dto.getReportId())
                .workshopId(dto.getWorkshopId())
                .workshopName(dto.getWorkshopName())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .build();
    }
}
